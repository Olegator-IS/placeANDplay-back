package com.is.rbs.model.SwaggerResponses;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel(description = "Response model for invalid username or password of method /login")
public class LoginAuthFailed {

    @ApiModelProperty(value = "Status", example = "401")
    private int status;
    @ApiModelProperty(value = "Message", example = "INVALID_USERNAME_OR_PASSWORD")
    private String message;

    @ApiModelProperty(value = "Result", example = "Пользователь не найден")
    private String result;

    public LoginAuthFailed(int status, String message, String result) {
        this.status = status;
        this.message = message;
        this.result = result;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getResult() {
        return result;
    }

    public void setResult(String result) {
        this.result = result;
    }
}
