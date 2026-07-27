package com.sky.controller.user;

import com.sky.dto.ChatRequestDTO;
import com.sky.dto.ChatResponseDTO;
import com.sky.result.Result;
import com.sky.service.AiService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController("userAiChatController")
@RequestMapping("/user/ai")
@Slf4j
@Api(tags = "C端AI助手")
public class AiChatController {

    @Autowired
    private AiService aiService;

    @PostMapping("/chat")
    @ApiOperation("AI对话")
    public Result<ChatResponseDTO> chat(@RequestBody ChatRequestDTO request) {
        log.info("用户端智能对话，sessionId: {}", request.getSessionId());
        ChatResponseDTO response = aiService.chat(request, "USER");
        return Result.success(response);
    }
}
