package fr.fne.services.domain.serviceimpl;

import fr.abes.cbs.exception.CBSException;
import fr.fne.core.services.CbsService;
import fr.fne.core.utils.mapper.AutoriteToString;
import fr.fne.services.domain.NoticeService;
import fr.fne.services.event.entities.CbsNoticeCreated;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

@Slf4j
@RequiredArgsConstructor
@Service
public class NoticeServiceImpl implements NoticeService {

    private final ApplicationEventPublisher applicationEventPublisher;
    private final CbsService cbsService;


    @Override
    public void createNotice(AutoriteToString autoriteToString, String wikibaseItemName) {

        System.out.println("============ Starting insertion in CBS =================");
        String result = null;
        String ppn = null;
        try {
            System.out.println("============ Connecting CBS =================");
            cbsService.getConnection();
            result = cbsService.CreateNewAutorite(autoriteToString.convertAutoriteToString());
            ppn = cbsService.getPpn(result);
            String doublonMessageCbs = cbsService.checkDoublonMessageCbs(result);

            if (doublonMessageCbs != null) {
                log.info(doublonMessageCbs);
            }
            log.info("PPN créé = " + ppn);

            System.out.println("============ New event CBS notice created  =================");
            // Création event
            if (wikibaseItemName != null && ppn != null) {
                applicationEventPublisher.publishEvent(new CbsNoticeCreated(wikibaseItemName, ppn));
            }

        } catch (CBSException e) {
            log.error(e.getMessage());
        }
    }

    @Override
    public void editNotice(AutoriteToString autoriteToString, String ppn) {
        System.out.println("============ Warning: PPN existed ,Starting modify notice in CBS =================");

        try {
            // Si le PPN existe : on modifie la notice correspondante dans CBS. Pas d'insertion de nouvelle notice
            // On vérifie quand même que le PPN existe vraiment dans le CBS avant de faire une modification
            System.out.println("============ Connecting CBS =================");
            cbsService.getConnection();
            if (cbsService.checkNoticeExisteWithPpn(ppn)) {
                String editResult = cbsService.editNotice(autoriteToString.convertAutoriteToString());
                log.info(editResult);
            } else {
                log.info("PPN non trouvé dans CBS");
            }

        } catch (CBSException e) {
            log.error(e.getMessage());
        }
    }
}
