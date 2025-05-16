package com.is.events.model.chat;

import lombok.Data;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@Data
@ApiModel(description = "Request parameters for fetching chat messages")
public class ChatMessagesRequest {
    @ApiModelProperty(value = "Page number (0-based)", example = "0")
    private int page = 0;

    @ApiModelProperty(value = "Number of messages per page", example = "20")
    private int size = 20;

    @ApiModelProperty(value = "Sort direction (asc/desc)", example = "desc")
    private String sortDirection = "desc";
} 