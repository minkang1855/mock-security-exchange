package edu.cnu.swacademy.security.auth;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import edu.cnu.swacademy.security.auth.dto.LoginRequest;
import edu.cnu.swacademy.security.auth.dto.LoginResponse;
import edu.cnu.swacademy.security.auth.dto.TokenReissueRequest;
import edu.cnu.swacademy.security.common.ErrorCode;
import edu.cnu.swacademy.security.common.HashUtil;
import edu.cnu.swacademy.security.common.JwtUtil;
import edu.cnu.swacademy.security.common.SecurityException;
import edu.cnu.swacademy.security.common.TokenInfo;
import edu.cnu.swacademy.security.user.User;
import edu.cnu.swacademy.security.user.UserRepository;
import lombok.RequiredArgsConstructor;

@Slf4j
@RequiredArgsConstructor
@Service
public class AuthService {

  private final UserRepository userRepository;
  private final AuthenticationRepository authenticationRepository;
  private final JwtUtil jwtUtil;

  @Transactional(rollbackFor = Exception.class)
  public LoginResponse login(LoginRequest request) throws SecurityException {

    User user = userRepository.findByEmail(request.userEmail())
        .orElseThrow(() -> {
          log.info("User not found with email: {}", request.userEmail());
          return new SecurityException(ErrorCode.USER_NOT_FOUND);
        });
    log.info("Got user id (={})", user.getId());

    String hashedPassword = HashUtil.sha512(request.userPassword());
    if (!user.getPassword().equals(hashedPassword)) {
      log.info("Invalid credentials for email: {}", request.userEmail());
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

  @Transactional(rollbackFor = Exception.class)
  public LoginResponse reissue(TokenReissueRequest request) throws SecurityException {

    if (!jwtUtil.validateToken(request.refreshToken())) {
      throw new SecurityException(ErrorCode.REFRESH_TOKEN_INVALID);
    }

    int userId = jwtUtil.getUserIdFromToken(request.refreshToken());

    Authentication authentication = authenticationRepository.findByUserIdAndRefreshToken(userId, request.refreshToken())
        .orElseThrow(() -> {
          log.info("Refresh token not found for userId: {}", userId);
          return new SecurityException(ErrorCode.REFRESH_TOKEN_NOT_FOUND);
        });
    log.info("Got authentication id (={})", authentication.getId());

    TokenInfo newTokenInfo = jwtUtil.generateAccessAndRefreshToken(userId);

    authentication.updateRefreshToken(newTokenInfo.refreshToken(), newTokenInfo.refreshTokenExpiredAt());

    return new LoginResponse(newTokenInfo);
  }
}