package fr.fne.web.domain.router;


import fr.fne.web.domain.handler.WikiDataSearchHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;

import static org.springframework.web.reactive.function.server.RequestPredicates.GET;
import static org.springframework.web.reactive.function.server.RequestPredicates.accept;

@Configuration
public class WikiDataSearchRouter {

    @Bean
    public RouterFunction<ServerResponse> routeSearch(WikiDataSearchHandler handler) {

       return RouterFunctions.route(
               GET("/api/wiki/search/{term}")
               .and(accept(MediaType.APPLICATION_JSON)),
               handler::searchItemByName
        );
    }
}
