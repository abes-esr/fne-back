package fr.fne.core.services.serviceImpl;

import fr.abes.cbs.exception.CBSException;
import fr.abes.cbs.process.ProcessCBS;
import fr.fne.core.services.CbsService;
import fr.fne.core.utils.CbsConnector;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;


@Slf4j
@Service
public class CbsServiceImpl implements CbsService {

    private final CbsConnector cbsConnector;

    @Getter
    private ProcessCBS client;

    @Autowired
    public CbsServiceImpl(CbsConnector cbsConnector) {
        this.cbsConnector = cbsConnector;
    }

    @Override
    public void getConnection() {
        this.client = cbsConnector.getClient();
    }

    @Override
    public String CreateNewAutorite(String autorite) throws CBSException {
        return client.enregistrerNewAut(autorite);
    }

    @Override
    public String getPpn(String notice) {
        String Str1D = new String(new char[]{(char) 29});
        return StringUtils.substringBetween(notice, "03Notice créée avec PPN ", Str1D);
    }

    @Override
    public String searchNoticeWithPpn(String ppn) throws CBSException {
        return client.search("che ppn " + ppn);
    }

    @Override
    public Boolean checkNoticeExisteWithPpn(String ppn) throws CBSException {
        boolean find = false;
        String end = "</TD>";
        String result = client.search("che ppn " + ppn);
        String response = StringUtils.substringBetween(result, "No notice : ", end);
        System.out.println("PPN trouvé dans CBS ==> " + response);
        if (result.startsWith("02PPN erro")) {
            return false;
        } else if (!result.startsWith("02PPN erro") && response != null && response.length() > 0) {
            find = true;
        }
        return find;
    }

    @Override
    public String editNotice(String notice) throws CBSException {
        String response = null;
        String result = client.modifierNotice("1", notice);
        if (result.startsWith("03OK")) {
            response = "Notice modifiée dans CBS avec succès";
        } else {
            response = "Notice non modifiée";
        }
        return response;
    }

    @Override
    public String checkDoublonMessageCbs(String notice) {
        String result = null;
        String Str1D = new String(new char[]{(char) 29});
        if (notice.startsWith("03Doublon possible")) {
            result = "Doublon possible: ==> "
                    + "Ancien PPN trouvé : "
                    + StringUtils.substringBetween(notice, "trouve aussi PPN ", Str1D)
                    + " - Nouveau PPN: "
                    + StringUtils.substringBetween(notice, "03Notice créée avec PPN ", Str1D);
        }
        return result;
    }
}
