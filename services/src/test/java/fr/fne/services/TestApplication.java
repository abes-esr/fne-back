package fr.fne.services;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class TestApplication {

    @Test
    void contextLoads() {
    }

    @Test
    void indexOfX() {
        String x = "19xx";
        System.out.println(x.indexOf("x"));
    }

}