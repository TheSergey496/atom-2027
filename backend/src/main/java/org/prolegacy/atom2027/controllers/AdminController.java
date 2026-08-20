package org.prolegacy.atom2027.controllers;

import org.prolegacy.atom2027.common.annotation.TypescriptEndpoint;
import org.prolegacy.atom2027.common.auth.dto.UserDto;
import org.prolegacy.atom2027.common.auth.readers.UserReader;
import org.prolegacy.atom2027.common.auth.services.UserService;
import org.prolegacy.atom2027.enums.Role;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("admin")
@PreAuthorize("hasRole('admin')")
@TypescriptEndpoint
public class AdminController {
    @Autowired
    private UserReader userReader;
    @Autowired
    private UserService userService;

    @GetMapping("users")
    public List<UserDto> getUsers(@RequestParam Boolean joinRoles) {
        return userReader.getUsers(Optional.ofNullable(joinRoles).orElse(false));
    }

    @GetMapping
    public void test(Role role) {

    }
}
