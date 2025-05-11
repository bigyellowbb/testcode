package com.example.demo.service;

import com.example.demo.model.PreData;

import java.util.List;

public interface PreDataService {
    List<PreData> findAll();
    void saveAll(List<PreData> dataList);
    void deleteAll();
    void truncateTable(); // 新增方法
}