package com.is.auth.model.SwaggerResponses;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel(description = "Response model for internal error of generating token of method /login")
public class LoginAuthInternalError {

    @ApiModelProperty(value = "Status", example = "500")
    private int status;
    @ApiModelProperty(value = "Message", example = "TOKEN_GENERATION_ERROR")
    private String message;

    @ApiModelProperty(value = "Result", example = "ERROR")
    private String result;

    public LoginAuthInternalError(int status, String message, String result) {
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
