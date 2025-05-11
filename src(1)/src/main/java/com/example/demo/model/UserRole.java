package com.example.demo.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.Data;
import java.io.Serializable;


@Entity
@Table(name = "user_role")
@Data
public class UserRole {
    @EmbeddedId
    private UserRoleId id; // 组合主键

    @ManyToOne
    @MapsId("userId")      // 映射复合主键中的 userId 字段
    @JoinColumn(name = "user_id")
    private UserInfo user;

    @ManyToOne
    @MapsId("roleId")      // 映射复合主键中的 roleId 字段
    @JoinColumn(name = "role_id")
    private Role role;
}