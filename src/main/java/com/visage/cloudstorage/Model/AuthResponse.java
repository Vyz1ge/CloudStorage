package com.visage.cloudstorage.Model;

import io.micrometer.common.lang.NonNullApi;
import lombok.*;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AuthResponse {
    private String token;
}
