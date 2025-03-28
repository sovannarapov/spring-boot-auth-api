package com.sovannara.spring_boot_auth.auth;

import com.sovannara.spring_boot_auth.auth.dto.AuthenticationDto;
import com.sovannara.spring_boot_auth.auth.dto.LoginRequest;
import com.sovannara.spring_boot_auth.auth.dto.RegisterRequest;
import com.sovannara.spring_boot_auth.exception.ApiResponse;
import com.sovannara.spring_boot_auth.user.User;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

public interface AuthenticationService {

    ApiResponse<User> register(RegisterRequest request);

    ApiResponse<AuthenticationDto> login(LoginRequest request);

    String confirm(String token);

    void refreshToken(HttpServletRequest request, HttpServletResponse response) throws IOException;

}
