package com.datasqrl.ai.spring;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "config")
@Data
public class ServerProperties {

  private String memoryUrl;
  private String toolsUrl;

}
