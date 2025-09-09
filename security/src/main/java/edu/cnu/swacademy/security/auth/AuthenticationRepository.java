package edu.cnu.swacademy.security.auth;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuthenticationRepository extends JpaRepository<Authentication, Long> {

  Optional<Authentication> findByRefreshToken(String refreshToken);

  List<Authentication> findByUserId(Long userId);

  void deleteByRefreshToken(String refreshToken);
}
