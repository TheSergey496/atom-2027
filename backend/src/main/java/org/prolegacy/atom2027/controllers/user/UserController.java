package org.prolegacy.atom2027.controllers.user;

import org.prolegacy.atom2027.common.annotation.TypescriptEndpoint;
import org.prolegacy.atom2027.common.auth.dto.UserDto;
import org.prolegacy.atom2027.services.user.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@CrossOrigin("*")
@RequestMapping("user")
public class UserController {
    @Autowired
    private UserService userService;

    @PostMapping
    public UserDto registerUser(@RequestBody UserDto userDto) {
        return userService.registerUser(userDto);
    }

    @PutMapping
    public UserDto updateUser(@RequestParam String password, @RequestBody UserDto userDto) {
        return userService.updateUser(password, userDto);
    }
}
