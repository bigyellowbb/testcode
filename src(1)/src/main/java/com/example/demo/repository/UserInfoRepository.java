package com.example.demo.repository;

import com.example.demo.model.UserInfo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserInfoRepository extends JpaRepository<UserInfo, Long> {
    // 注意方法名使用userName（驼峰命名）
    Optional<UserInfo> findByUserName(String userName);
    boolean existsByUserName(String userName);
    boolean existsByUserEmail(String userEmail);

}