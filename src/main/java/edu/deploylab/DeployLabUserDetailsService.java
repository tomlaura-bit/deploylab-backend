package edu.deploylab;

import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service;

@Service
public class DeployLabUserDetailsService implements UserDetailsService {
    private final Db db;
    public DeployLabUserDetailsService(Db db){this.db=db;}
    @Override public UserDetails loadUserByUsername(String email) {
        var rows=db.query("SELECT email,password_hash,role FROM app_user WHERE email=?",email.trim().toLowerCase(java.util.Locale.ROOT));
        if(rows.isEmpty()) throw new UsernameNotFoundException("Usuario no encontrado");
        var row=rows.getFirst();
        return User.withUsername(row.get("email").toString()).password(row.get("password_hash").toString())
            .roles(row.get("role").toString()).build();
    }
}
