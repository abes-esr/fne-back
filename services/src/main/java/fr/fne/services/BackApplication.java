package fr.fne.services;

import fr.fne.services.oauth.OauthService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;


@SpringBootApplication(scanBasePackages = {"fr.fne.core", "fr.fne.services"})
@Slf4j
@EnableScheduling
@EnableAsync
public class BackApplication {

    @Autowired
    private OauthService oauthService;

    public static void main(String[] args) {

        SpringApplication.run(BackApplication.class, args);

    }
/*
	@Override
	public void run(String... args) throws Exception {
		oauthService.go2();
	}
*/

}
