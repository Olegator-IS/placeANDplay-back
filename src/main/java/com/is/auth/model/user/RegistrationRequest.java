package com.is.auth.model.user;

import io.swagger.annotations.ApiModelProperty;
import io.swagger.annotations.ApiOperation;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

@Setter
@Getter
@NoArgsConstructor
@ApiOperation(value = "Первичная регистрация пользователя", notes = "Передаётся JSON модель,содержащая email," +
        "password - в открытом виде,fisrt_name и last_name")

public class RegistrationRequest {
    @ApiModelProperty(value = "phoneNumber", example = "+998998888931")
    private String phoneNumber;
    @ApiModelProperty(value = "password", example = "Test123")
    private String password;
    @ApiModelProperty(value = "firstName", example = "Oleg")
    private String firstName;
    @ApiModelProperty(value = "lastName", example = "Abramov")
    private String lastName;

    public RegistrationRequest(String phoneNumber,String password,String firstName,String lastName){
        this.phoneNumber = phoneNumber;
        this.password = password;
        this.firstName = firstName;
        this.lastName = lastName;
    }
}
