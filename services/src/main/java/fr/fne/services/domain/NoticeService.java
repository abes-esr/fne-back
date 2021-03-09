package fr.fne.services.domain;

import fr.fne.core.utils.mapper.AutoriteToString;

public interface NoticeService {

    void createNotice(AutoriteToString autoriteToString, String wikibaseItemName);

    void editNotice(AutoriteToString autoriteToString, String ppn);
}
