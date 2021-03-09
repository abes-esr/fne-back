package fr.fne.core.services;

import fr.abes.cbs.exception.CBSException;
import fr.abes.cbs.process.ProcessCBS;
import fr.fne.core.utils.CbsConnector;

public interface CbsService {

    void getConnection();

    String CreateNewAutorite(String autorite) throws CBSException;

    String getPpn(String notice);

    String searchNoticeWithPpn(String ppn) throws CBSException;

    Boolean checkNoticeExisteWithPpn(String ppn) throws CBSException;

    String editNotice(String notice) throws CBSException;

    String checkDoublonMessageCbs(String notice);

}
