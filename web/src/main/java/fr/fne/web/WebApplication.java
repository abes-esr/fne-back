package fr.fne.web;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = {
        "fr.fne.core",
        "fr.fne.web",
        "fr.fne.services.domain",
        "fr.fne.services.event",
})
public class WebApplication {

    public static void main(String[] args) {
        SpringApplication.run(WebApplication.class, args);
    }

}
