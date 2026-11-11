package org.prolegacy.atom2027;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing
public class Atom2027Application {

    public static void main(String[] args) {
        SpringApplication.run(Atom2027Application.class, args);
    }

}
