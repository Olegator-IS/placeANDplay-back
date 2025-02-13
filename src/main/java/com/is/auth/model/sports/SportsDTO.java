package com.is.auth.model.sports;

import lombok.Data;

@Data
public class SportsDTO {
    private Integer id;
    private String name;

    public SportsDTO(Integer id, String name) {
        this.id = id;
        this.name = name;
    }
}