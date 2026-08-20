package org.prolegacy.atom2027.common.auth.entities;

import com.google.common.base.Strings;
import com.querydsl.core.annotations.PropertyType;
import com.querydsl.core.annotations.QueryType;
import jakarta.persistence.*;
import lombok.*;
import org.prolegacy.atom2027.common.annotation.LongIdGenerator;
import org.prolegacy.atom2027.common.auth.dto.UserDto;
import org.prolegacy.atom2027.common.auth.entities.id.UserId;
import org.prolegacy.atom2027.common.exceptions.BusinessLogicException;

import java.time.Instant;
import java.util.*;
import java.util.regex.Pattern;

@Entity
@Getter
@Setter(AccessLevel.PRIVATE)
@EqualsAndHashCode
@NoArgsConstructor
@Table(name = "\"user\"")
public class User {
    @Id
    @LongIdGenerator
    private UserId id;

    @Column(unique = true, nullable = false)
    private String email;
    @Setter(AccessLevel.PUBLIC)
    @Column(nullable = false)
    @QueryType(PropertyType.NONE)
    private String password;

    @Column(nullable = false)
    private String firstname;
    @Column(nullable = false)
    private String lastname;
    private String surname;
    private String phoneNumber;
    private Instant registrationDate;
    @Column(nullable = false, columnDefinition = "boolean default false")
    private Boolean archived = false;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "UserRole",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    private Set<Role> roles = new HashSet<>();

    // Для дебага
    @Deprecated(forRemoval = true)
    public User(String email, String password, String firstname, String lastname, String surname, String phoneNumber, Collection<Role> roles) {
        this.email = email;
        this.password = password;
        this.firstname = firstname;
        this.lastname = lastname;
        this.surname = surname;
        this.phoneNumber = phoneNumber;
        this.registrationDate = Instant.now();
        this.roles.addAll(roles);
    }

    public User(@NonNull UserDto userDto,
                @NonNull String encodedPassword,
                @NonNull List<Role> roles) {
        this.registrationDate = Instant.now();
        this.email = formatAndValidateEmail(userDto.getEmail());
        this.roles.addAll(roles);
        this.update(userDto, encodedPassword);
    }

    public void adminUpdate(@NonNull UserDto userDto,
                            @NonNull List<Role> roles) {
        this.roles = new HashSet<>(roles);
        this.update(userDto, null);
    }

    public void update(@NonNull UserDto userDto, String newEncodedPassword) {
        if (Strings.isNullOrEmpty(userDto.getFirstname())) {
            throw new BusinessLogicException("Отсутствует имя");
        }
        this.firstname = userDto.getFirstname();
        if (Strings.isNullOrEmpty(userDto.getLastname())) {
            throw new BusinessLogicException("Отсутствует фамилия");
        }
        this.lastname = userDto.getLastname();
        this.surname = userDto.getSurname();
        this.phoneNumber = userDto.getPhoneNumber();
        Optional.ofNullable(newEncodedPassword).ifPresent(this::setPassword);
    }

    private String formatAndValidateEmail(String email) {
        return Optional.ofNullable(email)
                .map(e -> {
                            boolean matches = Pattern.compile("^[\\w-.]+@([\\w-]+\\.)+[\\w-]{2,4}$")
                                    .matcher(e)
                                    .matches();
                            if (!matches) {
                                throw new BusinessLogicException("Некорректный email");
                            }
                            return e.toLowerCase(Locale.US);
                        }
                ).orElseThrow(() -> new BusinessLogicException("Отсутствует email-адрес"));
    }

    public void archive() {
        this.archived = true;
    }

    public void unarchive() {
        this.archived = false;
    }

    public String getShortName() {
        String shortName = null;
        if (lastname != null) {
            shortName = lastname;
        }
        if (firstname != null) {
            shortName = shortName != null ? firstname + " %s.".formatted(firstname.substring(0, 1))
                    : firstname;
        }
        if (!Strings.isNullOrEmpty(surname)) {
            shortName = shortName != null ? shortName + " %s.".formatted(surname.substring(0, 1))
                    : surname;
        }
        return shortName;
    }
}
