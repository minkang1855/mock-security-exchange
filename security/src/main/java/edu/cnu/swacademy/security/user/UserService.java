package edu.cnu.swacademy.security.user;

import edu.cnu.swacademy.security.common.ErrorCode;
import edu.cnu.swacademy.security.common.HashUtil;
import edu.cnu.swacademy.security.common.SecurityException;
import edu.cnu.swacademy.security.user.dto.UserSignupRequest;
import edu.cnu.swacademy.security.user.dto.UserSignupResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {

  private final UserRepository userRepository;

  @Transactional
  public UserSignupResponse signup(UserSignupRequest request) throws Exception {
    if (userRepository.existsByEmail(request.userEmail())) {
      throw new SecurityException(ErrorCode.EMAIL_ALREADY_EXISTS);
    }

    User user = new User(request.userName(), request.userEmail(), HashUtil.sha512(request.userPassword()));

    User savedUser = userRepository.save(user);

    return new UserSignupResponse(savedUser.getId());
  }
}
