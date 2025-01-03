package com.sovannara.spring_boot_auth.auth;

import com.sovannara.spring_boot_auth.exception.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthenticationController {

    private final AuthenticationService service;

    @PostMapping("/register")
    ApiResponse<AuthenticationResponseDto> register(
            @RequestBody RegisterRequestDto registerRequestDto
    ) {
        return service.register(registerRequestDto);
    }

    @PostMapping("/login")
    ApiResponse<AuthenticationResponseDto> login(
            @RequestBody LoginRequestDto loginRequestDto
    ) {
        return service.login(loginRequestDto);
    }

    @GetMapping("/confirm")
    String confirm(
            @RequestParam String token
    ) {
        return service.confirm(token);
    }

    @PostMapping("/refresh-token")
    void refreshToken(
            HttpServletRequest request,
            HttpServletResponse response
    ) throws IOException {
        service.refreshToken(request, response);
    }

}
