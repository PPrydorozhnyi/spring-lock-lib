package com.petro.locklib.locks;

import java.util.List;
import lombok.experimental.UtilityClass;

@UtilityClass
public class LockUtils {

  private static final String LOCK_PREFIX = "LOCK::";
  private static final String DELIMITER = "::";

  public static String getLockKey(String type, List<String> params) {
    return LOCK_PREFIX + type + DELIMITER + String.join(DELIMITER, params);
  }

}
