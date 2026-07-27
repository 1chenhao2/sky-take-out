package com.sky.ai.tool;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiToolContext {

    private Long currentUserId;

    private String roleType;

    @Builder.Default
    private Map<String, Object> extra = new HashMap<>();

}
