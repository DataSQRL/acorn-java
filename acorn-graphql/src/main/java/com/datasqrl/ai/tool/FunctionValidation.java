package com.datasqrl.ai.tool;

import lombok.NonNull;

public record FunctionValidation(
    @NonNull ErrorType errorType,
    String errorMessage
) {

  public boolean isValid() {
    return errorType==ErrorType.NONE;
  }

  public enum ErrorType {
    NONE,
    FUNCTION_NOT_FOUND,
    INVALID_JSON
  }

}
