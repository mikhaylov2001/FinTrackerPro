package com.example.fintrackerpro;

import com.example.fintrackerpro.security.JwtUtil;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles("test")
@SpringBootTest(properties = {
        "google.client-id=test-client-id",
        "google.client-secret=test-client-secret",
        "google.redirect-uri=http://localhost/test-callback"
})
class FinTrackerProApplicationTests {

    @MockBean
    private JwtUtil jwtUtil;

    @Test
    void contextLoads() {
    }

}
