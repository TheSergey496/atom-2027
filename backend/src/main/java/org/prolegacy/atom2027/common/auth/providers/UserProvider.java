package org.prolegacy.atom2027.common.auth.providers;

import jakarta.inject.Provider;
import org.prolegacy.atom2027.common.auth.config.UserDetails;
import org.prolegacy.atom2027.common.auth.exceptions.UserNotFoundException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Optional;


@Component
public class UserProvider implements Provider<UserDetails> {
    @Override
    public UserDetails get() {
        final var authentication = SecurityContextHolder.getContext().getAuthentication();
        final var principal = Optional.ofNullable(authentication).map(a -> (UserDetails) a.getPrincipal());
        return principal.orElseThrow(UserNotFoundException::new);
    }
}
