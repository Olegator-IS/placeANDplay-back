package com.is.auth.model.user;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
@ApiModel(description = "Request model for user registration with additional information")
public class RegistrationAddInfoRequest {
    @ApiModelProperty(value = "User ID", example = "1")
    private Long userId;

    @ApiModelProperty(value = "User hobbies (comma-separated)", example = "reading,swimming,photography")
    private String hobbies;

    @ApiModelProperty(value = "User's favorite sports")
    private List<FavoriteSport> favoriteSports;

    @ApiModelProperty(value = "Current location city ID", example = "1")
    private Integer currentLocationCityId;

    @ApiModelProperty(value = "Current location country ID", example = "1")
    private Integer currentLocationCountryId;

    @ApiModelProperty(value = "User biography", example = "Sports enthusiast and outdoor lover")
    private String bio;

    @ApiModelProperty(value = "User gender", example = "male")
    private String gender;

    @ApiModelProperty(value = "User birth date", example = "1990-01-01")
    private LocalDate birthDate;

    @ApiModelProperty(value = "User availability schedule", example = "{\"monday\":\"9:00-17:00\",\"tuesday\":\"9:00-17:00\"}")
    private String availability;

    @ApiModelProperty(value = "What the user is looking for", example = "Looking for tennis partners")
    private String lookingFor;

    @ApiModelProperty(value = "Whether user is open to new connections", example = "true")
    private Boolean openToNewConnections;

    @ApiModelProperty(value = "User contact information", example = "{\"telegram\":\"@username\",\"instagram\":\"@username\"}")
    private String contacts;
}