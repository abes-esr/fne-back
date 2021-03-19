package fr.fne.services.event.eventlistener;

import fr.fne.services.domain.WikiDataServicePersonNotice;
import fr.fne.services.event.entities.CbsNoticeCreated;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;


/**
 * Event listener d'objet java CbsNoticeCreated
 */
@RequiredArgsConstructor
@Slf4j
@Component
public class CbsNoticeCreatedListener {

    private String csrfToken;

    @Value("${wikibase.urls.fne}")
    private String urlWikiBase;

    private final WikiDataServicePersonNotice wikibaseDataService;


    @EventListener
    @Async
    public void handle(CbsNoticeCreated event) {
        log.info("Event PPN created ");
        try {
            if (csrfToken == null) {
                System.out.println("=========== Connection to WIKIBASE with token CSRF OAUTH 1.0 ================");
                csrfToken = wikibaseDataService.getCsrfToken(urlWikiBase);
                log.info("The csrftoken is : " + csrfToken);
            }
        } catch (Exception e) {
            e.printStackTrace();
            log.error(e.getMessage());
        }

        System.out.println("=========== Update ppn to Wikibase================");

        try {
            wikibaseDataService.insertPpn(urlWikiBase, csrfToken, event.getName(), event.getPpn());
        } catch (Exception e) {
            e.printStackTrace();
            log.error(e.getMessage());
        }
    }

}
