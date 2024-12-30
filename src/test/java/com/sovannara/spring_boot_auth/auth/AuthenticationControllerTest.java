package com.sovannara.spring_boot_auth.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sovannara.spring_boot_auth.config.SecurityConfigTest;
import com.sovannara.spring_boot_auth.jwt.JwtService;
import com.sovannara.spring_boot_auth.user.Role;
import com.sovannara.spring_boot_auth.user.User;
import com.sovannara.spring_boot_auth.user.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.ArrayList;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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

    private final List<User> users = new ArrayList<>();

    @BeforeEach
    void setUp() {
        users.add(new User(
                1,
                "John",
                "Doe",
                "johndoe@gmail.com",
                "password",
                Role.USER
        ));
    }

    @Test
    void shouldRegisterNewUser() throws Exception {
        var user = new User(2, "John", "Wick", "johnwick@gmail.com", "password", Role.USER);
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(user)))
                .andExpect(status().isOk());
    }

}
