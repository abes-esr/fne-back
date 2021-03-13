package fr.fne.core.utils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.scribejava.apis.MediaWikiApi;
import com.github.scribejava.core.builder.ServiceBuilder;
import com.github.scribejava.core.model.OAuth1AccessToken;
import com.github.scribejava.core.model.OAuthRequest;
import com.github.scribejava.core.model.Verb;
import com.github.scribejava.core.oauth.OAuth10aService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Se connecter avec la méthode OAuth 1.0 à Wikibase
 * OAuth 1.0 est plus sécurisé et recommandé par Wikibase
 * Utilisation de la librairie Java Scribe (https://github.com/scribejava/scribejava)
 */
@Slf4j
@Component
public class OAuthHttp {

    @Value("${wikibase.oauth.consumerToken}")
    private String consumerToken;
    @Value("${wikibase.oauth.consumerSecret}")
    private String consumerSecret;
    @Value("${wikibase.oauth.accessToken}")
    private String accessToken;
    @Value("${wikibase.oauth.accessSecret}")
    private String accessSecret;

    private OAuth1AccessToken oAuthAccessToken;
    private OAuth10aService oAuthService;
    //########################### Version avec utilisation de Scribe pour l'authentification owner-only Connected App / OAuth 1.0

    //OAUth
    public JsonNode httpOAuthGet(String url) throws Exception {
        return httpOAUthCall(Verb.GET, url, Collections.emptyMap(), Collections.emptyMap());
    }

    //OAUth
    public JsonNode httpOAuthPost(String url, Map<String, String> params) throws Exception {
        Map<String, String> map = new HashMap<String, String>();
        map.put("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");
        return httpOAUthCall(Verb.POST, url, map, params);
    }

    //OAuth
    private JsonNode httpOAUthCall(Verb verb, String url, Map<String, String> headers, Map<String, String> params) throws Exception {
        OAuthRequest request = new OAuthRequest(verb, url);
        request.setCharset(StandardCharsets.UTF_8.name());
        params.forEach(request::addParameter);
        headers.forEach(request::addHeader);
        //OAuth 1.0
        // https://meta.wikimedia.org/wiki/User-Agent_policy
        String userAgent = "";
        request.addHeader("User-Agent", userAgent);
        oAuthService.signRequest(oAuthAccessToken, request);

        //logger.info("CompleteUrl :  "+request.getCompleteUrl());
        /*Collection<String>  ListofKeys = request.getHeaders().values().stream().collect(Collectors. 
                toCollection(ArrayList::new)); 
        logger.info(ListofKeys.toString());
        */
        return new ObjectMapper().readTree(oAuthService.execute(request).getBody());
    }

    //Connexion à WikiBase avec un compte OAuth
    //oAuth vaudra forcément true, donc getJson et postJson utiliseront les versions avec OAuth
    private void connectOauth() {
        oAuthService = new ServiceBuilder(consumerToken).apiSecret(consumerSecret).build(MediaWikiApi.instance());
        //Sinon, avec debug :
        //oAuthService = new ServiceBuilder(consumerToken).debug().apiSecret(consumerSecret).build(MediaWikiApi.instance());
        oAuthAccessToken = new OAuth1AccessToken(accessToken, accessSecret);

        // Check authentication
        //logger.info(Utilitaire.getJson(urlWikiBase + "?action=query&meta=userinfo&uiprop=blockinfo|groups|rights|ratelimits&format=json"));
        // Fetch CSRF token, mandatory for upload using the Mediawiki API
    }

    public String getCsrfToken(String urlWikibase) {

        String csrfToken = null;
        try {
            connectOauth();
            csrfToken = httpOAuthGet(urlWikibase + "?action=query&meta=tokens&format=json").findValue("csrftoken").asText();
        } catch (Exception ex) {
            log.error(ex.getMessage());
        }
        return csrfToken;
    }

}