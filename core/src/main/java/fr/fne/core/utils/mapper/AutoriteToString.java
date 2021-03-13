package fr.fne.core.utils.mapper;

import fr.fne.core.entities.Autorite;
import fr.fne.core.entities.Zone;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Cette classe est utilisée pour convertir l'objet java en chaine String avant de l'insérer dans le CBS, comme une notice
 */
@Slf4j
@Component
public class AutoriteToString {

    @Setter
    private Autorite autorite;

    public String convertAutoriteToString() {

        StringBuilder stringBuilderZone = new StringBuilder();
        try {
            List<Zone> zones = autorite.getZones();
            // On ajoute l'entête de la notice : toujours avec 008 $aTp5
            stringBuilderZone.append("008 $aTp5\n");
            int nbCountZone = 0;

            // On boucle sur la liste de zone de notre objet java Autorite
            for (int i = 0; i < zones.size(); i++) {

                StringBuilder stringBuilderSubZone = new StringBuilder();
                // Chaque tour de boucle on rajoute le zone number (200,100,033,035 etc..)
                stringBuilderZone.append(zones.get(i).getZoneNumber());
                // Ensuite on rajoute les tags après le numéro de zone (##,#1,#2,etc)
                //stringBuilderZone.append(zones.get(i).getTag());

                //Ici on check, si c'est la derniere ligne : on sort de la boucle
                if (i == zones.size() - 1) {
                    stringBuilderSubZone.append(zones.get(i).getSubZones());
                    // Si on est sur la même ligne, on supprime le zone number afin de garder seulement les subzones
                    // Parce ce qu'à chaque tour de boucle, le script rajoute aussi les zone number au début
                    if (nbCountZone > 0) {
                        stringBuilderZone.delete(stringBuilderZone.length() - 7, stringBuilderZone.length());
                    }
                    stringBuilderZone.append(stringBuilderSubZone.toString());
                    break;
                }

                // On regarde si la zone avec indice +1 == l'indice. Si oui, on ajoute les subzones dans la même ligne
                if (zones.get(i + 1).getZoneNumber().substring(0, 3).equals(zones.get(i).getZoneNumber().substring(0, 3))
                        && zones.get(i + 1).getPos() == zones.get(i).getPos() && (zones.get(i).getPos() > 0 ||
                        zones.get(i).getPos() == 0)) {

                    stringBuilderSubZone.append(zones.get(i).getSubZones());
                    stringBuilderSubZone.append(zones.get(i + 1).getZoneNumber(), 5, 7);
                    // On utilise cette variable afin de savoir si on est toujours sur la même ligne
                    nbCountZone++;
                    // si non, on ajoute le saut de ligne à la fin
                } else {
                    stringBuilderSubZone.append(zones.get(i).getSubZones());
                    if (nbCountZone > 0) {
                        stringBuilderZone.delete(stringBuilderZone.length() - 7, stringBuilderZone.length());
                    }
                    stringBuilderSubZone.append("\n");
                    nbCountZone = 0;
                }
                // On supprime les zones number à partir de la 2ème boucle une fois on saute la ligne
                if (nbCountZone > 1) {
                    stringBuilderZone.delete(stringBuilderZone.length() - 7, stringBuilderZone.length());
                }

                stringBuilderZone.append(stringBuilderSubZone.toString());

            }
        } catch (Exception ex) {
            log.error(ex.getMessage());
        }
        return stringBuilderZone.toString();
    }

}
