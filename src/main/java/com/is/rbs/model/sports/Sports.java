package com.is.rbs.model.sports;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;

@Entity
@Table(name = "sports", schema = "sports")
@Getter
@Setter

@Data
public class Sports {
    @Id
    private Integer id;

    @JsonProperty("name_ru")
    private String name_ru;

    @JsonProperty("name_en")
    private String name_en;

    @JsonProperty("name_uz")
    private String name_uz;

}
