package com.mtcrm.auth;

import com.mtcrm.common.api.MessageResponse;
import com.mtcrm.config.AppProperties;
import com.mtcrm.security.CurrentUser;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.HttpStatus;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {
    private static final String COOKIE = "mtcrm_refresh";
    private final AuthService auth;
    private final AppProperties properties;

    public AuthController(AuthService auth, AppProperties properties) { this.auth = auth; this.properties = properties; }

    @PostMapping("/register")
    ResponseEntity<MessageResponse> register(@Valid @RequestBody AuthDtos.RegisterRequest request) {
        auth.register(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new MessageResponse("Check your email. Verify the signup to create your workspace"));
    }

    @PostMapping("/login")
    ResponseEntity<AuthDtos.AuthResponse> login(@Valid @RequestBody AuthDtos.LoginRequest request,
                                                 HttpServletRequest http) {
        return session(auth.login(request, http.getHeader("User-Agent"), clientIp(http)));
    }

    @PostMapping("/refresh")
    ResponseEntity<AuthDtos.AuthResponse> refresh(@CookieValue(name = COOKIE, required = false) String token,
                                                   HttpServletRequest http) {
        return session(auth.refresh(token, http.getHeader("User-Agent"), clientIp(http)));
    }

    @PostMapping("/logout")
    ResponseEntity<MessageResponse> logout(@CookieValue(name = COOKIE, required = false) String token) {
        auth.logout(token);
        return ResponseEntity.ok().header(HttpHeaders.SET_COOKIE, clearCookie().toString())
                .body(new MessageResponse("Signed out"));
    }

    @GetMapping("/me")
    AuthDtos.UserSummary me(@AuthenticationPrincipal CurrentUser user) { return auth.me(user.id(), user.tenantId()); }

    @PostMapping("/forgot-password")
    MessageResponse forgot(@Valid @RequestBody AuthDtos.ForgotPasswordRequest request) {
        auth.forgotPassword(request.email());
        return new MessageResponse("If that account exists, a reset link has been sent");
    }

    @PostMapping("/reset-password")
    MessageResponse reset(@Valid @RequestBody AuthDtos.ResetPasswordRequest request) {
        auth.resetPassword(request.token(), request.password());
        return new MessageResponse("Password updated");
    }

    @PostMapping("/verify-email")
    MessageResponse verify(@Valid @RequestBody AuthDtos.VerifyEmailRequest request) {
        auth.verifyEmail(request.token());
        return new MessageResponse("Email verified");
    }

    @PostMapping("/resend-verification")
    MessageResponse resendVerification(@Valid @RequestBody AuthDtos.ForgotPasswordRequest request) {
        auth.resendVerification(request.email());
        return new MessageResponse("If verification is still required, a new link has been sent");
    }

    private ResponseEntity<AuthDtos.AuthResponse> session(AuthService.Session session) {
        return ResponseEntity.ok().header(HttpHeaders.SET_COOKIE, refreshCookie(session.refreshToken()).toString())
                .body(session.response());
    }

    private ResponseCookie refreshCookie(String value) {
        var builder = ResponseCookie.from(COOKIE, value).httpOnly(true).secure(properties.cookie().secure())
                .sameSite(properties.cookie().sameSite()).path("/api/v1/auth").maxAge(properties.jwt().refreshTtl());
        if (properties.cookie().domain() != null && !properties.cookie().domain().isBlank()) builder.domain(properties.cookie().domain());
        return builder.build();
    }

    private ResponseCookie clearCookie() {
        var builder = ResponseCookie.from(COOKIE, "").httpOnly(true).secure(properties.cookie().secure())
                .sameSite(properties.cookie().sameSite()).path("/api/v1/auth").maxAge(0);
        if (properties.cookie().domain() != null && !properties.cookie().domain().isBlank()) builder.domain(properties.cookie().domain());
        return builder.build();
    }

    private static String clientIp(HttpServletRequest request) {
        return request.getRemoteAddr();
    }
}
