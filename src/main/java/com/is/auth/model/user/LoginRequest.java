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
    @ApiModelProperty(value = "email", example = "abramov.o.o.1998@gmail.com")
    private String email;
    @ApiModelProperty(value = "password", example = "Test123")
    private String password;

    public LoginRequest(String email, String password){
        this.email = email;
        this.password = password;
    }
}
