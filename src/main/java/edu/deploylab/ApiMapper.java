package edu.deploylab;

import edu.deploylab.dto.*;
import org.springframework.stereotype.Component;

@Component
public class ApiMapper {
    AuthService.Register register(RegisterRequest request){return new AuthService.Register(request.name(),request.email(),request.password());}
    AuthService.Login login(LoginRequest request){return new AuthService.Login(request.email(),request.password());}
    UserResponse user(AuthService.Actor actor){return new UserResponse(actor.id(),actor.name(),actor.email(),actor.role());}
    AuthSessionResponse session(AuthService.Session session){
        return new AuthSessionResponse(session.token(),session.refreshToken(),session.expiresAt(),user(session.user()));
    }
}
