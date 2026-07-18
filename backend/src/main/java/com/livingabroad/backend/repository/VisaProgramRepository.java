package com.livingabroad.backend.repository;

import com.livingabroad.backend.entity.VisaProgram;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface VisaProgramRepository extends JpaRepository<VisaProgram, Long> {

    Optional<VisaProgram> findFirstByCountryCodeAndIsActiveTrue(String countryCode);
}
