package com.sovannara.spring_boot_auth.auth;

import com.sovannara.spring_boot_auth.auth.dto.AuthenticationDto;
import com.sovannara.spring_boot_auth.auth.dto.LoginRequest;
import com.sovannara.spring_boot_auth.auth.dto.RegisterRequest;
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
    ApiResponse<User> register(@RequestBody RegisterRequest request) {
        return _service.register(request);
    }

    @PostMapping("/login")
    ApiResponse<AuthenticationDto> login(@RequestBody LoginRequest request) {
        return _service.login(request);
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
