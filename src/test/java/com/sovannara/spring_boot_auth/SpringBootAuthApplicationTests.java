package com.sovannara.spring_boot_auth;

import com.sovannara.spring_boot_auth.config.SecurityConfigTest;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@SpringBootTest
@Import({SpringBootAuthApplication.class, SecurityConfigTest.class})
class SpringBootAuthApplicationTests {

	@Test
	void contextLoads() {
	}

}
