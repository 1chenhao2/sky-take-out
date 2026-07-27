package com.sky.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "sky.local-file")
@Data
public class LocalFileProperties {

    private String uploadPath;

    private String accessPath;

}
