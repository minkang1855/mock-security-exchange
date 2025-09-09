package edu.cnu.swacademy.security.auth;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import edu.cnu.swacademy.security.auth.dto.LoginRequest;
import edu.cnu.swacademy.security.auth.dto.LoginResponse;
import edu.cnu.swacademy.security.common.ErrorCode;
import edu.cnu.swacademy.security.common.HashUtil;
import edu.cnu.swacademy.security.common.JwtUtil;
import edu.cnu.swacademy.security.common.SecurityException;
import edu.cnu.swacademy.security.common.TokenInfo;
import edu.cnu.swacademy.security.user.User;
import edu.cnu.swacademy.security.user.UserRepository;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Service
public class AuthService {

  private final UserRepository userRepository;
  private final AuthenticationRepository authenticationRepository;
  private final JwtUtil jwtUtil;

  @Transactional
  public LoginResponse login(LoginRequest request) throws SecurityException {

    User user = userRepository.findByEmail(request.userEmail())
        .orElseThrow(() -> new SecurityException(ErrorCode.USER_NOT_FOUND));

    String hashedPassword = HashUtil.sha512(request.userPassword());
    if (!user.getPassword().equals(hashedPassword)) {
      throw new SecurityException(ErrorCode.INVALID_CREDENTIALS);
    }

    TokenInfo tokenInfo = jwtUtil.generateAccessAndRefreshToken(user.getId());

    authenticationRepository.save(
        new Authentication(
            user,
            tokenInfo.refreshToken(),
            tokenInfo.refreshTokenExpiredAt()
        )
    );

    return new LoginResponse(tokenInfo);
  }
}