package com.is.org.model;
import lombok.Data;

@Data
public class OrganizationLoginRequest {
    private String email;
    private String password;
}
