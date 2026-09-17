package edu.deploylab;

import java.util.*;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/api/groups")
public class GroupController {
    private final GroupService groups; private final JobService jobs;
    public GroupController(GroupService groups,JobService jobs) {this.groups=groups;this.jobs=jobs;}
    @PostMapping @ResponseStatus(org.springframework.http.HttpStatus.CREATED)
    public Object create(@AuthenticationPrincipal AuthService.Actor user,@Valid @RequestBody GroupService.NewGroup body) {return Map.of("id",groups.create(user,body.name()));}
    @GetMapping public Object list(@AuthenticationPrincipal AuthService.Actor user) {return groups.list(user);}
    @GetMapping("/{id}") public Object detail(@PathVariable UUID id,@AuthenticationPrincipal AuthService.Actor user) {return groups.detail(id,user);}
    @PostMapping("/{id}/members") @ResponseStatus(org.springframework.http.HttpStatus.NO_CONTENT)
    public void add(@PathVariable UUID id,@AuthenticationPrincipal AuthService.Actor user,@Valid @RequestBody GroupService.Member body) {groups.add(id,user,body.userId());}
    @DeleteMapping("/{id}/members/{member}") @ResponseStatus(org.springframework.http.HttpStatus.NO_CONTENT)
    public void remove(@PathVariable UUID id,@PathVariable UUID member,@AuthenticationPrincipal AuthService.Actor user) {groups.remove(id,user,member);}
    @PostMapping("/{id}/assignments") @ResponseStatus(org.springframework.http.HttpStatus.CREATED)
    public Object assign(@PathVariable UUID id,@AuthenticationPrincipal AuthService.Actor user,@Valid @RequestBody GroupService.Assign body) {return Map.of("id",groups.assign(id,user,body));}
    @GetMapping("/{id}/statistics") public Object stats(@PathVariable UUID id,@AuthenticationPrincipal AuthService.Actor user) {return groups.statistics(id,user);}
    @PostMapping("/{id}/reports") @ResponseStatus(org.springframework.http.HttpStatus.ACCEPTED)
    public Object report(@PathVariable UUID id,@AuthenticationPrincipal AuthService.Actor user) {groups.owner(id,user);return Map.of("id",jobs.enqueue(user.id(),"GROUP_REPORT",id.toString()));}
}
