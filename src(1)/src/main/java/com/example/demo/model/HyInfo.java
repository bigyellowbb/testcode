package com.example.demo.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "hyinfo")
public class HyInfo {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "hy_name")
    @JsonProperty("hy_name")
    private String hyName;

    @Column(name = "water_level")
    @JsonProperty("water_level")
    private Double waterLevel;

    @Column(name = "sand_content")
    @JsonProperty("sand_content")
    private Double sandContent;

    @Column(name = "dew_point")
    @JsonProperty("dew_point")
    private Double dewPoint;

    @Column(name = "precipitation")
    @JsonProperty("precipitation")
    private Double precipitation;

    @Column(name = "sea_pressure")
    @JsonProperty("sea_pressure")
    private Double seaPressure;

    @Column(name = "temperature")
    @JsonProperty("temperature")
    private Double temperature;

    @Column(name = "visibility")
    @JsonProperty("visibility")
    private Double visibility;

    @Column(name = "wind_speed")
    @JsonProperty("wind_speed")
    private Double windSpeed;

    @Column(name = "internet_traffic")
    @JsonProperty("internet_traffic")
    private Double internetTraffic;
}