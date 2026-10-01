package edu.deploylab;

import edu.deploylab.dto.*;
import java.util.*;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/api/groups")
@org.springframework.boot.autoconfigure.condition.ConditionalOnProperty(name="app.extras-enabled",havingValue="true")
public class GroupController {
    private final GroupService groups; private final JobService jobs;
    public GroupController(GroupService groups,JobService jobs) {this.groups=groups;this.jobs=jobs;}
    @PostMapping @ResponseStatus(org.springframework.http.HttpStatus.CREATED) @PreAuthorize("hasRole('INSTRUCTOR')")
    public Object create(@AuthenticationPrincipal AuthService.Actor user,@Valid @RequestBody GroupCreateRequest body) {return Map.of("id",groups.create(user,body.name()));}
    @PatchMapping("/{id}") @ResponseStatus(org.springframework.http.HttpStatus.NO_CONTENT) @PreAuthorize("hasRole('INSTRUCTOR')")
    public void edit(@PathVariable UUID id,@AuthenticationPrincipal AuthService.Actor user,@Valid @RequestBody GroupEditRequest body) {groups.edit(id,user,new GroupService.EditGroup(body.name(),body.archived()));}
    @PostMapping("/{id}/members/by-email") @ResponseStatus(org.springframework.http.HttpStatus.NO_CONTENT) @PreAuthorize("hasRole('INSTRUCTOR')")
    public void email(@PathVariable UUID id,@AuthenticationPrincipal AuthService.Actor user,@Valid @RequestBody EmailMemberRequest body) {groups.addByEmail(id,user,body.email());}
    @GetMapping public Object list(@AuthenticationPrincipal AuthService.Actor user) {return groups.list(user);}
    @GetMapping("/{id}") public Object detail(@PathVariable UUID id,@AuthenticationPrincipal AuthService.Actor user) {return groups.detail(id,user);}
    @PostMapping("/{id}/members") @ResponseStatus(org.springframework.http.HttpStatus.NO_CONTENT) @PreAuthorize("hasRole('INSTRUCTOR')")
    public void add(@PathVariable UUID id,@AuthenticationPrincipal AuthService.Actor user,@Valid @RequestBody MemberRequest body) {groups.add(id,user,body.userId());}
    @DeleteMapping("/{id}/members/{member}") @ResponseStatus(org.springframework.http.HttpStatus.NO_CONTENT) @PreAuthorize("hasRole('INSTRUCTOR')")
    public void remove(@PathVariable UUID id,@PathVariable UUID member,@AuthenticationPrincipal AuthService.Actor user) {groups.remove(id,user,member);}
    @PostMapping("/{id}/assignments") @ResponseStatus(org.springframework.http.HttpStatus.CREATED) @PreAuthorize("hasRole('INSTRUCTOR')")
    public Object assign(@PathVariable UUID id,@AuthenticationPrincipal AuthService.Actor user,@Valid @RequestBody AssignmentCreateRequest body) {return Map.of("id",groups.assign(id,user,new GroupService.Assign(body.workshopId(),body.dueAt())));}
    @GetMapping("/{id}/statistics") @PreAuthorize("hasRole('INSTRUCTOR')") public Object stats(@PathVariable UUID id,@AuthenticationPrincipal AuthService.Actor user) {return groups.statistics(id,user);}
    @PostMapping("/{id}/reports") @ResponseStatus(org.springframework.http.HttpStatus.ACCEPTED) @PreAuthorize("hasRole('INSTRUCTOR')")
    public Object report(@PathVariable UUID id,@AuthenticationPrincipal AuthService.Actor user) {groups.owner(id,user);return Map.of("id",jobs.enqueue(user.id(),"GROUP_REPORT",id.toString()));}
}
