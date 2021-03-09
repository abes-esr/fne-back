package fr.fne.core.entities;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@AllArgsConstructor
@NoArgsConstructor
public class Zone {

    private String zoneNumber;
    private int pos;
    private String tag;
    private String subZones;


}
