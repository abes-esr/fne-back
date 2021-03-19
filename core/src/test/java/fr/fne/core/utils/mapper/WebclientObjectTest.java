package fr.fne.core.utils.mapper;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import fr.fne.core.config.BeanConfigurationCore;
import fr.fne.core.config.WikibaseProperties;
import fr.fne.core.entities.Autorite;
import fr.fne.core.entities.resApiWikibase.JsonObjectWikibase;
import fr.fne.core.entities.resApiWikibase.MainsnakWikibase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;

@SpringBootTest
@ContextConfiguration(classes = BeanConfigurationCore.class)
public class WebclientObjectTest {

    @Value("${wikibase.urls.property-detail}")
    String uriProperty;
    @Value("${wikibase.urls.property-value}")
    String uriPropertyValue;

    @Autowired
    WikibaseProperties wikibaseProperties;
    @Autowired
    WebClient.Builder webClientBuilder;


    @Test
    void getListMainSnakFromWiki() throws InterruptedException {

        CountDownLatch countDownLatch = new CountDownLatch(1);

        String url = this.uriProperty + "Q8";
        WebClient webClient = webClientBuilder.baseUrl(url).build();

        webClient.get().accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .bodyToFlux(JsonObjectWikibase.class)
                .subscribe(System.out::println, null, countDownLatch::countDown);

        countDownLatch.await();
    }

    @Test
    void getPropertiesList() throws InterruptedException {

        CountDownLatch countDownLatch = new CountDownLatch(1);
        List<String> properties = new ArrayList<>();
        ObjectMapper objectMapper = new ObjectMapper();
        String url = this.uriProperty + "Q8";
        WebClient webClient = webClientBuilder.baseUrl(url).build();

        webClient.get().retrieve()
                .bodyToMono(JsonNode.class)
                .map(v -> v.findValue("claims"))
                .map(v -> {
                    for (JsonNode j : v) {
                        properties.add((j.findValue("property").asText()));
                    }
                    return properties;
                })
                .flatMapMany(Flux::fromIterable)
                .subscribe(System.out::println, null, countDownLatch::countDown);

        countDownLatch.await();
    }

    @Test
    void getValueTypeString() throws InterruptedException {

        String item = "Q8";
        CountDownLatch countDownLatch = new CountDownLatch(1);
        List<String> properties = new ArrayList<>();
        Autorite autorite;
        String url = this.uriProperty + item;
        WebClient webClient = webClientBuilder.baseUrl(url).build();

        webClient.get().retrieve()
                .bodyToMono(JsonNode.class)
                .map(v -> v.findValue("claims"))
                .map(v -> {
                    for (JsonNode j : v) {
                        properties.add((j.findValue("property").asText()));
                    }
                    return properties;
                })
                .flatMapMany(Flux::fromIterable)
                .map(v -> getPropertyValue(item, v))
                .flatMap(v -> v.map(s -> {
                    return s;
                }))
                .filter(v -> v.getDatatype().equals("string"))
                .subscribe(System.out::println, null, countDownLatch::countDown);

        countDownLatch.await();

    }

    @Test
    void getValueTypeTime() throws InterruptedException {

        String item = "Q8";
        CountDownLatch countDownLatch = new CountDownLatch(1);
        List<String> properties = new ArrayList<>();
        Autorite autorite;
        String url = this.uriProperty + item;
        WebClient webClient = webClientBuilder.baseUrl(url).build();

        webClient.get().retrieve()
                .bodyToMono(JsonNode.class)
                .map(v -> v.findValue("claims"))
                .map(v -> {
                    for (JsonNode j : v) {
                        properties.add((j.findValue("property").asText()));
                    }
                    return properties;
                })
                .flatMapMany(Flux::fromIterable)
                .map(v -> getPropertyValue(item, v))
                .flatMap(v -> v.map(s -> {
                    return s;
                }))
                .filter(v -> v.getDatatype().equals("time"))
                .subscribe(System.out::println, null, countDownLatch::countDown);

        countDownLatch.await();

    }

    @Test
    void getPropertyValueTest() {

        String url = this.uriProperty + "Q8" + "&property=" + "P16";
        ObjectMapper mapper = new ObjectMapper();

        WebClient webClient = webClientBuilder.baseUrl(url).build();

        System.out.println(
                webClient.get().retrieve()
                        .bodyToMono(JsonNode.class)
                        .map(v -> v.findValue("mainsnak"))
                        .map(s -> {
                            try {
                                return mapper.readValue(s.traverse(), new TypeReference<MainsnakWikibase>() {
                                });
                            } catch (IOException e) {
                                e.printStackTrace();
                                return new MainsnakWikibase();
                            }
                        })
                        .block()
        );

    }

    @Test
    void mainTest() throws InterruptedException {

        CountDownLatch countDownLatch = new CountDownLatch(1);

        propertiesMapper("Q8").subscribe(System.out::println, null, countDownLatch::countDown);

        countDownLatch.await();
    }


    private Flux<MainsnakWikibase> propertiesMapper(String item) {

        List<String> properties = new ArrayList<>();
        Autorite autorite;
        String url = this.uriProperty + item;
        WebClient webClient = webClientBuilder.baseUrl(url).build();

        return webClient.get().retrieve()
                .bodyToMono(JsonNode.class)
                .map(v -> v.findValue("claims"))
                .map(v -> {
                    for (JsonNode j : v) {
                        properties.add((j.findValue("property").asText()));
                    }
                    return properties;
                })
                .flatMapMany(Flux::fromIterable)
                .flatMap(v -> getPropertyValue(item, v))
                .expand(v -> {
                    if (v.getDatatype().equals("wikibase-item")) {
                        return propertiesMapper(v.getDatavalueWikibase().getId());
                    }
                    return Flux.empty();
                })
                .filter(v -> !v.getDatatype().equals("wikibase-item"))
                .flatMap(v -> getPropertyName(v.getProperty(), v));
        //.flatMap(v -> Flux.fromIterable(Collections.singletonList(v)).sort(Comparator.comparing(MainsnakWikibase::getZoneNumber)) );


    }

    private Flux<MainsnakWikibase> propertiesMapperWithItem(String item) {

        List<String> properties = new ArrayList<>();
        String url = this.uriProperty + item;
        WebClient webClient = webClientBuilder.baseUrl(url).build();

        return webClient.get().retrieve()
                .bodyToMono(JsonNode.class)
                .map(v -> v.findValue("claims"))
                .map(v -> {
                    for (JsonNode j : v) {
                        properties.add((j.findValue("property").asText()));
                    }
                    return properties;
                })
                .flatMapMany(Flux::fromIterable)
                .map(v -> getPropertyValue(item, v))
                .flatMap(v -> v.map(s -> s));
    }


    private Mono<MainsnakWikibase> getPropertyValue(String item, String property) {

        String url = this.uriProperty + item + "&property=" + property;
        ObjectMapper mapper = new ObjectMapper();
        WebClient webClient = webClientBuilder.baseUrl(url).build();

        return webClient.get().retrieve()
                .bodyToMono(JsonNode.class)
                .map(v -> v.findValue("mainsnak"))
                .map(s -> {
                    try {
                        return mapper.readValue(s.traverse(), new TypeReference<MainsnakWikibase>() {
                        });
                    } catch (IOException e) {
                        e.printStackTrace();
                        return new MainsnakWikibase();
                    }
                });

    }

    private Mono<MainsnakWikibase> getPropertyName(String propertyValue, MainsnakWikibase mainsnakWikibase) {

        String url = this.uriPropertyValue + propertyValue;
        WebClient webClient = webClientBuilder.baseUrl(url).build();

        return webClient.get().retrieve()
                .bodyToMono(JsonNode.class)
                .map(v -> v.findValue("fr"))
                .map(s -> {
                    String pName = s.findValue("value").asText();
                    mainsnakWikibase.setPropertyName(pName);
                    if (wikibaseProperties.getZones().containsKey(pName)) {
                        String pTag = wikibaseProperties.getZones().get(pName);
                        mainsnakWikibase.setZoneNumber(pTag);
                    }
                    return mainsnakWikibase;
                });
    }

}
