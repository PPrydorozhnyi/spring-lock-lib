package com.petro.locklib.exceptions;

import java.util.List;
import lombok.Getter;

@Getter
public class LockAcquireException extends RuntimeException {

  private final List<String> params;

  public LockAcquireException(String lockName, List<String> params) {
    super("Target " + lockName + " is locked already.");
    this.params = params;
  }

}
