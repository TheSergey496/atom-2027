package org.prolegacy.atom2027.common.health;

import lombok.extern.log4j.Log4j2;
import org.jspecify.annotations.NonNull;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.health.contributor.Health;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

@Component
@Log4j2
public class StartupConfigChecker implements ApplicationRunner {
    private final AtomicReference<Health> startupHealth =
            new AtomicReference<>(
                    Health.down()
                            .withDetail("reason", "Startup validation not yet completed")
                            .build()
            );

    private final List<HealthValidator> validators;

    public StartupConfigChecker(List<HealthValidator> validators) {
        this.validators = validators;
    }

    @Override
    public void run(@NonNull ApplicationArguments args) {
        Health.Builder builder = Health.up();
        List<String> failed = new ArrayList<>();

        if (validators.isEmpty()) {
            log.warn("No HealthValidator beans found; startup validation has nothing to run");
            startupHealth.set(Health.up().withDetail("validators", "none").build());
            return;
        }

        for (HealthValidator validator : validators) {
            String name = validator.name();
            try {
                HealthValidator.ValidationResult result = validator.validate();
                if (result == null) {
                    failed.add(name);
                    builder.withDetail(name, "validator returned null");
                } else if (result.healthy()) {
                    builder.withDetail(name, result.message() == null ? "OK" : result.message());
                } else {
                    failed.add(name);
                    builder.withDetail(name, result.message() == null ? "failed" : result.message());
                }
            } catch (Exception e) {
                log.error("Health validator '{}' threw an exception", name, e);
                failed.add(name);
                builder.withDetail(name, "exception: " + e.getMessage());
            }
        }

        if (!failed.isEmpty()) {
            builder.down().withDetail("failedValidators", failed);
        }

        Health health = builder.build();
        startupHealth.set(health);
        log.info("Startup validation finished with status {}", health.getStatus());
    }

    public Health getStartupHealth() {
        return startupHealth.get();
    }
}