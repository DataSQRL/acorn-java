package com.datasqrl.ai.acorn;

import java.nio.file.Files;
import java.nio.file.Paths;
import lombok.SneakyThrows;
import org.springframework.core.io.Resource;

public class AcornSpringAIUtils {

  @SneakyThrows
  public static String loadResourceAsString(Resource resource) {
    byte[] resourceBytes = Files.readAllBytes(Paths.get(resource.getURI()));
    return new String(resourceBytes);
  }
}
