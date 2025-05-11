package com.example.demo.service;

import com.example.demo.model.HyInfo;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface HyInfoService {
    HyInfo save(HyInfo hyInfo);
    List<HyInfo> saveAll(List<HyInfo> hyInfos); // 新增批量保存
    List<HyInfo> findAll();
    Page<HyInfo> findPage(Pageable pageable);
    void delete(Integer id);
    // HyInfoService.java
    Page<HyInfo> findByHyNameContaining(String hyName, Pageable pageable);
}