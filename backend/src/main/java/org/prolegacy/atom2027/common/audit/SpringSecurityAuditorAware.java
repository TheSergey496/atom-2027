package org.prolegacy.atom2027.common.audit;

import org.jspecify.annotations.NonNull;
import org.prolegacy.atom2027.common.auth.entities.id.UserId;
import org.prolegacy.atom2027.common.auth.exceptions.UserNotFoundException;
import org.prolegacy.atom2027.common.auth.providers.UserProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.AuditorAware;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class SpringSecurityAuditorAware implements AuditorAware<UserId> {
    @Autowired
    private UserProvider userProvider;

    @Override
    @NonNull
    public Optional<UserId> getCurrentAuditor() {
        try {
            return Optional.of(userProvider.get().getId());
        } catch (UserNotFoundException e) {
            return Optional.empty();
        }
    }
}
