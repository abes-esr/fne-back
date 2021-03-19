package fr.fne.services.domain.entities;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@AllArgsConstructor
@NoArgsConstructor
public class WikibaseItem {

    private String status;
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
