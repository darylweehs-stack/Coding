package com.example.messenger.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;

public record SendMessageRequest(
        @NotNull Long userId,
        @NotBlank @Size(max = 5000) String content,
        @NotNull Instant timestamp) {
}