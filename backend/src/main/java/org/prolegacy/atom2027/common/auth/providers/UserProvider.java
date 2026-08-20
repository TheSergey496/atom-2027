package org.prolegacy.atom2027.common.auth.providers;

import jakarta.inject.Provider;
import org.prolegacy.atom2027.common.auth.entities.User;
import org.prolegacy.atom2027.common.auth.exceptions.UserNotFoundException;
import org.prolegacy.atom2027.common.auth.repositories.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Optional;


@Component
public class UserProvider implements Provider<User> {
    @Autowired
    private UserRepository userRepository;

    @Override
    public User get() {
        final var authentication = SecurityContextHolder.getContext().getAuthentication();
        final var principal = Optional.ofNullable(authentication).map(a -> (org.springframework.security.core.userdetails.User) a.getPrincipal());
        return principal.flatMap(user -> userRepository.findActualUserByEmail(user.getUsername()))
                .orElseThrow(UserNotFoundException::new);
    }
}
