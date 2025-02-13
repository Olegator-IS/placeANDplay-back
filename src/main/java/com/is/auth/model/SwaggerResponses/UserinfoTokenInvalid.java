package com.is.auth.model.SwaggerResponses;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel(description = "Response model for invalid token of method /userinfo")
public class UserinfoTokenInvalid {

    @ApiModelProperty(value = "Status", example = "500")
    private int status;
    @ApiModelProperty(value = "Message", example = "INVALID_TOKEN")
    private String message;

    @ApiModelProperty(value = "Result", example = "ERROR")
    private String result;

    public UserinfoTokenInvalid(int status, String message, String result) {
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