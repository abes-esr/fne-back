package fr.fne.services.oauth;

import com.github.scribejava.apis.MediaWikiApi;
import com.github.scribejava.core.builder.ServiceBuilder;
import com.github.scribejava.core.model.*;
import com.github.scribejava.core.oauth.OAuth10aService;
import com.github.scribejava.core.oauth.OAuth20Service;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Random;
import java.util.Scanner;

/**
 * Utiliser cette classe seulement pour récupérer les jetons secrets
 * Ensuite il faut mettre à jour les jetons dans les properties de l'application
 */

@Service
@Slf4j
public class OauthService {

    @Value("${wikibase.oauth.consumerToken}")
    private String CONSUMER_KEY;
    @Value("${wikibase.oauth.consumerSecret}")
    private String CONSUMER_SECRET;
    @Value("${wikibase.oauth.userinfo-url}")
    private String API_USERINFO_URL;
    @Value("${wikibase.oauth.index-url}")
    private String indexUrl;
    @Value("${wikibase.oauth.nice-url}")
    private String niceUrl;

    // Connexion à OAuth1 avec demande à l'utilisateur
    public void go() {
        try {

            MediaWikiApi mediaWiki = new MediaWikiApi(indexUrl, niceUrl);
            OAuth10aService service = new ServiceBuilder(CONSUMER_KEY).apiSecret(CONSUMER_SECRET).build(mediaWiki);
            Scanner in = new Scanner(System.in);

            log.info("=== MediaWiki's OAuth Workflow ===");

            // Obtain the Request Token
            log.info("Fetching the Request Token...");
            OAuth1RequestToken requestToken = service.getRequestToken();
            log.info("Got the Request Token!");

            log.info("Now go and authorize ScribeJava here:");
            log.info(service.getAuthorizationUrl(requestToken));
            log.info("And paste the verifier here");
            log.info(">>");
            final String oauthVerifier = in.nextLine();

            // Trade the Request Token and Verifier for the Access Token
            log.info("Trading the Request Token for an Access Token...");
            final OAuth1AccessToken accessToken = service.getAccessToken(requestToken, oauthVerifier);
            log.info("Got the Access Token!");
            log.info("(The raw response looks like this: " + accessToken.getRawResponse() + "')");

            // Now let's go and ask for a protected resource!
            log.info("Now we're going to access a protected resource...");
            final OAuthRequest request = new OAuthRequest(Verb.GET, API_USERINFO_URL);
            service.signRequest(accessToken, request);
            try (Response response = service.execute(request)) {
                log.info("Got it! Lets see what we found...");
                log.info(response.getBody());
            }
            log.info("Thats it man! Go and build something awesome with MediaWiki and ScribeJava! :)");
        } catch (Exception e) {
            log.error(e.getMessage());
            e.printStackTrace();
        }
    }

    public void go2() {
        try {

            OAuth20Service service = new ServiceBuilder(CONSUMER_KEY).apiSecret(CONSUMER_SECRET).build(MediaWiki20API.instance());
            Scanner in = new Scanner(System.in);

            log.info("=== MediaWiki's OAuth Workflow ===");

            // Obtain the Authorization URL
            log.info("Fetching the Authorization URL...");
            final String secretState = "secret" + new Random().nextInt(999_999);
            final String authorizationUrl = service.getAuthorizationUrl(secretState);

            log.info("Got the Authorization URL!");
            log.info("Now go and authorize ScribeJava here:");
            System.out.println(authorizationUrl);
            log.info("And paste the authorization code here");
            System.out.print(">>");
            final String code = in.nextLine();
            System.out.println();

            log.info("Trading the Authorization Code for an Access Token...");
            final OAuth2AccessToken accessToken = service.getAccessToken(code);
            log.info("Got the Access Token!");
            log.info("(The raw response looks like this: " + accessToken.getRawResponse() + "')");
            System.out.println();

            log.info("Now we're going to access a protected profile resource...");
            final OAuthRequest request = new OAuthRequest(Verb.GET, API_USERINFO_URL);
            service.signRequest(accessToken, request);
            try (Response response = service.execute(request)) {
                log.info("Got it! Lets see what we found...");
                log.info(response.getBody());
            }
            log.info("Thats it man! Go and build something awesome with MediaWiki and ScribeJava! :)");
        } catch (Exception e) {
            log.error(e.getMessage());
            e.printStackTrace();
        }
    }
}
