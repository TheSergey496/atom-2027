package org.prolegacy.atom2027.common.auth.services;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.prolegacy.atom2027.common.auth.dto.UserDto;
import org.prolegacy.atom2027.common.auth.readers.UserReader;
import org.prolegacy.atom2027.common.auth.util.JwtTokenUtil;
import org.prolegacy.atom2027.common.exceptions.BusinessLogicException;
import org.prolegacy.atom2027.services.user.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class AuthService {
    @Autowired
    private UserReader userReader;

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private JwtTokenUtil jwtTokenUtil;

    @Value("${auth.password-restoration-code-expiration-minutes:15}")
    private Long passRestorationExpirationMinutes = 15L;

    private final Cache<String, Long> passwordRestorationCodes = Caffeine.newBuilder()
            .expireAfterWrite(passRestorationExpirationMinutes, TimeUnit.MINUTES)
            .build();

    public UserDto authenticate(String email, String password) throws BadCredentialsException {
        Authentication authentication = authenticationManager
                .authenticate(
                        new UsernamePasswordAuthenticationToken(
                                email, password
                        )
                );
        return userReader.getUserByEmail(email, true);
    }

    public void logout(String authorization) {
        Pattern pattern = Pattern.compile("Bearer (.*)$");
        Matcher matcher = pattern.matcher(authorization);

        if (!matcher.find()) {
            throw new BusinessLogicException("Нет токена");
        }

        String token = matcher.group(1);
        jwtTokenUtil.getBlacklistedTokens().put(token, Boolean.TRUE);
    }

    public UserDto getCurrentUserDto() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication.getPrincipal() instanceof org.springframework.security.core.userdetails.User user) {
            return userReader.getUserByEmail(user.getUsername(), true);
        } else {
            return null;
        }
    }
}
