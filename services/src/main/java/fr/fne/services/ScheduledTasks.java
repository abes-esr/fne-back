package fr.fne.services;


import fr.fne.core.entities.Autorite;
import fr.fne.core.entities.Recentchange;
import fr.fne.core.utils.GetRecentChangeListToObject;
import fr.fne.core.utils.mapper.AutoriteToString;
import fr.fne.core.utils.mapper.ResponseToAutorite;
import fr.fne.services.domain.NoticeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Dans cette classe on a mis en place un CRON avec le timestamp par défaut pour la liste de "recent change" dans Wikibase
 * Ensuite, à chaque fois que le CRON démarre il va affecter la nouvelle valeur de timestamp avec le timestamp du dernier
 * élément dans la liste "recent change" de Wikibase
 * Le but est d'obtenir la derniere nouvelle liste de "recent change" dans Wikibase avec timestamp
 */

@RequiredArgsConstructor
@Slf4j
@Component
public class ScheduledTasks {

    private final GetRecentChangeListToObject getRecentChangeListToObject;

    private final AutoriteToString autoriteToString;

    private final ResponseToAutorite responseToAutorite;

    private final NoticeService noticeService;

    @Value("${wikibase.timestamp}")
    private String timeStamp; //2021-02-01 : 09:00:00

    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm:ss");

    @Scheduled(fixedRate = 60 * 1000)
    public void launchAppCron() {
        log.info("************ The time is now {} ************", dateFormat.format(new Date()));
        log.info("************ Starting Application Scheduled ************");

        System.out.println("============ Get all items in page recent change of Wikibase =================");
        // Donner le temps pour la liste "recent change" dans wikibase
        // http://fagonie-test.v102.abes.fr:8181/w/api.php?action=help&modules=query%2Brecentchanges
        getRecentChangeListToObject.setTimeEnd(this.timeStamp);

        // Récupère la liste d'objet java => Recentchange

        Flux<Recentchange> recentChangeList = getRecentChangeListToObject.toObjectReactive();
        //List<Recentchange> recentChangeList = getRecentChangeListToObject.toObjectReactive().collectList().block();

        recentChangeList.subscribe(System.out::println);

        recentChangeList.subscribe(t -> {
            // On donne le Q item dans le lien
            responseToAutorite.setItem(t.getTitle());

            Autorite autorite = null;
            try {
                autorite = responseToAutorite.toObjectReactive();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            System.out.println(autorite);

            if (autorite != null && autorite.getIsNotice()) {

                autoriteToString.setAutorite(autorite);
                System.out.println(autoriteToString.convertAutoriteToString());

                // On regarde si le PPN existe ou non dans CBS avant de créer une nouvelle notice
                if (!autorite.getExiste()) {
                    // Si le PPN n'existe pas (non trouvé le zone 001 dans Wikibase ) , on va créer une nouvelle notice dans le CBS
                    // Ensuite, on va récupérer le PPN retourné par le CBS afin de pouvoir l'insérer dans le Wikibase

                    noticeService.createNotice(autoriteToString, t.getTitle());
                } else {
                    // Si le PPN a trouvé dans Wikibase: Essayer de mettre à jour la notice dans CBS
                    noticeService.editNotice(autoriteToString, autorite.getPpn());
                }
            }

        });

        // Récuperer le temp dernier item dans la nouvelle liste recent change de Wikibase
        this.timeStamp = recentChangeList.elementAt(0)
                .map(v -> v.getTimeStamp().replaceAll("[-T:Z]", ""))
                .block();

        //this.timeStamp = recentChangeList.get(0).getTimeStamp().replaceAll("[-T:Z]", "");

    }

}
