package com.sovannara.spring_boot_auth.user;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Integer> {

    // SDP = Spring Data Pattern (Allow us to create query without writing query)
    // SELECT * FROM users WHERE email = 'user1@gmail.com'
    Optional<User> findByEmail(String email);

}
