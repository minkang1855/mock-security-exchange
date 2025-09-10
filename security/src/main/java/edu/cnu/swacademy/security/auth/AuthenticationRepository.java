package edu.cnu.swacademy.security.auth;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface AuthenticationRepository extends JpaRepository<Authentication, Integer> {

  Optional<Authentication> findByRefreshToken(String refreshToken);
  Optional<Authentication> findByUserIdAndRefreshToken(int userId, String refreshToken);
}
