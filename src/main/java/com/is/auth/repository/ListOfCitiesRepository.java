package com.is.auth.repository;

import com.is.auth.model.locations.Cities;
import com.is.auth.model.locations.CitiesDTO;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;


@Repository
public interface ListOfCitiesRepository extends JpaRepository<Cities, Long> {

    @Query("SELECT new com.is.auth.model.locations.CitiesDTO(s.id, s.country_id, " +
            "CASE WHEN :language = 'name_ru' THEN s.name_ru " +
            "WHEN :language = 'name_en' THEN s.name_en " +
            "WHEN :language = 'name_uz' THEN s.name_uz END) " +
            "FROM Cities s WHERE s.country_id = :countryCode")
    List<CitiesDTO> findByLanguage(@Param("language") String language, @Param("countryCode") Integer countryCode);
}



