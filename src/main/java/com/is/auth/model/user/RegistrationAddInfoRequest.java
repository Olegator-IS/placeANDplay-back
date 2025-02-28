package com.is.auth.model.user;

import io.swagger.annotations.ApiModelProperty;
import io.swagger.annotations.ApiOperation;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Setter
@Getter
@ApiOperation(value = "Вторичная регистрация пользователя", notes = "Передаётся JSON модель, содержащая userId, hobbies, favoriteSports, currentLocationCityId, currentLocationCountryId, bio, profilePictureUrl")
public class RegistrationAddInfoRequest {

    @ApiModelProperty(value = "Идентификатор пользователя", example = "1")
    private Long userId;

    @ApiModelProperty(value = "Хобби пользователя", example = "Reading, Swimming")
    private String hobbies;

   // @ApiModelProperty(value = "Избранные виды спорта пользователя", example = "[{\"sportId\": 1, \"skillId\": 5, \"readyToTeach\": true}, ...]")
    private List<FavoriteSport> favoriteSports;

    @ApiModelProperty(value = "Идентификатор города текущего местоположения", example = "1")
    private Integer currentLocationCityId;

    @ApiModelProperty(value = "Идентификатор страны текущего местоположения", example = "1")
    private Integer currentLocationCountryId;

    @ApiModelProperty(value = "Биография пользователя", example = "I'm Olegator")
    private String bio;

    @ApiModelProperty(value = "URL профильной фотографии пользователя", example = "null")
    private String profilePictureUrl;

    // Конструктор, если необходим
    public RegistrationAddInfoRequest() {}
}