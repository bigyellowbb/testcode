package com.example.demo.repository;

import com.example.demo.model.PreData;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

public interface PreDataRepository extends JpaRepository<PreData, Long> {

    @Modifying
    @Transactional
    @Query(value = "TRUNCATE TABLE prediction_data", nativeQuery = true)
    void truncateTable();
}