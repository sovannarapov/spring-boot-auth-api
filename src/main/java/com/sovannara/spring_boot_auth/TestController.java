package com.sovannara.spring_boot_auth;

import com.sovannara.spring_boot_auth.user.Role;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.annotation.Secured;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/test")
@RequiredArgsConstructor
public class TestController {

    @GetMapping
    @PreAuthorize("isAuthenticated()")  // Requires any authenticated user
    public String test() {
        return "Test API";
    }

    @GetMapping("/admin")
    @PreAuthorize("hasAuthority('ADMIN')") // Requires ADMIN role
    public String adminOnly() {
        return "Admin API";
    }

    @GetMapping("/user")
    @PreAuthorize("hasAuthority('USER')")  // Requires USER role
    public String userOnly() {
        return "User API";
    }

}
