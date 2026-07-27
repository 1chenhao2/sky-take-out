package com.sky.dto;

import lombok.Data;

import java.io.Serializable;

@Data
public class ChatRequestDTO implements Serializable {

    private String message;

    private String sessionId;

}
