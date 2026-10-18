package org.prolegacy.atom2027.health;

import org.prolegacy.atom2027.common.health.HealthValidator;
import org.springframework.stereotype.Component;

@Component
public class DummyHealthValidator implements HealthValidator {
    @Override
    public String name() {
        return "Dummy Validator";
    }

    @Override
    public ValidationResult validate() {
        return ValidationResult.ok();
    }
}
