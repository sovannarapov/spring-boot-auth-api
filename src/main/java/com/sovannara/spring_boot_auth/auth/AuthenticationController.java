package com.sovannara.spring_boot_auth.auth;

import com.sovannara.spring_boot_auth.auth.dto.AuthenticationResponseDto;
import com.sovannara.spring_boot_auth.auth.dto.LoginRequestDto;
import com.sovannara.spring_boot_auth.auth.dto.RegisterRequestDto;
import com.sovannara.spring_boot_auth.exception.ApiResponse;
import com.sovannara.spring_boot_auth.user.User;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthenticationController {

    private final AuthenticationService _service;

    @PostMapping("/register")
    ApiResponse<User> register(@RequestBody RegisterRequestDto registerRequestDto) {
        return _service.register(registerRequestDto);
    }

    @PostMapping("/login")
    ApiResponse<AuthenticationResponseDto> login(@RequestBody LoginRequestDto loginRequestDto) {
        return _service.login(loginRequestDto);
    }

    @GetMapping("/confirm")
    String confirm(@RequestParam String token) {
        return _service.confirm(token);
    }

    @PostMapping("/refresh-token")
    void refreshToken(HttpServletRequest request, HttpServletResponse response) throws IOException {
        _service.refreshToken(request, response);
    }

}
