package org.prolegacy.atom2027.common.auth.controllers;

import org.prolegacy.atom2027.common.auth.dto.UserDto;
import org.prolegacy.atom2027.common.auth.services.AuthService;
import org.prolegacy.atom2027.services.user.UserService;
import org.prolegacy.atom2027.common.auth.util.JwtTokenUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.annotation.*;

import java.time.format.DateTimeFormatter;
import java.util.Locale;

@RestController
@CrossOrigin("*")
@RequestMapping("/authentication")
public class AuthenticationController {
    @Autowired
    private AuthService authService;

    @Autowired
    private JwtTokenUtil jwtTokenUtil;

    @PostMapping
    public ResponseEntity<UserDto> authenticate(@RequestBody AuthRequest authRequest) {
        try {
            UserDto userDto = authService.authenticate(authRequest.email().toLowerCase(Locale.US), authRequest.password());

            return ResponseEntity.ok()
                    .header(
                            HttpHeaders.AUTHORIZATION,
                            jwtTokenUtil.generateAccessToken(userDto.getEmail())
                    )
                    .header(
                            HttpHeaders.EXPIRES,
                            DateTimeFormatter.ISO_INSTANT.format(jwtTokenUtil.getExpirationTimeFromNow())
                    )
                    .body(
                            userDto
                    );
        } catch (BadCredentialsException ex) {
            return ResponseEntity.ok(null);
        }
    }

    @GetMapping
    public UserDto getCurrentUser() {
        return authService.getCurrentUserDto();
    }

    @DeleteMapping
    public void logout(@RequestHeader(HttpHeaders.AUTHORIZATION) String authorization) {
        authService.logout(authorization);
    }

    public record AuthRequest(String email, String password) {
    }
}
