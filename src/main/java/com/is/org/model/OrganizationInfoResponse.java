package com.is.org.model;

import lombok.Data;

@Data
public class OrganizationInfoResponse {
    private Organizations organization;
    private OrganizationAccounts account;

    public OrganizationInfoResponse(Organizations organization, OrganizationAccounts account) {
        this.organization = organization;
        this.account = account;
    }
}

