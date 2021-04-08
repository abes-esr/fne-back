package fr.fne.services.domain.entities;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import reactor.core.publisher.Mono;


@Data
@AllArgsConstructor
@NoArgsConstructor
public class WikiDataPersonNotice {

    private String status;
    private String ppn;
    private String itemId;
    private String instantOf;
    private String firstName;
    private String lastName;
    private String dateBirth;
    private String dateDead;
    private String langue;
    private String country;
    private String source;

}
