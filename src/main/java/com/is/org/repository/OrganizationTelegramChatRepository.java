package com.is.org.repository;

import com.is.org.model.OrganizationTelegramChat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OrganizationTelegramChatRepository extends JpaRepository<OrganizationTelegramChat, Long> {

    @Query("SELECT otc FROM OrganizationTelegramChat otc WHERE otc.orgId = :orgId AND otc.isActive = true")
    List<OrganizationTelegramChat> findByOrgIdAndActive(@Param("orgId") Long orgId);

    @Query("SELECT otc FROM OrganizationTelegramChat otc WHERE otc.chatId = :chatId AND otc.isActive = true")
    Optional<OrganizationTelegramChat> findByChatIdAndActive(@Param("chatId") String chatId);

    @Query("SELECT otc FROM OrganizationTelegramChat otc WHERE otc.isActive = true")
    List<OrganizationTelegramChat> findAllActive();

    boolean existsByOrgIdAndIsActiveTrue(Long orgId);

    boolean existsByChatIdAndIsActiveTrue(String chatId);
}
