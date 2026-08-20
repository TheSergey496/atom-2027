package org.prolegacy.atom2027.common.auth.dto;

import org.prolegacy.atom2027.common.auth.entities.id.UserId;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Set;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder(toBuilder = true)
public class UserDto {
    private UserId id;
    private String email;
    private String password;
    private String firstname;
    private String lastname;
    private String surname;
    private String phoneNumber;
    private Instant registrationDate;
    private Boolean archived;

    private String fullName;
    private String shortName;
    private String rolesAsString;

    private Set<RoleDto> roles;
}
