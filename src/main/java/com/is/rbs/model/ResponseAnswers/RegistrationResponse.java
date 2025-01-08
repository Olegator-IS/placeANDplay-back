package com.is.rbs.model.ResponseAnswers;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class RegistrationResponse {
    private Long userId;
    private String message;
    private Boolean success;


    public RegistrationResponse(){

    }
    public RegistrationResponse(Long userId,String message,Boolean success) {
        this.userId = userId;
        this.message = message;
        this.success = success;
    }
}
