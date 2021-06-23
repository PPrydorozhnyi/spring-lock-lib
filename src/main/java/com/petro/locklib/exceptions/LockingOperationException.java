package com.petro.locklib.exceptions;

import lombok.Getter;

@Getter
public class LockingOperationException extends RuntimeException {

  public LockingOperationException(String message, Exception cause) {
    super(message, cause);
  }

}
