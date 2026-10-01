package edu.deploylab;

import edu.deploylab.dto.*;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping({"/auth","/api/auth","/api/v1/auth"})
public class AuthController {
    private final AuthService auth; private final ApiMapper mapper;
    public AuthController(AuthService auth,ApiMapper mapper) {this.auth=auth;this.mapper=mapper;}
    @PostMapping("/register") @ResponseStatus(org.springframework.http.HttpStatus.CREATED)
    @io.swagger.v3.oas.annotations.security.SecurityRequirements
    public UserResponse register(@Valid @RequestBody RegisterRequest body) {return mapper.user(auth.register(mapper.register(body)));}
    @PostMapping("/login") @io.swagger.v3.oas.annotations.security.SecurityRequirements
    public AuthSessionResponse login(@Valid @RequestBody LoginRequest body) {return mapper.session(auth.login(mapper.login(body)));}
    @PostMapping("/refresh") @io.swagger.v3.oas.annotations.security.SecurityRequirements
    public AuthSessionResponse refresh(@Valid @RequestBody RefreshTokenRequest body) {return mapper.session(auth.refresh(new AuthService.Refresh(body.refreshToken())));}
    @GetMapping("/me") public UserResponse me(@AuthenticationPrincipal AuthService.Actor user) {return mapper.user(user);}
    @PostMapping("/logout") @ResponseStatus(org.springframework.http.HttpStatus.NO_CONTENT)
    public void logout(@RequestHeader("Authorization") String token) { auth.logout(token.substring(7)); }
}
