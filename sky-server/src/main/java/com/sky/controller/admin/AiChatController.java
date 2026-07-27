package com.sky.controller.admin;

import com.sky.dto.ChatRequestDTO;
import com.sky.dto.ChatResponseDTO;
import com.sky.result.Result;
import com.sky.service.AiService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/ai")
@Slf4j
@Api(tags = "管理端AI助手")
public class AiChatController {

    @Autowired
    private AiService aiService;
    @Autowired
    private RedisTemplate redisTemplate;

    @PostMapping("/chat")
    @ApiOperation("AI对话")
    public Result<ChatResponseDTO> chat(@RequestBody ChatRequestDTO request) {
        log.info("管理端智能对话，sessionId: {}", request.getSessionId());
        ChatResponseDTO response = aiService.chat(request, "ADMIN");
        return Result.success(response);
    }

    @DeleteMapping("/session/{sessionId}")
    @ApiOperation("清除会话")
    public Result clearSession(@PathVariable String sessionId) {
        log.info("清除AI会话: {}", sessionId);
        redisTemplate.delete("ai:chat:session:" + sessionId);
        return Result.success();
    }
}
