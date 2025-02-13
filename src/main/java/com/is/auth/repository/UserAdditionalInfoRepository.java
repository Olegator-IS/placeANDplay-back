package com.is.auth.repository;

import com.is.auth.model.user.UserAdditionalInfo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public interface UserAdditionalInfoRepository extends JpaRepository<UserAdditionalInfo, Long> {

    @Modifying
    @Query(value = "INSERT INTO user_details (user_id, hobbies, favorite_sports, bio, profile_picture_url, " +
            "last_login, current_location_city_id, current_location_country_id) " +
            "VALUES (:userId, :hobbies, to_jsonb(:favoriteSports::json), :bio, :profilePictureUrl, " +
            "now(), :currentLocationCityId, :currentLocationCountryId)", nativeQuery = true)
    @Transactional
    void insertUserAdditionalInfo(@Param("userId") Long userId,
                                  @Param("hobbies") String hobbies,
                                  @Param("favoriteSports") String favoriteSports,
                                  @Param("bio") String bio,
                                  @Param("profilePictureUrl") String profilePictureUrl,
                                  @Param("currentLocationCityId") Integer currentLocationCityId,
                                  @Param("currentLocationCountryId") Integer currentLocationCountryId);
}