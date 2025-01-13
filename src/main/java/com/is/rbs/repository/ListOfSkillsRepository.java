package com.is.rbs.repository;

import com.is.rbs.model.sports.SkillsDTO;
import com.is.rbs.model.sports.Sport_skills;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;


@Repository
public interface ListOfSkillsRepository extends JpaRepository<Sport_skills, Long> {

    @Query("SELECT new com.is.rbs.model.sports.SkillsDTO(s.id,s.min_value,s.max_value," +
            "CASE WHEN :language = 'name_ru' THEN s.name_ru " +
            "WHEN :language = 'name_en' THEN s.name_en " +
            "WHEN :language = 'name_uz' THEN s.name_uz END) " +
            "FROM Sport_skills s")
    List<SkillsDTO> findByLanguage(@Param("language") String language);
}



