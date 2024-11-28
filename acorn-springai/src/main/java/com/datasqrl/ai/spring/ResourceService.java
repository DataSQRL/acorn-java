package com.datasqrl.ai.spring;

import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;

import java.nio.file.Files;
import java.nio.file.Paths;

@Service
public class ResourceService {

  @Autowired
  private ResourceLoader resourceLoader;

  @SneakyThrows
  public String loadResourceFileAsString(String filePath) {
    Resource resource = resourceLoader.getResource("classpath:" + filePath);
    byte[] resourceBytes = Files.readAllBytes(Paths.get(resource.getURI()));
    return new String(resourceBytes);
  }
}