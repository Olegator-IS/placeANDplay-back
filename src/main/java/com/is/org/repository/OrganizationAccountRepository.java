package com.is.org.repository;

import com.is.org.model.OrganizationAccounts;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrganizationAccountRepository extends JpaRepository<OrganizationAccounts, Long> {
    boolean existsByEmail(String email);
    boolean existsByPhone(String phone);

    OrganizationAccounts findByEmail(String email);


}