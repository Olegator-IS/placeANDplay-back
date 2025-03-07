package com.is.events.dto;

import com.is.events.model.OrganizerEvent;
import com.is.events.model.SportEvent;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.time.LocalDateTime;

@Data
public class CreateEventRequest {
    
    @NotBlank(message = "Title is required")
    private String title;
    
    @NotBlank(message = "Description is required")
    private String description;
    
    @NotNull(message = "DateTime is required")
    private LocalDateTime dateTime;
    
    @NotNull(message = "PlaceId is required")
    private Long placeId;
    
    @NotNull(message = "OrganizerId is required")
    private Long organizerId;
    
    @Valid
    @NotNull(message = "SportEvent is required")
    private SportEvent sportEvent;
    
    @Valid
    @NotNull(message = "OrganizerEvent is required")
    private OrganizerEvent organizerEvent;
} 