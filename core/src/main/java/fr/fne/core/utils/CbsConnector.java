package fr.fne.core.utils;

import fr.abes.cbs.exception.CBSException;
import fr.abes.cbs.process.ProcessCBS;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Connecxion à CBS avec l'API CBS ACCES
 */

@Slf4j
@Component
public class CbsConnector {

    @Getter
    @Value("${cbs.host}")
    private String host;

    @Getter
    @Value("${cbs.port}")
    private String port;

    @Getter
    @Value("${cbs.login}")
    private String login;

    @Getter
    @Value("${cbs.pwd}")
    private String passWord;

    private ProcessCBS client;

    public ProcessCBS getClient() {

        try {
            client = new ProcessCBS();
            client.authenticate(getHost(), getPort(), getLogin(), getPassWord());

        } catch (CBSException ex) {

            log.error(ex.getMessage());

        }

        return client;
    }

}
