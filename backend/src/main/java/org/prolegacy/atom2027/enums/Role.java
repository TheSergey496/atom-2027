package org.prolegacy.atom2027.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.prolegacy.atom2027.common.auth.interfaces.RoleInfo;
import org.prolegacy.atom2027.common.intefaces.LocaleEnum;

@RequiredArgsConstructor
@Getter
public enum Role implements RoleInfo, LocaleEnum {
    ADMIN("admin", "Администратор"),
    USER("user", "Пользователь");

    private final String roleName;
    private final String localization;

    @Override
    public String locale() {
        return this.localization;
    }
}
