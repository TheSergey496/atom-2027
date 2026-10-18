package org.prolegacy.atom2027.common.health;

public interface HealthValidator {
    String name();

    ValidationResult validate() throws Exception;

    record ValidationResult(boolean healthy, String message) {

        public static ValidationResult ok() {
            return new ValidationResult(true, null);
        }

        public static ValidationResult ok(String message) {
            return new ValidationResult(true, message);
        }

        public static ValidationResult fail(String message) {
            return new ValidationResult(false, message);
        }
    }
}
