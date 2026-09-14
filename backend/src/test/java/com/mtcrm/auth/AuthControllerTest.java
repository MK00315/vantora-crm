package com.mtcrm.auth;

import com.mtcrm.config.AppProperties;
import com.mtcrm.user.Role;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AuthControllerTest {
    @Test
    void registrationRequiresEmailVerificationAndDoesNotCreateASession() throws Exception {
        AuthService service = mock(AuthService.class);
        MockMvc mvc = MockMvcBuilders.standaloneSetup(new AuthController(service, properties())).build();

        mvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"companyName\":\"Example Inc\",\"firstName\":\"Alex\",\"lastName\":\"Morgan\",\"email\":\"alex@example.test\",\"password\":\"Secret12345\"}"))
                .andExpect(status().isCreated())
                .andExpect(cookie().doesNotExist("mtcrm_refresh"))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Verify")));
        verify(service).register(any());
    }

    @Test
    void loginReturnsFrontendContractAndHttpOnlyRefreshCookie() throws Exception {
        AuthService service = mock(AuthService.class);
        UUID userId = UUID.randomUUID(), tenantId = UUID.randomUUID();
        var response = new AuthDtos.AuthResponse("access.jwt", 900,
                new AuthDtos.UserSummary(userId, "Alex", "Morgan", "alex@example.test", Role.COMPANY_ADMIN,
                        tenantId, "Example Inc", null));
        when(service.login(any(), any(), any())).thenReturn(new AuthService.Session(response, "opaque-refresh"));
        MockMvc mvc = MockMvcBuilders.standaloneSetup(new AuthController(service, properties())).build();

        mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"alex@example.test\",\"password\":\"Secret12345\"}"))
                .andExpect(status().isOk())
                .andExpect(cookie().value("mtcrm_refresh", "opaque-refresh"))
                .andExpect(cookie().httpOnly("mtcrm_refresh", true))
                .andExpect(jsonPath("$.accessToken").value("access.jwt"))
                .andExpect(jsonPath("$.expiresIn").value(900))
                .andExpect(jsonPath("$.user.tenantName").value("Example Inc"))
                .andExpect(jsonPath("$.user.role").value("COMPANY_ADMIN"));
    }

    private static AppProperties properties() {
        return new AppProperties(new AppProperties.Jwt("test-secret-longer-than-thirty-two-bytes-0123456789",
                Duration.ofMinutes(15), Duration.ofDays(30), Duration.ofMinutes(30), "test"),
                new AppProperties.Cookie(false, "Lax", ""), new AppProperties.Cors(List.of("http://localhost")),
                new AppProperties.Mail(false, "test@localhost"),
                new AppProperties.Storage("local", "./target/test", "http://localhost",
                        new AppProperties.Storage.S3("", "ap-south-1", "")), new AppProperties.Demo(false, ""));
    }
}
