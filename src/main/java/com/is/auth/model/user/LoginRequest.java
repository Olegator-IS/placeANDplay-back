package com.is.auth.model.user;

import io.swagger.annotations.ApiModelProperty;
import io.swagger.annotations.ApiOperation;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

@Setter
@Getter
@NoArgsConstructor
@ApiOperation(value = "Авторизация пользователя", notes = "Передаётся JSON модель email и password в открытом виде")
public class LoginRequest {
    @ApiModelProperty(value = "phoneNumber", example = "+998998888931")
    private String phoneNumber;
    @ApiModelProperty(value = "password", example = "Test123")
    private String password;

    public LoginRequest(String phoneNumber, String password){
        this.phoneNumber = phoneNumber;
        this.password = password;
    }
}
