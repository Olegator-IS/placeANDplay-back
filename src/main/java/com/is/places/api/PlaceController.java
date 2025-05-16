package com.is.places.api;

import com.is.places.model.Place;
import com.is.places.service.PlaceService;
import com.is.org.service.OrganizationService;
import com.is.org.model.Organizations;
import io.swagger.annotations.Api;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.Map;

@Api(tags = "Available APIs for the PLACE", description = "List of methods for interacting with PLACE")

@RestController
@RequestMapping("/api/private/places")
@Slf4j
public class PlaceController {
    private final PlaceService placeService;
    private final OrganizationService organizationService;

    @Autowired
    public PlaceController(PlaceService placeService, OrganizationService organizationService) {
        this.placeService = placeService;
        this.organizationService = organizationService;
        log.info("PlaceController initialized!");
    }

    @GetMapping("/allByCity")
    public ResponseEntity<List<Place>> getAllPlaces(
            @RequestParam int currentCity,
            @RequestParam(required = false) List<Integer> sportId,
            @RequestHeader String accessToken,
            @RequestHeader String refreshToken) {

        List<Place> places;
        if (sportId == null || sportId.isEmpty()) {
            places = new ArrayList<>();
        } else {
            places = placeService.getAllPlacesBySport(currentCity, sportId);
        }

        return places.isEmpty() ? ResponseEntity.noContent().build() : ResponseEntity.ok(places);
    }

    @PostMapping("/add")
    public ResponseEntity<Place> addPlace(@RequestBody Place place) {
        return ResponseEntity.ok(placeService.addPlace(place));
    }

    @GetMapping("/sport")
    public ResponseEntity<List<Place>> getPlacesBySportId(@RequestParam int sportTypeId,
                                                          @RequestParam int currentCity,
                                                          @RequestHeader String accessToken,
                                                          @RequestHeader String refreshToken) {
        List<Place> places = placeService.getPlacesBySportIdAndCurrentCity(sportTypeId, currentCity);
        return places.isEmpty() ? ResponseEntity.noContent().build() : ResponseEntity.ok(places);
    }

    @GetMapping("/getPlace")
    public ResponseEntity<?> getPlace(@RequestParam long placeId,
                                    @RequestHeader String accessToken,
                                    @RequestHeader String refreshToken) {
        Place place = placeService.getPlace(placeId);
        if (place == null) {
            return ResponseEntity.noContent().build();
        }

        Map<String, Object> response = new HashMap<>();
        response.put("place", place);

        if (place.getOrgId() != null) {
            Organizations organization = organizationService.getOrganizationById(place.getOrgId());
            if (organization != null) {
                response.put("organization", organization);
            }
        }

        return ResponseEntity.ok(response);
    }


}
