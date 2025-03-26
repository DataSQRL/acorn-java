package com.datasqrl.ai.example;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "config")
@Data
public class ServerProperties {

  private String backendUrl = "http://localhost:8888/graphql";
}
