package com.sweng.notes;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Entry point dell’applicazione Spring Boot.
 * - Avvia il backend su http://localhost:8080
 * - Espone il bean PasswordEncoder per la cifratura delle password.
 */

@SpringBootApplication
public class App {
    public static void main(String[] args) {
        SpringApplication.run(App.class, args);
        System.out.println("Backend Spring Boot avviato su http://localhost:8080");
    }

    /**
     * BCrypt viene usato da UserService per generare e verificare
     * gli hash delle password. Registrarlo come bean permette
     * di iniettarlo automaticamente nei service (@Autowired).
     */

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
