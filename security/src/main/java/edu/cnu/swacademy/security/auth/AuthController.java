package edu.cnu.swacademy.security.auth;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import edu.cnu.swacademy.security.auth.dto.LoginRequest;
import edu.cnu.swacademy.security.auth.dto.LoginResponse;
import edu.cnu.swacademy.security.auth.dto.TokenReissueRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

  private final AuthService authService;

  @PostMapping("/login")
  public LoginResponse login(@Valid @RequestBody LoginRequest request) throws Exception {
    return authService.login(request);
  }

  @PutMapping("/reissue")
  public LoginResponse reissue(@Valid @RequestBody TokenReissueRequest request) throws Exception {
    return authService.reissue(request);
  }
}
