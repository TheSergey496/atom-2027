package org.prolegacy.atom2027.common.audit;

import org.jspecify.annotations.NonNull;
import org.prolegacy.atom2027.common.auth.entities.User;
import org.prolegacy.atom2027.common.auth.entities.id.UserId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.AuditorAware;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class SpringSecurityAuditorAware implements AuditorAware<UserId> {
    @Autowired
    private Optional<User> user;

    @Override
    @NonNull
    public Optional<UserId> getCurrentAuditor() {
        return user.map(User::getId);
    }
}
