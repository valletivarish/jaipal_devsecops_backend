package com.airquality.repository;

import com.airquality.model.PollutantType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PollutantTypeRepository extends JpaRepository<PollutantType, Long> {

    Optional<PollutantType> findByName(String name);

    boolean existsByName(String name);
}
