package com.datasqrl.ai;

import com.datasqrl.ai.util.ErrorHandling;
import com.google.common.io.Resources;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import lombok.SneakyThrows;

public class TestUtil {

  public static URL getResourceFile(String path) {
    URL url = TestUtil.class.getClassLoader().getResource(path);
    ErrorHandling.checkArgument(url!=null, "Invalid url: %s", url);
    return url;
  }

  @SneakyThrows
  public static String getResourcesFileAsString(String path) {
    return Resources.toString(getResourceFile(path), StandardCharsets.UTF_8);
  }

}
