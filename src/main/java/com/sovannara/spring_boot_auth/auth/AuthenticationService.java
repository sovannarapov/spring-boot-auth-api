package com.sovannara.spring_boot_auth.auth;

import com.sovannara.spring_boot_auth.auth.dto.AuthenticationResponseDto;
import com.sovannara.spring_boot_auth.auth.dto.LoginRequestDto;
import com.sovannara.spring_boot_auth.auth.dto.RegisterRequestDto;
import com.sovannara.spring_boot_auth.exception.ApiResponse;
import com.sovannara.spring_boot_auth.user.User;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

public interface AuthenticationService {

    ApiResponse<User> register(RegisterRequestDto registerRequestDto);

    ApiResponse<AuthenticationResponseDto> login(LoginRequestDto request);

    String confirm(String token);

    void refreshToken(HttpServletRequest request, HttpServletResponse response) throws IOException;

}
