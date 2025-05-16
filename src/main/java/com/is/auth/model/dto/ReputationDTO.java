package com.is.auth.model.dto;

import lombok.Data;
import lombok.Builder;

@Data
@Builder
public class ReputationDTO {
    private Double rating;
    private Integer reviewsCount;
} 