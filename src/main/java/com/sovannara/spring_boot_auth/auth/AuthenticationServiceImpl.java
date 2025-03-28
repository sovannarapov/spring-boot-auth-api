package com.sovannara.spring_boot_auth.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sovannara.spring_boot_auth.auth.dto.AuthenticationDto;
import com.sovannara.spring_boot_auth.auth.dto.LoginRequest;
import com.sovannara.spring_boot_auth.auth.dto.RegisterRequest;
import com.sovannara.spring_boot_auth.exception.ApiResponse;
import com.sovannara.spring_boot_auth.exception.BadRequestException;
import com.sovannara.spring_boot_auth.exception.UnauthorizedException;
import com.sovannara.spring_boot_auth.jwt.JwtService;
import com.sovannara.spring_boot_auth.token.Token;
import com.sovannara.spring_boot_auth.token.TokenType;
import com.sovannara.spring_boot_auth.user.Role;
import com.sovannara.spring_boot_auth.user.User;
import com.sovannara.spring_boot_auth.token.TokenRepository;
import com.sovannara.spring_boot_auth.user.UserRepository;
import com.sovannara.spring_boot_auth.mail.MailService;
import jakarta.mail.MessagingException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AuthenticationServiceImpl implements AuthenticationService {

    private static final Logger _logger = LoggerFactory.getLogger(AuthenticationServiceImpl.class);
    @Value("${application.security.auth.confirmation-url}")
    private String CONFIRM_URL;

    private final UserRepository _repository;
    private final PasswordEncoder _passwordEncoder;
    private final TokenRepository _tokenRepository;
    private final MailService _mailService;
    private final AuthenticationManager _authenticationManager;
    private final JwtService _jwtService;

    @Override
    @Transactional
    public ApiResponse<User> register(RegisterRequest request) {
        final boolean userExists = _repository.findByEmail(request.getEmail()).isPresent();

        if (userExists) {
            throw new BadRequestException("The email is already exists.");
        }

        // Encode the password
        String encodedPassword = _passwordEncoder.encode(request.getPassword());

        var user = User.builder()
                .firstname(request.getFirstname())
                .lastname(request.getLastname())
                .email(request.getEmail())
                .password(encodedPassword)
                .role(Role.USER)
                .build();
        var savedUser = _repository.save(user);
        var jwtToken = _jwtService.generateToken(user);

        _jwtService.generateRefreshToken(user);

        saveUserToken(savedUser, jwtToken);

        // Send the confirmation email
        String username = request.getFirstname() + request.getLastname();
        try {
            _mailService.send(
                    request.getEmail(),
                    username,
                    null,
                    String.format(CONFIRM_URL, jwtToken)
            );
        } catch (MessagingException e) {
            _logger.error("Failed to send confirmation email", e);
        }

        return ApiResponse.success(savedUser);
    }

    @Override
    public ApiResponse<AuthenticationDto> login(@NotNull LoginRequest request) {
        try {
            _authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getEmail(),
                            request.getPassword()
                    )
            );
        } catch (Exception e) {
            throw new UnauthorizedException("Incorrect email or password.");
        }

        var user = _repository.findByEmail(request.getEmail())
                .orElseThrow();
        var token = _tokenRepository.findByUserIdAndTokenType(user.getId(), TokenType.BEARER)
                .orElseThrow(() -> new UnauthorizedException("Token not found or not confirmed"));

        if (token.getValidatedAt() == null) {
            throw new UnauthorizedException("Token not confirmed");
        }
        var accessToken = _jwtService.generateToken(user);
        var refreshToken = _jwtService.generateRefreshToken(user);

        revokeAllUserTokens(user);
        saveUserToken(user, accessToken);

        return ApiResponse.success(AuthenticationDto.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .build());
    }

    private void saveUserToken(User user, String jwtToken) {
        var token = Token.builder()
                .user(user)
                .token(jwtToken)
                .createdAt(LocalDateTime.now())
                .expiresAt(LocalDateTime.now().plusMinutes(10))
                .tokenType(TokenType.BEARER)
                .expired(false)
                .revoked(false)
                .build();

        _tokenRepository.save(token);
    }

    private void revokeAllUserTokens(User user) {
        var validUserTokens = _tokenRepository.findAllValidTokenByUser(user.getId());

        if (validUserTokens.isEmpty()) return;

        validUserTokens.forEach(token -> {
            token.setExpired(true);
            token.setRevoked(true);
        });

        _tokenRepository.saveAll(validUserTokens);
    }

    @Override
    public String confirm(String token) {
        // get the token
        Token savedToken = _tokenRepository.findByToken(token)
                .orElseThrow(() -> new IllegalArgumentException("Token not found"));

        if (LocalDateTime.now().isAfter(savedToken.getExpiresAt())) {
            // Generate a token
            var jwtToken = _jwtService.generateToken(savedToken.getUser());
            Token newToken = Token.builder()
                    .token(jwtToken)
                    .createdAt(LocalDateTime.now())
                    .expiresAt(LocalDateTime.now().plusMinutes(10))
                    .user(savedToken.getUser())
                    .build();

            _tokenRepository.save(newToken);

            try {
                _mailService.send(
                        savedToken.getUser().getEmail(),
                        savedToken.getUser().getFirstname(),
                        null,
                        String.format(CONFIRM_URL, jwtToken)
                );
            } catch (MessagingException e) {
                _logger.error("Error sending email: {}", e.getMessage());
            }

            return "Token expired, a new token has been sent to your email";
        }

        if (savedToken.isExpired() || savedToken.isRevoked()) {
            throw new UnauthorizedException("Invalid token");
        }

        User user = _repository.findById(savedToken.getUser().getId())
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        _repository.save(user);

        savedToken.setValidatedAt(LocalDateTime.now());
        _tokenRepository.save(savedToken);
        return "<h1>Your account hase been successfully activated</h1>";
    }

    @Override
    public void refreshToken(
            HttpServletRequest request,
            HttpServletResponse response
    ) throws IOException {
        final String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
        final String refreshToken;
        final String userEmail;
        if (authHeader == null ||!authHeader.startsWith("Bearer ")) {
            return;
        }
        refreshToken = authHeader.substring(7);
        userEmail = _jwtService.extractUsername(refreshToken);
        if (userEmail != null) {
            var user = this._repository.findByEmail(userEmail)
                    .orElseThrow();
            if (_jwtService.isTokenValid(refreshToken, user)) {
                var accessToken = _jwtService.generateToken(user);
                revokeAllUserTokens(user);
                saveUserToken(user, accessToken);
                var authResponse = AuthenticationDto.builder()
                        .accessToken(accessToken)
                        .refreshToken(refreshToken)
                        .build();
                new ObjectMapper().writeValue(response.getOutputStream(), authResponse);
            }
        }
    }

}
