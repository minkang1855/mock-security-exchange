package edu.cnu.swacademy.security.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UserSignupRequest(
    @NotBlank
    @Email
    @Size(min = 10, max = 100)
    String userEmail,

    @NotBlank
    @Size(min = 2, max = 10)
    String userName,

    @NotBlank
    @Size(min = 8, max = 32)
    String userPassword
) {}
