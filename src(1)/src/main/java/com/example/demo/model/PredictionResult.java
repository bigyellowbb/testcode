// src/main/java/com/example/demo/model/PredictionResult.java
package com.example.demo.model;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import java.time.Instant;

@Entity
@Data
@Table(name = "prediction_result")
public class PredictionResult {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    private PredictionType type;

    @Column(name = "predicted_value")
    private Double predictedValue;

    @CreationTimestamp
    @Column(name = "create_time", updatable = false)
    private Instant createTime;

    public enum PredictionType {
        TRAIN, TEST
    }
}