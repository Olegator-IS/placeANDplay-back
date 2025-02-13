package com.is.auth.model.locations;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "cities", schema = "locations")
@Getter
@Setter

@Data
public class Cities {
    @Id
    private Integer id;

    @JsonProperty("name_ru")
    private String name_ru;

    @JsonProperty("name_en")
    private String name_en;

    @JsonProperty("name_uz")
    private String name_uz;

    @JsonProperty("country_id")
    private Integer country_id;

}
