package fr.fne.web.domain.router;


import fr.fne.web.domain.handler.WikiDataItemHandler;
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
public class WikiDataItemRouter {

    @Bean
    public RouterFunction<ServerResponse> routeItem(WikiDataItemHandler handler) {

       return RouterFunctions.route(
               GET("/api/wiki/item/description/{term}")
               .and(accept(MediaType.APPLICATION_JSON)),
               handler::findItemDescriptionById
        );
    }
}
