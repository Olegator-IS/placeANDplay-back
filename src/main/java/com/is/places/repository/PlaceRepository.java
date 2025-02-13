package com.is.places.repository;

import com.is.places.model.Place;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PlaceRepository extends JpaRepository<Place, Long> {
    List<Place> findByTypeIdAndCurrentLocationCityId(int sportType,int currentCity); // Получение площадок по виду спорта и города
    List<Place> findAllPlacesByCurrentLocationCityId(int currentCity); // Получение площадок по виду спорта
    Place findPlaceByPlaceId(long placeId);

}
