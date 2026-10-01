package edu.deploylab;

import edu.deploylab.dto.*;
import java.util.*;
import org.springframework.stereotype.Component;

@Component
public class ApiMapper {
    AuthService.Register register(RegisterRequest request){return new AuthService.Register(request.name(),request.email(),request.password());}
    AuthService.Login login(LoginRequest request){return new AuthService.Login(request.email(),request.password());}
    UserResponse user(AuthService.Actor actor){return new UserResponse(actor.id(),actor.name(),actor.email(),actor.role());}
    AuthSessionResponse session(AuthService.Session session){
        return new AuthSessionResponse(session.token(),session.refreshToken(),session.expiresAt(),user(session.user()));
    }
    ApiResourceResponse resource(Map<String,Object> fields,String self){return new ApiResourceResponse(fields,self);}
    List<ApiResourceResponse> resources(List<Map<String,Object>> rows,String collection){
        return rows.stream().map(row->resource(row,collection+"/"+row.get("id"))).toList();
    }
}
