package com.example.demo.service.impl;

import com.example.demo.model.PreData;
import com.example.demo.repository.PreDataRepository;
import com.example.demo.service.PreDataService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.List;

@Service
public class PreDataServiceImpl implements PreDataService {

    private final PreDataRepository preDataRepository;

    @PersistenceContext
    private EntityManager entityManager; // 添加 EntityManager

    @Autowired
    public PreDataServiceImpl(PreDataRepository preDataRepository) {
        this.preDataRepository = preDataRepository;
    }

    @Override
    public List<PreData> findAll() {
        return preDataRepository.findAll();
    }

    @Override
    public void saveAll(List<PreData> dataList) {
        preDataRepository.saveAll(dataList);
    }

    @Override
    public void deleteAll() {
        preDataRepository.deleteAll();
    }

    @Override
    @Transactional
    public void truncateTable() {
        preDataRepository.truncateTable();
        entityManager.clear(); // 清除Hibernate缓存
    }
}