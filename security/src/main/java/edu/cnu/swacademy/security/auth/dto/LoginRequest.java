package edu.cnu.swacademy.security.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequest(
    @NotBlank
    @Email
    @Size(min = 10, max = 100)
    String userEmail,

    @NotBlank
    @Size(min = 8, max = 32)
    String userPassword
) {}
