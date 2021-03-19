package fr.fne.services;

import com.fasterxml.jackson.databind.JsonNode;
import fr.fne.core.config.WikibaseProperties;
import fr.fne.services.domain.entities.WikibaseLangues;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.DefaultUriBuilderFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;

@RunWith(SpringRunner.class)
@SpringBootTest
class WikibasePropertiesTest {

    @Autowired
    private WikibaseProperties wikibaseProperties;
    @Autowired
    WebClient.Builder webClientBuilder;

    @Test
    public void whenYamlFileProvided() {
        System.out.println(wikibaseProperties.getZones());
    }

    @Test
    public void findAllLangues() throws InterruptedException {

        CountDownLatch countDownLatch = new CountDownLatch(1);
        String urlsWikibaseLangues = "http://fne-query-test.abes.fr";
        String queryLangues = "SELECT ?item ?itemLabel\n" +
                "WHERE \n" +
                "{\n" +
                "  ?item wdt:P1 wd:Q6.\n" +
                "  ?item wdt:P13 ?b.\n" +
                "  SERVICE wikibase:label { bd:serviceParam wikibase:language \"[AUTO_LANGUAGE],fr\". }\n" +
                "}";

        WebClient webClient = webClientBuilder.baseUrl(urlsWikibaseLangues).build();

        List<WikibaseLangues> wikibaseLanguesList = new ArrayList<>();
        webClient.get()
            .uri(builder -> builder
                .path("/proxy/wdqs/bigdata/namespace/wdq/sparql")
                .queryParam("query", "{queryLangues}")
                .queryParam("format", "json")
                .build(queryLangues)
            )
            .retrieve()
            .bodyToMono(JsonNode.class)
            .flatMap(v -> {
                for (JsonNode j: v.findValue("bindings")) {
                    WikibaseLangues wikibaseLangues = new WikibaseLangues();
                    wikibaseLangues.setLangueName(j.findValue("itemLabel").findValue("value").asText());
                    wikibaseLanguesList.add(wikibaseLangues);
                }
                return Mono.just(wikibaseLanguesList);
            })
            .flatMapMany(Flux::fromIterable)
            .subscribe(System.out::println, System.out::println, countDownLatch::countDown);
        countDownLatch.await();

    }

}