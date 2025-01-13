package com.is.rbs.model.sports;


import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "sport_skills", schema = "sports")
@Getter
@Setter

@Data
public class Sport_skills {
    @Id
    private Integer id;

    @JsonProperty("name_ru")
    private String name_ru;

    @JsonProperty("name_en")
    private String name_en;

    @JsonProperty("name_uz")
    private String name_uz;

    @JsonProperty("min_value")
    private Integer min_value;

    @JsonProperty("max_value")
    private Integer max_value;

}

