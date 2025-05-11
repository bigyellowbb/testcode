package com.example.demo.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@Entity
@NoArgsConstructor // 添加无参构造器
@Table(name = "prediction_data") // 明确指定表名
public class PreData {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "water_level", nullable = true) // 明确允许空值
    @JsonProperty("water_level")
    private Double waterLevel;

    @Column(name = "sand_content", nullable = true)
    @JsonProperty("sand_content")
    private Double sandContent;

    @Column(name = "dew_point", nullable = true)
    @JsonProperty("dew_point")
    private Double dewPoint;

    @Column(name = "internet_traffic", nullable = true)
    @JsonProperty("internet_traffic")
    private Double internetTraffic;

}
