package org.prolegacy.atom2027.controllers.user;

import org.prolegacy.atom2027.common.annotation.TypescriptEndpoint;
import org.prolegacy.atom2027.common.auth.dto.RoleDto;
import org.prolegacy.atom2027.common.auth.dto.UserDto;
import org.prolegacy.atom2027.common.auth.entities.id.UserId;
import org.prolegacy.atom2027.common.auth.readers.RoleReader;
import org.prolegacy.atom2027.common.auth.readers.UserReader;
import org.prolegacy.atom2027.services.user.UserAdminService;
import org.prolegacy.atom2027.common.query.lazy.PageQuery;
import org.prolegacy.atom2027.common.query.lazy.PageResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@PreAuthorize("hasRole('admin')")
@CrossOrigin("*")
@RestController
@RequestMapping("administration/users")
@TypescriptEndpoint
public class UserAdminController {
    @Autowired
    private UserReader userReader;
    @Autowired
    private UserAdminService userAdminService;
    @Autowired
    private RoleReader roleReader;

    @PostMapping
    public UserDto createUser(@RequestBody UserDto userDto) {
        return userAdminService.createUser(userDto);
    }

    @PutMapping("{userId}")
    public UserDto updateUser(@PathVariable UserId userId,
                              @RequestBody UserDto userDto) {
        return userAdminService.updateUser(userId, userDto);
    }

    @PatchMapping("{userId}")
    public UserDto resetPassword(@PathVariable UserId userId) {
        return userAdminService.resetPassword(userId);
    }

    @DeleteMapping("{userId}/archive")
    public UserDto archiveUser(@PathVariable UserId userId) {
        return userAdminService.archiveUser(userId);
    }

    @PutMapping("{userId}/archive")
    public UserDto unarchiveUser(@PathVariable UserId userId) {
        return userAdminService.unarchiveUser(userId);
    }

    @GetMapping("{userId}")
    public UserDto getUser(@PathVariable UserId userId,
                           @RequestParam(required = false, defaultValue = "false") Boolean joinRoles) {
        return userReader.getUser(userId, joinRoles);
    }

    @GetMapping
    public List<UserDto> getUsers(@RequestParam(required = false, defaultValue = "false") Boolean joinRoles) {
        return userReader.getUsers(joinRoles);
    }

    @PostMapping("search")
    public PageResponse<UserDto> searchUsers(@RequestBody PageQuery pageQuery) {
        return userReader.searchUsers(pageQuery);
    }

    @GetMapping("/roles")
    public List<RoleDto> getRoles() {
        return roleReader.getRoles();
    }
}
