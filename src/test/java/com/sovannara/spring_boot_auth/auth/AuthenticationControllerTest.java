package com.sovannara.spring_boot_auth.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sovannara.spring_boot_auth.auth.dto.AuthenticationResponseDto;
import com.sovannara.spring_boot_auth.auth.dto.LoginRequestDto;
import com.sovannara.spring_boot_auth.auth.dto.RegisterRequestDto;
import com.sovannara.spring_boot_auth.config.SecurityConfigTest;
import com.sovannara.spring_boot_auth.exception.ApiResponse;
import com.sovannara.spring_boot_auth.exception.BadRequestException;
import com.sovannara.spring_boot_auth.exception.UnauthorizedException;
import com.sovannara.spring_boot_auth.jwt.JwtService;
import com.sovannara.spring_boot_auth.user.Role;
import com.sovannara.spring_boot_auth.user.User;
import com.sovannara.spring_boot_auth.user.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthenticationController.class)
@Import(SecurityConfigTest.class)
public class AuthenticationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuthenticationService authenticationService;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private UserService userService;

    @Test
    void shouldRegisterNewUser() throws Exception {
        RegisterRequestDto user = new RegisterRequestDto("John", "Wick", "johnwick@gmail.com", "password");
        User responseDto = User.builder()
                .firstname("John")
                .lastname("Wick")
                .email("johnwick@gmail.com")
                .password("password")
                .role(Role.USER)
                .build();

        when(authenticationService.register(any(RegisterRequestDto.class))).thenReturn(ApiResponse.success(responseDto));

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(user)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.firstname").value("John"))
                .andExpect(jsonPath("$.data.lastname").value("Wick"))
                .andExpect(jsonPath("$.data.email").value("johnwick@gmail.com"))
                .andExpect(jsonPath("$.data.role").value("USER"));
    }

    @Test
    void shouldLoginUser() throws Exception {
        LoginRequestDto user = new LoginRequestDto("johnwick@gmail.com", "password");
        AuthenticationResponseDto responseDto = new AuthenticationResponseDto("accessToken", "refreshToken");

        when(authenticationService.login(any(LoginRequestDto.class))).thenReturn(ApiResponse.success(responseDto));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(user)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.access_token").value("accessToken"))
                .andExpect(jsonPath("$.data.refresh_token").value("refreshToken"));
    }

    @Test
    void shouldConfirmUser() throws Exception {
        String token = "sampleToken";
        String responseMessage = "<h1>Your account has been successfully activated</h1>";

        when(authenticationService.confirm(token)).thenReturn(responseMessage);

        mockMvc.perform(get("/api/auth/confirm")
                        .param("token", token))
                .andExpect(status().isOk())
                .andExpect(content().string(responseMessage));
    }

    @Test
    void shouldRefreshToken() throws Exception {
        String refreshToken = "Bearer sampleRefreshToken";
        AuthenticationResponseDto responseDto = new AuthenticationResponseDto("newAccessToken", "newRefreshToken");

        when(jwtService.extractUsername(any(String.class))).thenReturn("johnwick@gmail.com");
        doAnswer(invocation -> {
            HttpServletResponse response = invocation.getArgument(1);
            new ObjectMapper().writeValue(response.getOutputStream(), responseDto);
            return null;
        }).when(authenticationService).refreshToken(any(HttpServletRequest.class), any(HttpServletResponse.class));

        mockMvc.perform(post("/api/auth/refresh-token")
                        .header(HttpHeaders.AUTHORIZATION, refreshToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.access_token").value("newAccessToken"))
                .andExpect(jsonPath("$.refresh_token").value("newRefreshToken"));
    }

    @Test
    void shouldNotRegisterUserWithExistingEmail() throws Exception {
        RegisterRequestDto user = new RegisterRequestDto("John", "Wick", "johnwick@gmail.com", "password");

        when(authenticationService.register(any(RegisterRequestDto.class)))
                .thenThrow(new BadRequestException("The email is already exists."));

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(user)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("The email is already exists."));
    }

    @Test
    void shouldNotLoginWithIncorrectPassword() throws Exception {
        LoginRequestDto user = new LoginRequestDto("johnwick@gmail.com", "wrongpassword");

        when(authenticationService.login(any(LoginRequestDto.class)))
                .thenThrow(new UnauthorizedException("Incorrect email or password."));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(user)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Incorrect email or password."));
    }

    @Test
    void shouldNotConfirmWithInvalidToken() throws Exception {
        String token = "invalidToken";

        when(authenticationService.confirm(token)).thenThrow(new UnauthorizedException("Invalid token"));

        mockMvc.perform(get("/api/auth/confirm")
                        .param("token", token))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid token"));
    }

}
