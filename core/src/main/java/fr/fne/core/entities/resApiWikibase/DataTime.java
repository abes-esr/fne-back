package fr.fne.core.entities.resApiWikibase;

import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DataTime {

    private String time ;
    @NonNull
    private final int timezone = 0;
    @NonNull
    private final int before = 0;
    @NonNull
    private final int after = 0;
    @NonNull
    private final int precision = 11;
    @NonNull
    private final String calendarmodel = "http://www.wikidata.org/entity/Q1985727";

}
