package fr.fne.services;


import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import fr.fne.core.config.BeanConfigurationCore;
import fr.fne.core.entities.resApiWikibase.DataTime;
import fr.fne.core.entities.resApiWikibase.PropertyWikibaseValuefr;
import fr.fne.core.utils.OAuthHttp;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;

@SpringBootTest
@ContextConfiguration(classes = OAuthHttp.class)
public class CreateWikibaseItemTest {

    @Autowired
    private OAuthHttp oAuthHttp;

    @Test
    void createItem() {
        try {
            //String data = "{\"claims\":[{\"mainsnak\":{\"snaktype\":\"value\",\"property\":\"P166\",\"datavalue\":{\"value\":\""+ppn+"\",\"type\":\"string\"}},\"type\":\"statement\",\"rank\":\"normal\"}]}";
            PropertyWikibaseValuefr propertyWikibaseValuefr = new PropertyWikibaseValuefr("fr", "Test Value Json Item");

            ObjectMapper objectMapper = new ObjectMapper();

            String jsonString = objectMapper.writeValueAsString(propertyWikibaseValuefr);

            System.out.println(jsonString);

            Map<String, String> params = new LinkedHashMap<>();
            String urlWikiBase = "http://fne-test.abes.fr/w/api.php";
            String token = getCsrfToken(urlWikiBase);

            System.out.println(token);

            params.put("action", "wbeditentity");
            params.put("format", "json");
            params.put("new", "item");
            params.put("data", "{\"labels\":{\"fr\":"+ jsonString + "}}");
            params.put("token", token);

            JsonNode json = oAuthHttp.httpOAuthPost(urlWikiBase, params);
            System.out.println("Réponse de WIKIBASE  ===> " + json.toString());

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    void createPropertyTime() {
        try {

            DateTimeFormatter dtf = DateTimeFormatter.ofPattern("+yyyy-MM-dd'T'HH:mm:ss'Z'");
            LocalDateTime now = LocalDateTime.parse("1986-04-08T00:00");

            DataTime dataTime = new DataTime(dtf.format(now), 11);
            ObjectMapper objectMapper = new ObjectMapper();

            String jsonString = objectMapper.writeValueAsString(dataTime);

            System.out.println(jsonString);

            Map<String, String> params = new LinkedHashMap<>();
            String urlWikiBase = "http://fne-test.abes.fr/w/api.php";
            String token = getCsrfToken(urlWikiBase);

            System.out.println(token);

            params.put("action", "wbcreateclaim");
            params.put("format", "json");
            params.put("entity", "Q44");
            params.put("snaktype", "value");
            params.put("property", "P4");
            params.put("value", jsonString);
            params.put("token", token);

            JsonNode json = oAuthHttp.httpOAuthPost(urlWikiBase, params);
            System.out.println("Réponse de WIKIBASE  ===> " + json.toString());

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    void createPropertyItem() {
        try {

            Map<String, String> params = new LinkedHashMap<>();
            String urlWikiBase = "http://fne-test.abes.fr/w/api.php";
            String token = getCsrfToken(urlWikiBase);

            System.out.println(token);

            params.put("action", "wbcreateclaim");
            params.put("format", "json");
            params.put("entity", "Q44");
            params.put("snaktype", "value");
            params.put("property", "P6");
            params.put("value", "{\"entity-type\":\"item\",\"numeric-id\":7}");
            params.put("token", token);

            JsonNode json = oAuthHttp.httpOAuthPost(urlWikiBase, params);
            System.out.println("Réponse de WIKIBASE  ===> " + json.toString());

        } catch (Exception e) {
            e.printStackTrace();
        }
    }



    private String getCsrfToken(String urlWikibase) {
        return oAuthHttp.getCsrfToken(urlWikibase);
    }

    @Test
    void dateTimeTest() {

        String date = "1960-02-11"+"T00:00";
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("+yyyy-MM-dd'T'HH:mm:ss'Z'");
        LocalDateTime now = LocalDateTime.parse(date);

        DataTime dataTime = new DataTime(dtf.format(now), 11);

        System.out.println(dataTime);
    }

}
