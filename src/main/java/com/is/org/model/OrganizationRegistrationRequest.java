package com.is.org.model;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;

import java.util.List;

@Data
public class OrganizationRegistrationRequest {

    private String email;


    private String password;


    private String phone;



    private String orgType;


    private Long currentLocation;


    private JsonNode attributes;

    private JsonNode orgInfo;

    private List<Long> sportIds;

    private String address;
    private int sportTypeId;
    private String orgName;
    private String description;
    private Double latitude;
    private Double longitude;
}
