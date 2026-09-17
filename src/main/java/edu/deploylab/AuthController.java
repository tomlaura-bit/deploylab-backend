package edu.deploylab;

import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService auth;
    public AuthController(AuthService auth) { this.auth=auth; }
    @PostMapping("/register") @ResponseStatus(org.springframework.http.HttpStatus.CREATED)
    public AuthService.Actor register(@Valid @RequestBody AuthService.Register body) { return auth.register(body); }
    @PostMapping("/login") public AuthService.Session login(@Valid @RequestBody AuthService.Login body) { return auth.login(body); }
    @GetMapping("/me") public AuthService.Actor me(@AuthenticationPrincipal AuthService.Actor user) { return user; }
    @PostMapping("/logout") @ResponseStatus(org.springframework.http.HttpStatus.NO_CONTENT)
    public void logout(@RequestHeader("Authorization") String token) { auth.logout(token.substring(7)); }
}
