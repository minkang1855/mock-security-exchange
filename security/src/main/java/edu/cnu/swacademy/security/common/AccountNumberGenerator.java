package edu.cnu.swacademy.security.common;

import java.security.SecureRandom;

public class AccountNumberGenerator {
  
  private static final SecureRandom random = new SecureRandom();
  private static final String PREFIX = "777";
  private static final String SUFFIX = "1";
  private static final int MIDDLE_DIGITS_COUNT = 8;
  
  public static String generateAccountNumber() {
    StringBuilder accountNumber = new StringBuilder();
    accountNumber.append(PREFIX);
    
    for (int i = 0; i < MIDDLE_DIGITS_COUNT; i++) {
      accountNumber.append(random.nextInt(10));
    }
    
    accountNumber.append(SUFFIX);
    return accountNumber.toString();
  }
}
