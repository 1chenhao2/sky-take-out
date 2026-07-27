package com.sky.ai.tool;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiFunctionDefinition {

    private String name;

    private String description;

    private String parametersJsonSchema;

}
