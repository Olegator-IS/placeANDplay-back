package com.is.events.model;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SportEvent {
    private Long sportTypeId;
    private String sportTypeName;
}
