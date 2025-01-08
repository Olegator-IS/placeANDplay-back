package com.is.rbs.model.SwaggerResponses;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel(description = "Response model for not found token /userinfo")
public class UserinfoTokenNotFound {

    @ApiModelProperty(value = "Status", example = "404")
    private int status;
    @ApiModelProperty(value = "Message", example = "TOKEN_NOT_FOUND")
    private String message;

    @ApiModelProperty(value = "Result", example = "ERROR")
    private String result;

    public UserinfoTokenNotFound(int status, String message, String result) {
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