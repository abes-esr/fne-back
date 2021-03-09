package fr.fne.services.domain;

import com.fasterxml.jackson.core.JsonProcessingException;
import fr.fne.services.event.entities.WikibaseItem;
import reactor.core.publisher.Mono;

public interface WikibaseDataService {

    void insertPpn(String urlWikiBase, String csrftoken, String id, String ppn);

    String getCsrfToken(String urlWikibase);
    Mono<WikibaseItem> save(WikibaseItem wikibaseItem) throws Exception;
}
