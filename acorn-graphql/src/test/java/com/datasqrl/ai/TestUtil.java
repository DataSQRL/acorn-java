package com.datasqrl.ai;

import static java.nio.file.StandardOpenOption.CREATE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

import com.datasqrl.ai.util.ErrorHandling;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import lombok.SneakyThrows;

public class TestUtil {

  public static URL getResourceFile(String path) {
    URL url = TestUtil.class.getClassLoader().getResource(path);
    ErrorHandling.checkArgument(url != null, "Invalid url: %s", url);
    return url;
  }

  @SneakyThrows
  public static String getResourcesFileAsString(String path) {
    Path uriPath = Path.of(getResourceFile(path).toURI());
    return Files.readString(uriPath, StandardCharsets.UTF_8);
  }

  @SneakyThrows
  public static void snapshotTest(String result, Path pathToExpected) {
    if (Files.isRegularFile(pathToExpected)) {
      String expected = Files.readString(pathToExpected, StandardCharsets.UTF_8);
      assertEquals(expected, result);
    } else {
      Files.writeString(pathToExpected, result, StandardCharsets.UTF_8, CREATE);
      fail("Created snapshot: " + pathToExpected.toAbsolutePath());
    }
  }
}
