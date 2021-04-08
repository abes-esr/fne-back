package fr.fne.services.domain;

import fr.fne.core.entities.resApiWikibase.PropertyWikibaseValuefr;
import fr.fne.services.domain.entities.WikiDataPersonNotice;
import fr.fne.services.domain.entities.WikiDataSearchItem;
import reactor.core.publisher.Mono;
import reactor.core.publisher.ParallelFlux;

public interface WikiDataService {

    ParallelFlux<WikiDataSearchItem> findItemByNameWithDescription(String itemName);
    ParallelFlux<WikiDataSearchItem> findItemByName(String itemName);
    Mono<PropertyWikibaseValuefr> getItemDescriptionById(String itemId);
}
