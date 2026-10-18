package org.prolegacy.atom2027.common.health;

import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.stereotype.Component;

@Component
public class StartupConfigHealthIndicator implements HealthIndicator {
    private final StartupConfigChecker checker;

    public StartupConfigHealthIndicator(StartupConfigChecker checker) {
        this.checker = checker;
    }

    @Override
    public Health health() {
        return checker.getStartupHealth();
    }
}
