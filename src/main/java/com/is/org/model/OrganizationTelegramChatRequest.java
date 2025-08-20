package com.is.org.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;



@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrganizationTelegramChatRequest {


    private Long orgId;


    private String chatId;

    private String chatName;

    private String botToken;
}
