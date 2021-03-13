package fr.fne.services;

import fr.fne.core.config.WikibaseProperties;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest
class WikibasePropertiesTest {

    @Autowired
    private WikibaseProperties wikibaseProperties;

    @Test
    public void whenYamlFileProvided() {
        System.out.println(wikibaseProperties.getZones());
    }

}