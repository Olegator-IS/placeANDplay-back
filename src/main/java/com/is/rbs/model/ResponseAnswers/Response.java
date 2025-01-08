package com.is.rbs.model.ResponseAnswers;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

@JsonInclude(JsonInclude.Include.NON_NULL)
@ApiModel(description = "Response model for successful generating token")
@Getter
@Setter
public class Response {

    @ApiModelProperty(value = "Status", example = "200")
    private int status;
    @ApiModelProperty(value = "Message", example = "OK")
    private Object message;
    @ApiModelProperty(value = "Result", example = "OK")
    private Object result;
    @ApiModelProperty(value = "TokenInfo", example = "accessToken/refreshToken")
    private Object tokenInfo;
    @ApiModelProperty(value = "ErrorCode", example = "EMAIL_IS_ALREADY_EXIST")
    private Object errorCode;


    public Response(){

    }

    public Response(int status, Object message, Object result) {
        super();
        this.status = status;
        this.message = message;
        this.result = result;
    }

    public Response(Object errorCode,Object Message,int Status){
        super();
        this.status = Status;
        this.message = Message;
        this.errorCode = errorCode;
    }
}
