package com.is.rbs.model.locations;

import lombok.Data;

@Data
public class CitiesDTO {
    private Integer id;
    private Integer country_id;
    private String name;

    public CitiesDTO(Integer id,Integer country_id,String name) {
        this.id = id;
        this.country_id = country_id;
        this.name = name;
    }
}