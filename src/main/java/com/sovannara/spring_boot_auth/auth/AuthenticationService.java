package com.sovannara.spring_boot_auth.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
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
public class AuthenticationService {

    private static final Logger logger = LoggerFactory.getLogger(AuthenticationService.class);
    @Value("${application.security.auth.confirmation-url}")
    private String CONFIRM_URL;

    private final UserRepository repository;
    private final PasswordEncoder passwordEncoder;
    private final TokenRepository tokenRepository;
    private final MailService mailService;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    @Transactional
    public AuthenticationResponseDto register(RegisterRequestDto registerRequestDto) {
        final boolean userExists = repository.findByEmail(registerRequestDto.getEmail()).isPresent();

        if (userExists) {
            throw new IllegalStateException("The user is already exists.");
        }

        // Encode the password
        String encodedPassword = passwordEncoder.encode(registerRequestDto.getPassword());

        var user = User.builder()
                .firstname(registerRequestDto.getFirstname())
                .lastname(registerRequestDto.getLastname())
                .email(registerRequestDto.getEmail())
                .password(encodedPassword)
                .role(Role.USER)
                .build();

        var savedUser = repository.save(user);
        var jwtToken = jwtService.generateToken(user);
        var refreshToken = jwtService.generateRefreshToken(user);

        saveUserToken(savedUser, jwtToken);

        // Send the confirmation email
        String username = registerRequestDto.getFirstname() + registerRequestDto.getLastname();
        try {
            mailService.send(
                    registerRequestDto.getEmail(),
                    username,
                    null,
                    String.format(CONFIRM_URL, jwtToken)
            );
        } catch (MessagingException e) {
            logger.error("Failed to send confirmation email", e);
        }

        return AuthenticationResponseDto
                .builder()
                .accessToken(jwtToken)
                .refreshToken(refreshToken)
                .build();
    }

    public AuthenticationResponseDto login(LoginRequestDto request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail(),
                        request.getPassword()
                )
        );

        var user = repository.findByEmail(request.getEmail())
                .orElseThrow();
        var accessToken = jwtService.generateToken(user);
        var refreshToken = jwtService.generateRefreshToken(user);

        revokeAllUserTokens(user);
        saveUserToken(user, accessToken);

        return AuthenticationResponseDto.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .build();
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

        tokenRepository.save(token);
    }

    private void revokeAllUserTokens(User user) {
        var validUserTokens = tokenRepository.findAllValidTokenByUser(user.getId());

        if (validUserTokens.isEmpty()) return;

        validUserTokens.forEach(token -> {
            token.setExpired(true);
            token.setRevoked(true);
        });

        tokenRepository.saveAll(validUserTokens);
    }

    public String confirm(String token) {
        // get the token
        Token savedToken = tokenRepository.findByToken(token)
                .orElseThrow(() -> new IllegalStateException("Token not found"));

        if (LocalDateTime.now().isAfter(savedToken.getExpiresAt())) {
            // Generate a token
            var jwtToken = jwtService.generateToken(savedToken.getUser());
            Token newToken = Token.builder()
                    .token(jwtToken)
                    .createdAt(LocalDateTime.now())
                    .expiresAt(LocalDateTime.now().plusMinutes(10))
                    .user(savedToken.getUser())
                    .build();

            tokenRepository.save(newToken);

            try {
                mailService.send(
                        savedToken.getUser().getEmail(),
                        savedToken.getUser().getFirstname(),
                        null,
                        String.format(CONFIRM_URL, jwtToken)
                );
            } catch (MessagingException e) {
                logger.error("Error sending email: {}", e.getMessage());
            }

            return "Token expired, a new token has been sent to your email";
        }

        User user = repository.findById(savedToken.getUser().getId())
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        repository.save(user);

        savedToken.setValidatedAt(LocalDateTime.now());
        tokenRepository.save(savedToken);
        return "<h1>Your account hase been successfully activated</h1>";
    }

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
        userEmail = jwtService.extractUsername(refreshToken);
        if (userEmail != null) {
            var user = this.repository.findByEmail(userEmail)
                    .orElseThrow();
            if (jwtService.isTokenValid(refreshToken, user)) {
                var accessToken = jwtService.generateToken(user);
                revokeAllUserTokens(user);
                saveUserToken(user, accessToken);
                var authResponse = AuthenticationResponseDto.builder()
                        .accessToken(accessToken)
                        .refreshToken(refreshToken)
                        .build();
                new ObjectMapper().writeValue(response.getOutputStream(), authResponse);
            }
        }
    }

}
