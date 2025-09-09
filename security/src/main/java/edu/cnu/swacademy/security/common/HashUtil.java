package edu.cnu.swacademy.security.common;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;

public class HashUtil {
  public static String sha512(String input) throws SecurityException {
    try {
      MessageDigest md = MessageDigest.getInstance("SHA-512");
      byte[] hashBytes = md.digest(input.getBytes(StandardCharsets.UTF_8));

      return Base64.getEncoder().encodeToString(hashBytes);
    } catch (Exception e) {
      throw new SecurityException(ErrorCode.PASSWORD_HASHING_FAILED, e);
    }
  }
}
