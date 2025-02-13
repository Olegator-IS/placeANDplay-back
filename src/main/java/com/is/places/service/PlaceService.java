package com.is.places.service;

import com.is.places.model.Place;
import com.is.places.repository.PlaceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PlaceService {

    private final PlaceRepository placeRepository;


    public List<Place> getAllPlaces(int city) {
        return placeRepository.findAllPlacesByCurrentLocationCityId(city);
    }

    public Place addPlace(Place place) {
        return placeRepository.save(place);
    }

    public List<Place> getPlacesBySportIdAndCurrentCity(int sportType,int currentCity) {
        System.out.println(currentCity);
        return placeRepository.findByTypeIdAndCurrentLocationCityId(sportType,currentCity);
    }

    public Place getPlace(long placeId) {
        return placeRepository.findPlaceByPlaceId(placeId);
    }
}