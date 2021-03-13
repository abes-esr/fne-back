package fr.fne.core.entities;

import lombok.Data;

import java.io.Serializable;

@Data
public class Recentchange implements Serializable {

    private String title;
    private String timeStamp;

    public Recentchange(String title, String timeStamp) {
        this.title = title;
        this.timeStamp = timeStamp;
    }
}
