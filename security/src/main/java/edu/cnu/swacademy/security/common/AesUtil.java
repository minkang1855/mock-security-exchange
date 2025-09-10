package edu.cnu.swacademy.security.common;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class AesUtil {
  
  private static final String ALGORITHM = "AES";
  private static final String TRANSFORMATION = "AES/CBC/PKCS5Padding";
  private static final int IV_LENGTH = 16;
  
  private final SecretKeySpec secretKey;
  private final IvParameterSpec iv;
  
  public AesUtil(@Value("${aes.secret-key}") String secretKey) {
    this.secretKey = new SecretKeySpec(secretKey.getBytes(StandardCharsets.UTF_8), ALGORITHM);
    this.iv = new IvParameterSpec(secretKey.substring(0, IV_LENGTH).getBytes(StandardCharsets.UTF_8));
  }
  
  public String encrypt(String plainText) throws SecurityException {
    try {
      Cipher cipher = Cipher.getInstance(TRANSFORMATION);
      cipher.init(Cipher.ENCRYPT_MODE, secretKey, iv);
      
      byte[] encryptedBytes = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));
      return Base64.getEncoder().encodeToString(encryptedBytes);
    } catch (Exception e) {
      log.info("Failed to encrypt text. e = {}, msg = {}", e.getClass(), e.getMessage());
      throw new SecurityException(ErrorCode.ENCRYPTION_FAILED, e);
    }
  }
  
  public String decrypt(String encryptedText) throws SecurityException {
    try {
      Cipher cipher = Cipher.getInstance(TRANSFORMATION);
      cipher.init(Cipher.DECRYPT_MODE, secretKey, iv);
      
      byte[] encryptedBytes = Base64.getDecoder().decode(encryptedText);
      byte[] decryptedBytes = cipher.doFinal(encryptedBytes);
      return new String(decryptedBytes, StandardCharsets.UTF_8);
    } catch (Exception e) {
      log.info("Failed to decrypt text. e = {}, msg = {}", e.getClass(), e.getMessage());
      throw new SecurityException(ErrorCode.DECRYPTION_FAILED, e);
    }
  }
}
