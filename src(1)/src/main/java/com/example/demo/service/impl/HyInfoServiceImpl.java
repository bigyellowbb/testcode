package com.example.demo.service.impl;

import com.example.demo.model.HyInfo;
import com.example.demo.repository.HyInfoRepository;
import com.example.demo.service.HyInfoService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class HyInfoServiceImpl implements HyInfoService {

    private final HyInfoRepository hyInfoRepository;

    public HyInfoServiceImpl(HyInfoRepository hyInfoRepository) {
        this.hyInfoRepository = hyInfoRepository;
    }

    @Override
    public HyInfo save(HyInfo hyInfo) {
        return hyInfoRepository.save(hyInfo);
    }

    @Override
    @Transactional // 添加事务注解
    public List<HyInfo> saveAll(List<HyInfo> hyInfos) {
        return hyInfoRepository.saveAll(hyInfos);
    }

    @Override
    @Transactional(readOnly = true) // 只读事务优化查询
    public List<HyInfo> findAll() {
        return hyInfoRepository.findAll();
    }
    @Override
    public Page<HyInfo> findPage(Pageable pageable) {
        return hyInfoRepository.findAll(pageable);
    }
    @Override
    public void delete(Integer id) {
        hyInfoRepository.deleteById(id);
    }
    // HyInfoServiceImpl.java
    @Override
    public Page<HyInfo> findByHyNameContaining(String hyName, Pageable pageable) {
        if (hyName == null || hyName.isEmpty()) {
            return hyInfoRepository.findAll(pageable);
        } else {
            return hyInfoRepository.findByHyNameContaining(hyName, pageable);
        }
    }
}