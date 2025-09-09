package edu.cnu.swacademy.security.user;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import edu.cnu.swacademy.security.user.dto.UserSignupRequest;
import edu.cnu.swacademy.security.user.dto.UserSignupResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RequestMapping("/api/v1/user")
@RequiredArgsConstructor
@RestController
public class UserController {

  private final UserService userService;

  @PostMapping
  public UserSignupResponse signup(@Valid @RequestBody UserSignupRequest request) throws Exception {
    return userService.signup(request);
  }
}
