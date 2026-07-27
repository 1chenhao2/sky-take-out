package com.sky.service;

import com.sky.dto.ChatRequestDTO;
import com.sky.dto.ChatResponseDTO;

public interface AiService {

    ChatResponseDTO chat(ChatRequestDTO request, String roleType);

}
