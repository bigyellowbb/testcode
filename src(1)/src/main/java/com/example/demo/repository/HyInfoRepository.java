package com.example.demo.repository;

import com.example.demo.model.HyInfo;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

// HyInfoRepository.java
public interface HyInfoRepository extends JpaRepository<HyInfo, Integer> {
    // 模糊查询支持[1](@ref)
    Page<HyInfo> findByHyNameContaining(String keyword, Pageable pageable);
}