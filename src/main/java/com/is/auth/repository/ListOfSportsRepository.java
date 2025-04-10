package com.is.auth.repository;

import com.is.auth.model.sports.Sports;
import com.is.auth.model.sports.SportsDTO;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

import org.springframework.data.jpa.repository.Query;


@Repository
public interface ListOfSportsRepository extends JpaRepository<Sports, Long> {

    @Query("SELECT new com.is.auth.model.sports.SportsDTO(s.id, " +
            "CASE WHEN :language = 'name_ru' THEN s.name_ru " +
            "WHEN :language = 'name_en' THEN s.name_en " +
            "WHEN :language = 'name_uz' THEN s.name_uz END) " +
            "FROM Sports s")
    List<SportsDTO> findByLanguage(@Param("language") String language);

    @Query("SELECT s.attr_model FROM Sports s WHERE s.id = :id")
    String findBySportId(@Param("id") int sportId);

}



