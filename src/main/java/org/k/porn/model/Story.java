package org.k.porn.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Story {
    private String title;
    private String author;
    private String idAuthor;
    private String key;
    private String desc;
    private String type;
    private String note;
    private String date;
    private List<String> tags = new ArrayList<>();
}