package org.example.web;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

// Starts the web server (http://localhost:8080). The console version is still org.example.Main.
// Spring only scans org.example.web; the engine, the league and the DAOs are plain Java classes
// that AppConfig turns into Spring beans.
@SpringBootApplication
public class WebApplication {

    public static void main(String[] args) {
        SpringApplication.run(WebApplication.class, args);
    }
}
