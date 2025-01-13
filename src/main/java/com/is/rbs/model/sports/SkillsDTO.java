package com.is.rbs.model.sports;

import lombok.Data;

@Data
public class SkillsDTO {
    private Integer id;
    private String name;
    private Integer min_value;
    private Integer max_value;

    public SkillsDTO(int id, int min_value, int max_value, String name) {
        this.id = id;
        this.min_value = min_value;
        this.max_value = max_value;
        this.name = name;
    }
}