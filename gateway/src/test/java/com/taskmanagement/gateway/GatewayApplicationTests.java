package com.taskmanagement.gateway;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Basic smoke test that verifies the Spring application context
 * for the gateway loads successfully with the configured routes.
 */
@SpringBootTest
class GatewayApplicationTests {

    @Test
    void contextLoads() {
        // If the application context fails to start (e.g. due to a
        // misconfigured route or bean), this test will fail.
    }
}
