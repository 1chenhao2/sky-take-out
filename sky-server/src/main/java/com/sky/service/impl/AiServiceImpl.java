package com.sky.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.sky.ai.tool.AiFunctionDefinition;
import com.sky.ai.tool.AiFunctionHandler;
import com.sky.ai.tool.AiToolContext;
import com.sky.ai.tool.ToolRegistry;
import com.sky.context.BaseContext;
import com.sky.dto.ChatRequestDTO;
import com.sky.dto.ChatResponseDTO;
import com.sky.dto.ToolCallRecord;
import com.sky.exception.AiServiceException;
import com.sky.model.ChatMessage;
import com.sky.properties.AiProperties;
import com.sky.properties.FaqProperties;
import com.sky.properties.MerchantProperties;
import com.sky.service.AiService;
import com.sky.utils.HttpClientUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class AiServiceImpl implements AiService {

    private static final int MAX_LOOP = 5;
    private static final String REDIS_KEY_PREFIX = "ai:chat:session:";

    @Autowired
    private AiProperties aiProperties;
    @Autowired
    private FaqProperties faqProperties;
    @Autowired
    private MerchantProperties merchantProperties;
    @Autowired
    private ToolRegistry toolRegistry;
    @Autowired
    private RedisTemplate redisTemplate;

    @Override
    public ChatResponseDTO chat(ChatRequestDTO request, String roleType) {
        if (request.getMessage() == null || request.getMessage().trim().isEmpty()) {
            throw new AiServiceException("消息不能为空");
        }

        String sessionId = request.getSessionId();
        if (sessionId == null || sessionId.trim().isEmpty()) {
            sessionId = UUID.randomUUID().toString().replace("-", "");
        }

        List<ChatMessage> messages = loadConversation(sessionId);
        if (messages.isEmpty()) {
            messages.add(ChatMessage.builder()
                    .role("system")
                    .content(buildSystemPrompt(roleType))
                    .build());
        }

        messages.add(ChatMessage.builder()
                .role("user")
                .content(request.getMessage())
                .build());

        List<AiFunctionDefinition> tools = toolRegistry.getDefinitionsForRole(roleType);
        AiToolContext toolContext = AiToolContext.builder()
                .currentUserId(BaseContext.getCurrentId())
                .roleType(roleType)
                .build();

        List<ToolCallRecord> toolCallRecords = new ArrayList<>();

        try {
            for (int i = 0; i < MAX_LOOP; i++) {
                String llmResponse = callLlm(messages, tools);

                JSONObject responseJson = JSON.parseObject(llmResponse);
                JSONArray choices = responseJson.getJSONArray("choices");
                if (choices == null || choices.isEmpty()) {
                    throw new AiServiceException("AI返回数据异常");
                }

                JSONObject choice = choices.getJSONObject(0);
                JSONObject message = choice.getJSONObject("message");

                if (message == null) {
                    throw new AiServiceException("AI返回消息为空");
                }

                JSONArray toolCalls = message.getJSONArray("tool_calls");

                if (toolCalls != null && !toolCalls.isEmpty()) {
                    ChatMessage assistantMsg = ChatMessage.builder()
                            .role("assistant")
                            .content(message.getString("content"))
                            .build();
                    messages.add(assistantMsg);

                    JSONArray processedToolCalls = new JSONArray();
                    for (int j = 0; j < toolCalls.size(); j++) {
                        JSONObject tc = toolCalls.getJSONObject(j);
                        String toolCallId = tc.getString("id");
                        JSONObject function = tc.getJSONObject("function");
                        String functionName = function.getString("name");
                        String arguments = function.getString("arguments");

                        log.info("AI调用工具: {} 参数: {}", functionName, arguments);

                        // Execute function
                        AiFunctionHandler handler = toolRegistry.getHandler(functionName);
                        String toolResult;
                        if (handler != null) {
                            toolResult = handler.execute(arguments, toolContext);
                        } else {
                            toolResult = "{\"error\": \"未知工具: " + functionName + "\"}";
                        }

                        toolCallRecords.add(ToolCallRecord.builder()
                                .functionName(functionName)
                                .arguments(arguments)
                                .result(toolResult)
                                .build());

                        messages.add(ChatMessage.builder()
                                .role("tool")
                                .toolCallId(toolCallId)
                                .content(toolResult)
                                .build());

                        // Track tool_calls for API format
                        JSONObject apiToolCall = new JSONObject();
                        apiToolCall.put("id", toolCallId);
                        apiToolCall.put("type", "function");
                        apiToolCall.put("function", function);
                        processedToolCalls.add(apiToolCall);
                    }

                    // Update assistant message with tool_calls in proper format
                    assistantMsg.setContent(null);
                    // Store tool_calls as JSON string in the message for serialization
                    assistantMsg.setToolName(processedToolCalls.toJSONString());

                    // Continue loop to send tool results back to LLM
                    continue;
                }

                // Text response - the final answer
                String reply = message.getString("content");
                if (reply == null || reply.trim().isEmpty()) {
                    throw new AiServiceException("AI未返回有效回复");
                }

                messages.add(ChatMessage.builder()
                        .role("assistant")
                        .content(reply)
                        .build());

                saveConversation(sessionId, messages);

                return ChatResponseDTO.builder()
                        .reply(reply)
                        .sessionId(sessionId)
                        .toolCalls(toolCallRecords.isEmpty() ? null : toolCallRecords)
                        .build();
            }

            throw new AiServiceException("AI处理超时，请重新提问");
        } catch (IOException e) {
            log.error("调用AI接口失败", e);
            throw new AiServiceException("AI服务通信异常，请稍后重试");
        }
    }

    private String callLlm(List<ChatMessage> messages, List<AiFunctionDefinition> tools) throws IOException {
        JSONObject body = new JSONObject();
        body.put("model", aiProperties.getModel());
        body.put("temperature", aiProperties.getTemperature());
        body.put("max_tokens", aiProperties.getMaxTokens());

        JSONArray messagesJson = new JSONArray();
        for (ChatMessage msg : messages) {
            JSONObject msgJson = new JSONObject();
            msgJson.put("role", msg.getRole());

            if ("tool".equals(msg.getRole())) {
                msgJson.put("tool_call_id", msg.getToolCallId());
                msgJson.put("content", msg.getContent());
            } else if ("assistant".equals(msg.getRole()) && msg.getToolName() != null) {
                // Assistant message with tool calls
                msgJson.put("content", msg.getContent());
                msgJson.put("tool_calls", JSON.parseArray(msg.getToolName()));
            } else {
                msgJson.put("content", msg.getContent());
            }

            messagesJson.add(msgJson);
        }
        body.put("messages", messagesJson);

        if (tools != null && !tools.isEmpty()) {
            JSONArray toolsJson = new JSONArray();
            for (AiFunctionDefinition tool : tools) {
                JSONObject toolJson = new JSONObject();
                toolJson.put("type", "function");

                JSONObject funcJson = new JSONObject();
                funcJson.put("name", tool.getName());
                funcJson.put("description", tool.getDescription());
                funcJson.put("parameters", JSON.parseObject(tool.getParametersJsonSchema()));

                toolJson.put("function", funcJson);
                toolsJson.add(toolJson);
            }
            body.put("tools", toolsJson);
        }

        Map<String, String> headers = new HashMap<>();
        headers.put("Authorization", "Bearer " + aiProperties.getApiKey());
        headers.put("Content-Type", "application/json");

        String url = aiProperties.getApiUrl() + "/chat/completions";
        log.debug("调用LLM API: {}", url);
        return HttpClientUtil.doPostJson(url, body.toJSONString(), headers);
    }

    private String buildSystemPrompt(String roleType) {
        if ("ADMIN".equals(roleType)) {
            return "你是\"" + merchantProperties.getName() + "\"管理后台的运营助手，名为\"小穹\"。\n" +
                    "你可以帮助管理员：\n" +
                    "1) 查询和分析订单数据（订单状态、订单列表、订单详情）\n" +
                    "2) 查看营业额、用户、订单等统计数据（指定日期范围）\n" +
                    "3) 查看销量排行榜 top10\n" +
                    "4) 查看今日运营概览（营业额、有效订单、完成率、菜品/套餐概览）\n" +
                    "5) 操作订单（接单、拒单、取消、完成、派送）\n" +
                    "6) 经营洞察：基于查询到的数据给出结论、问题诊断与运营建议\n" +
                    "7) 自动审单：根据订单信息、用户历史、取消/投诉理由给出处置建议与参考话术（仅建议，不直接改状态）\n" +
                    "回答要求：用专业、简洁的语言，回复结构为【结论 → 数据依据 → 行动建议】。所有数据均来自系统实时查询，请基于查询结果回答，不要凭空捏造数字。";
        }
        StringBuilder sb = new StringBuilder();
        sb.append("你是\"").append(merchantProperties.getName()).append("\"的智能客服，名为\"小食\"。\n");
        sb.append("你可以帮助用户：\n");
        sb.append("1) 推荐菜品，按分类、口味偏好、历史订单推荐\n");
        sb.append("2) 查询用户的订单状态和历史订单\n");
        sb.append("3) 查询菜品详情、菜单分类\n");
        sb.append("4) 回答配送、起送价、配送费、营业时间、退款政策、支付、投诉等问题\n");
        sb.append("5) 必要时主动调用 customer_service_faq / query_merchant_info 工具获取标准答案与商家信息\n");
        sb.append("\n【商家信息（已加载，无需重复查询）】\n");
        sb.append("- 商家名称：").append(merchantProperties.getName()).append("\n");
        sb.append("- 起送价：").append(merchantProperties.getMinOrderAmount()).append(" 元\n");
        sb.append("- 配送费：").append(merchantProperties.getDeliveryFee()).append(" 元\n");
        sb.append("- 配送范围：").append(merchantProperties.getDeliveryRange()).append(" 公里\n");
        sb.append("- 平均配送时长：").append(merchantProperties.getAvgDeliveryMinutes()).append(" 分钟\n");
        sb.append("- 营业时间：").append(merchantProperties.getOpenTime()).append(" ~ ").append(merchantProperties.getCloseTime()).append("\n");
        sb.append("- 客服电话：").append(merchantProperties.getServicePhone()).append("\n");
        sb.append("- 退款政策：").append(merchantProperties.getRefundPolicy()).append("\n");
        sb.append("\n【FAQ 知识库（已加载）】\n");
        if (faqProperties.getEntries() != null) {
            for (FaqProperties.Entry e : faqProperties.getEntries()) {
                if (e.getCategory() == null) continue;
                sb.append("- [").append(e.getCategory()).append("] 关键词：")
                        .append(e.getKeywords()).append("\n  答案：").append(e.getAnswer()).append("\n");
            }
        }
        sb.append("\n回答要求：\n");
        sb.append("- 优先用上面的【FAQ 知识库】和【商家信息】直接回答；需要时再调用工具\n");
        sb.append("- 用友好、热情的语气，避免冷冰冰的官方腔\n");
        sb.append("- 涉及具体价格/时间/政策的，引用上方已加载的数据，不要编造\n");
        sb.append("- 不确定或超出能力时，引导用户拨打客服 ").append(merchantProperties.getServicePhone()).append("\n");
        return sb.toString();
    }

    private String getRedisKey(String sessionId) {
        return REDIS_KEY_PREFIX + sessionId;
    }

    private List<ChatMessage> loadConversation(String sessionId) {
        try {
            String json = (String) redisTemplate.opsForValue().get(getRedisKey(sessionId));
            if (json != null && !json.isEmpty()) {
                return JSON.parseArray(json, ChatMessage.class);
            }
        } catch (Exception e) {
            log.warn("加载会话历史失败，使用新会话", e);
        }
        return new ArrayList<>();
    }

    private void saveConversation(String sessionId, List<ChatMessage> messages) {
        try {
            List<ChatMessage> toSave = messages;
            if (messages.size() > aiProperties.getMaxHistoryMessages()) {
                int start = messages.size() - aiProperties.getMaxHistoryMessages();
                // Keep system message if it's before the trim
                if (start > 0 && "system".equals(messages.get(0).getRole())) {
                    toSave = new ArrayList<>();
                    toSave.add(messages.get(0));
                    toSave.addAll(messages.subList(start, messages.size()));
                } else {
                    toSave = messages.subList(start, messages.size());
                }
            }
            String key = getRedisKey(sessionId);
            redisTemplate.opsForValue().set(key, JSON.toJSONString(toSave),
                    aiProperties.getConversationTtl(), TimeUnit.MINUTES);
        } catch (Exception e) {
            log.warn("保存会话历史失败", e);
        }
    }
}
