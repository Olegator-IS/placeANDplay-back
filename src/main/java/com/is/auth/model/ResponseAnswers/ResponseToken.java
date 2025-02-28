package com.is.auth.model.ResponseAnswers;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@JsonInclude(JsonInclude.Include.NON_NULL)
@ApiModel(description = "Response model for successful generating token")
public class ResponseToken {

    @ApiModelProperty(value = "Status", example = "200")
    private int status;
    @ApiModelProperty(value = "Message", example = "BUH240")
    private Object message;
    @ApiModelProperty(value = "Result", example = "OK")
    private Object result;
//    @ApiModelProperty(value = "AdditionalInfo", example = "{" +
//            "    \"user_id\": '1'," +
//            "    \"first_name\": \"'Oleg'\"," +
//            "    \"last_name\": \"'Abramov'\"" +
//            "    \"email\": \"'abramov.o.o.1998@gmail.com'\"" +
//            "  }")
    private Object additionalInfo;


    public ResponseToken() {

    }

    public ResponseToken(int status, Object message, Object result) {
        super();
        this.status = status;
        this.message = message;
        this.result = result;

    }

}
