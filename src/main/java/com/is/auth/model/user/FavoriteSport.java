package com.is.auth.model.user;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.IOException;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class FavoriteSport {
    @JsonProperty("skillId")
    private Integer skillId;

    @JsonProperty("sportId")
    private Integer sportId;

    @JsonProperty("readyToTeach")
    private boolean readyToTeach;

    public FavoriteSport(int skillId, int sportId, boolean readyToTeach) {
        this.skillId = skillId;
        this.sportId = sportId;
        this.readyToTeach = readyToTeach;
    }

    public static List<FavoriteSport> fromJson(String json) {
        try {
            return new ObjectMapper().readValue(json, new TypeReference<List<FavoriteSport>>(){});
        } catch (JsonParseException | JsonMappingException e) {
            throw new RuntimeException("Error parsing JSON: " + e.getMessage(), e);
        } catch (IOException e) {
            throw new RuntimeException("IO error while parsing JSON", e);
        }
    }

    public static String toJson(List<FavoriteSport> sports) {
        try {
            return new ObjectMapper().writeValueAsString(sports);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Error converting to JSON", e);
        }
    }


}