package com.is.places.api;

import com.is.places.model.Place;
import com.is.places.service.PlaceService;
import io.swagger.annotations.Api;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@Api(tags = "Available APIs for the PLACE", description = "List of methods for interacting with PLACE")

@RestController
@RequestMapping("/api/private/places")
@Slf4j
public class PlaceController {
    private final PlaceService placeService;

    @Autowired
    public PlaceController(PlaceService placeService) {
        this.placeService = placeService;
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
    public ResponseEntity<Place> getPlace(@RequestParam long placeId,
                                          @RequestHeader String accessToken,
                                          @RequestHeader String refreshToken) {
        Place places = placeService.getPlace(placeId);
        return places==null ? ResponseEntity.noContent().build() : ResponseEntity.ok(places);
    }


}
