package com.datasqrl.ai.tool;

import lombok.NonNull;

public record ValidationResult(
    @NonNull ErrorType errorType,
    String errorMessage
) {

  public static final ValidationResult VALID = new ValidationResult(ErrorType.NONE, null);

  public boolean isValid() {
    return errorType==ErrorType.NONE;
  }

  public enum ErrorType {
    NONE,
    NOT_FOUND,
    INVALID_JSON,
    INVALID_ARGUMENT
  }

}
