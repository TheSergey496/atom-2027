package org.prolegacy.atom2027.common.auth.config;

import lombok.Getter;
import org.jspecify.annotations.Nullable;
import org.prolegacy.atom2027.common.auth.entities.id.UserId;
import org.springframework.security.core.GrantedAuthority;

import java.util.Collection;

public class UserDetails extends org.springframework.security.core.userdetails.User {
    @Getter
    private final UserId id;

    public UserDetails(UserId id, String username, @Nullable String password, Collection<? extends GrantedAuthority> authorities) {
        super(username, password, authorities);
        this.id = id;
    }
}
