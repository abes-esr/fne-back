package fr.fne.services.domain;

import fr.fne.services.domain.entities.WikiDataSearchItem;
import reactor.core.publisher.ParallelFlux;

public interface WikiDataService {

    ParallelFlux<WikiDataSearchItem> findItemByName(String itemName);
}
