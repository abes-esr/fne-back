package fr.fne.core.utils.mapper;

import com.fasterxml.jackson.databind.JsonNode;
import fr.fne.core.config.BeanConfigurationCore;
import fr.fne.core.config.WikibaseProperties;
import fr.fne.core.entities.Autorite;
import fr.fne.core.entities.Zone;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

@SpringBootTest
@ContextConfiguration(classes = BeanConfigurationCore.class)
class ResponseToAutoriteTest {

    @Value("${wikibase.urls.property-detail}")
    String uriProperty;
    @Value("${wikibase.urls.property-value}")
    String uriPropertyValue;
    Map<Mono<String>, Mono<String>> mapper = new LinkedHashMap<>();
    Autorite autorite;


    @Test
    void wikibaseMapProperties() {

        Flux.just(wikibaseProperties.getZones()).subscribe(System.out::println);

    }

    @Autowired
    WikibaseProperties wikibaseProperties;
    @Autowired
    WebClient.Builder webClientBuilder;

    @Test
    public void toObjectReactive() throws InterruptedException {
        CountDownLatch countDownLatch = new CountDownLatch(1);

        getMapProperties("Q8")
                .log()
                .subscribe(null, null, countDownLatch::countDown);

        countDownLatch.await();

        mapper.forEach((k, v) -> System.out.println(k.block() + " " + v.block()));
    }

    @Test
    public void toObjectReactive2() throws InterruptedException {

        CountDownLatch countDownLatch = new CountDownLatch(1);

        getMapProperties2("Q8")
                .log()
                .subscribe(null, null, countDownLatch::countDown);

        countDownLatch.await();

        mapper.forEach((k, v) -> System.out.println(k.block() + " " + v.block()));
    }

    private Mono<Map<Mono<String>, Mono<String>>> getMapProperties(String itemName) {

        String url = this.uriProperty + itemName;
        WebClient webClient = webClientBuilder.baseUrl(url).build();
        return webClient.get().accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .bodyToMono(JsonNode.class)
                .map(v -> v.findValue("claims"))
                .flatMap(v -> {
                    for (JsonNode j : v) {
                        if (j.findValue("datatype").asText().equals("string")) {
                            String property = j.findValue("property").asText();
                            Mono<String> propertyName = getPropertyValueFromWikiMap(property);
                            Mono<String> propertyValue = Mono.just(j.findValue("value").asText());
                            mapper.put(propertyName, propertyValue);
                        } else if (j.findValue("datatype").asText().equals("wikibase-item") && !j.findValue("property").asText().equals("P1")) {
                            String property = j.findValue("property").asText();
                            Mono<String> propertyName = getPropertyValueFromWikiMap(property);
                            Mono<String> propertyValue = Mono.just(wikibaseProperties.getZones().get(j.findValue("id").asText()));
                            mapper.put(propertyName, propertyValue);
                        }
                    }
                    return Mono.just(mapper);
                });
    }


    private Flux<Map<Mono<String>, Mono<String>>> getMapProperties2(String itemName) {

        String url = this.uriProperty + itemName;
        List<Zone> zones = new ArrayList<>();
        WebClient webClient = webClientBuilder.baseUrl(url).build();
        return webClient.get().accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .bodyToMono(JsonNode.class)
                .map(v -> v.findValue("claims"))
                .flatMap(v -> {

                    for (JsonNode j : v) {

                        if (!j.findValue("datatype").asText().equals("wikibase-item")) {
                            String property = j.findValue("property").asText();
                            Mono<String> propertyName = getPropertyValueFromWikiMap(property);
                            Mono<String> propertyValue = Mono.just(j.findValue("value").asText());
                            ;
                            if (j.findValue("datatype").asText().equals("time")) {
                                propertyValue = Mono.just(StringUtils.substringBetween(j.findValue("time").asText(), "+", "T").replaceAll("-", ""));
                            }

                            mapper.put(propertyName, propertyValue);
                        }
                    }
                    return Mono.just(mapper);
                }).concatWith(getMapPropertiesNested(itemName));
    }

    private Mono<Map<Mono<String>, Mono<String>>> getMapPropertiesNested(String itemName) {

        String url = this.uriProperty + itemName;
        WebClient webClient = webClientBuilder.baseUrl(url).build();
        return webClient.get().accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .bodyToMono(JsonNode.class)
                .map(v -> v.findValue("claims"))
                .flatMap(v -> {
                    for (JsonNode j : v) {
                        if (j.findValue("datatype").asText().equals("wikibase-item") && !j.findValue("property").asText().equals("P1")) {
                            String property = j.findValue("property").asText();
                            Mono<String> propertyName = getPropertyValueFromWikiMap(property);
                            Mono<String> propertyValue = Mono.just(wikibaseProperties.getZones().get(j.findValue("id").asText()));
                            mapper.put(propertyName, propertyValue);
                        }
                    }
                    return Mono.just(mapper);
                });
    }


    private Mono<String> getPropertyValueFromWikiMap(String propertyName) {
        String url = this.uriPropertyValue + propertyName;
        WebClient webClient = webClientBuilder.baseUrl(url).build();

        return webClient.get().retrieve()
                .bodyToMono(JsonNode.class)
                .map(v -> v.findValue("value").asText())
                .map(v -> {
                    String propertyValue = null;
                    if (wikibaseProperties.getZones().containsKey(v)) {
                        propertyValue = wikibaseProperties.getZones().get(v);
                    }
                    return propertyValue;
                });
    }

//    private Mono<JsonNode> doRequestWebclient(String itemName) {
//        String url = this.uriProperty + itemName;
//        WebClient webClient = webClientBuilder.baseUrl(url).build();
//         return webClient.get().accept(MediaType.APPLICATION_JSON)
//                 .retrieve()
//                 .bodyToMono(JsonNode.class)
//                 .map(v -> v.findValue("claims"));
//    }

    @Test
    void getPropertiesValueTest() {
        String url = this.uriPropertyValue + "p5";
        WebClient webClient = webClientBuilder.baseUrl(url).build();

        System.out.println(webClient.get().retrieve()
                .bodyToMono(JsonNode.class)
                .map(v -> v.findValue("value").asText())
                .map(v -> {
                    String propertyValue = null;
                    if (wikibaseProperties.getZones().containsKey(v)) {
                        propertyValue = wikibaseProperties.getZones().get(v);
                    }
                    assert propertyValue != null;
                    return propertyValue;
                })
                .block()

        );

    }

    @Test
    void converTime() throws ParseException {

        String format = "yyyy-MM-dd";
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat(format);
        System.out.println(StringUtils.substringBetween("+1942-12-10T00:00:00Z", "+", "T").replaceAll("-", ""));

    }

}