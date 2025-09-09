package edu.cnu.swacademy.security.auth;

import java.time.LocalDateTime;

import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import edu.cnu.swacademy.security.common.BaseEntity;
import edu.cnu.swacademy.security.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@SQLRestriction("deleted_at IS NULL")
@SQLDelete(sql = "UPDATE authentication SET deleted_at = CURRENT_TIMESTAMP WHERE id = ?")
@NoArgsConstructor(access = lombok.AccessLevel.PROTECTED)
@Table(name = "authentication")
@Entity
public class Authentication extends BaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(columnDefinition = "INT UNSIGNED")
  private Long id;

  @OneToOne(fetch = FetchType.LAZY)
  @JoinColumn(nullable = false)
  private User user;

  @Column(nullable = false, length = 500, unique = true)
  private String refreshToken;

  @Column(nullable = false)
  private LocalDateTime expiredAt;

  public Authentication(User user, String refreshToken, LocalDateTime expiredAt) {
    this.user = user;
    this.refreshToken = refreshToken;
    this.expiredAt = expiredAt;
  }

  public void updateRefreshToken(String refreshToken, LocalDateTime expiredAt) {
    this.refreshToken = refreshToken;
    this.expiredAt = expiredAt;
  }
}
