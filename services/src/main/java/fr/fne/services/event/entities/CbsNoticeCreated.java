package fr.fne.services.event.entities;


import lombok.*;

/**
 * L'objet utilisé pour créer des spring events
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CbsNoticeCreated {
    private String name;
    private String ppn;
}
