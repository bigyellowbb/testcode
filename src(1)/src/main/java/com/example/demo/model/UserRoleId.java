package com.example.demo.model;

import jakarta.persistence.Embeddable;
import lombok.Data;
import java.io.Serializable;

@Embeddable  // 表示这是一个可嵌入的复合主键类
@Data
public class UserRoleId implements Serializable { // 必须实现 Serializable
    private Long userId;
    private Long roleId;
}