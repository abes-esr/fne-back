package fr.fne.web.domain.router;


import fr.fne.web.domain.handler.WikiDataPersonHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;

import static org.springframework.web.reactive.function.server.RequestPredicates.*;

@Configuration
public class WikiDataPersonRouter {

    @Bean
    public RouterFunction<ServerResponse> route(WikiDataPersonHandler handler) {

       return RouterFunctions.route(
               POST("/api/wiki/person")
               .and(accept(MediaType.APPLICATION_JSON)),
               handler::createWikiBaseItem
        ).andRoute(
                GET("/api/wiki/person/langues")
                       .and(accept(MediaType.APPLICATION_JSON)),
               handler::getPersonLangues
       ).andRoute(
                GET("/api/wiki/person/countries")
                        .and(accept(MediaType.APPLICATION_JSON)),
                handler::getPersonCountries
        );

    }
}
