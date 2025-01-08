package com.is.rbs.model.SwaggerResponses;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel(description = "Response model for successful generating token /userinfo")
public class UserinfoTokenSuccess {

    @ApiModelProperty(value = "Status", example = "200")
    private int status;
    @ApiModelProperty(value = "Message", example = "BUH240")
    private String message;

    @ApiModelProperty(value = "Result", example = "OK")
    private String result;

    public UserinfoTokenSuccess(int status, String message, String result) {
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