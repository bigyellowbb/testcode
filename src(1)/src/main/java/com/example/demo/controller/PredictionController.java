package com.example.demo.controller;

import com.example.demo.model.PredictionResult;
import com.example.demo.service.PredictionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/predict")
public class PredictionController {

    private final PredictionService predictionService;

    public PredictionController(PredictionService predictionService) {
        this.predictionService = predictionService;
    }
    @PostMapping("/lstm")
    public ResponseEntity<Map<String, Object>> triggerPrediction() {
        List<Map<String, Double>> dbData = predictionService.getPredictionDataFromDB();

        // 检查数据是否为空
        if (dbData.isEmpty()) {
            return ResponseEntity.badRequest().body(Collections.singletonMap("error", "无有效数据"));
        }

        // 提取数据
        List<Double> sampleData = dbData.stream()
                .flatMap(data -> {
                    List<Double> values = new ArrayList<>();
                    values.add(data.get("water_level"));
                    values.add(data.get("sand_content"));
                    values.add(data.get("dew_point"));
                    values.add(data.get("internet_traffic"));
                    return values.stream();
                })
                .collect(Collectors.toList());

        // 调用预测服务
        predictionService.trainAndPredict(sampleData);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "预测任务已启动");
        response.put("data_count", dbData.size());
        return ResponseEntity.ok(response);
    }

//    @PostMapping("/lstm")
//    public ResponseEntity<Map<String, Object>> triggerPrediction() {
//        List<Map<String, Double>> dbData = predictionService.getPredictionDataFromDB();
//
//        // 添加空值检查
//        List<Double> sampleData = dbData.stream()
//                .flatMap(data -> {
//                    List<Double> values = new ArrayList<>(4);
//                    values.add(Objects.requireNonNullElse(data.get("water_level"), 0.0));
//                    values.add(Objects.requireNonNullElse(data.get("sand_content"), 0.0));
//                    values.add(Objects.requireNonNullElse(data.get("dew_point"), 0.0));
//                    values.add(Objects.requireNonNullElse(data.get("internet_traffic"), 0.0));
//                    return values.stream();
//                })
//                .collect(Collectors.toList());
//
//        predictionService.trainAndPredict(sampleData);
//
//        Map<String, Object> response = new HashMap<>();
//        response.put("success", !sampleData.isEmpty());
//        response.put("message", sampleData.isEmpty() ? "无有效数据" : "预测任务已启动");
//        response.put("data_count", dbData.size());
//        return ResponseEntity.ok(response);
//    }

//    // PredictionController.java 新增
//    @GetMapping("/results")
//    public ResponseEntity<Map<String, Object>> getPredictionResults(
//            @RequestParam(required = false) String type
//    ) {
//        List<PredictionResult> results;
//
//        if (type != null) {
//            results = predictionResultRepository.findByType(
//                    PredictionResult.PredictionType.valueOf(type.toUpperCase())
//            );
//        } else {
//            results = predictionResultRepository.findAll();
//        }
//
//        Map<String, Object> response = new HashMap<>();
//        response.put("success", true);
//        response.put("results", results.stream()
//                .map(r -> Map.of(
//                        "type", r.getType().name(),
//                        "value", r.getPredictedValue(),
//                        "time", r.getCreateTime().toString()
//                ))
//                .collect(Collectors.toList()));
//        return ResponseEntity.ok(response);
//    }



}

//    @PostMapping("/lstm")
//    public ResponseEntity<Map<String, Object>> triggerPrediction() {
//        // 从数据库获取数据
//        List<Map<String, Double>> dbData = predictionService.getPredictionDataFromDB();
//
//        // 将数据转换为适合LSTM模型的格式
//        List<Double> sampleData = dbData.stream()
//                .flatMap(data -> List.of(
//                        data.get("water_level"),
//                        data.get("sand_content"),
//                        data.get("dew_point"),
//                        data.get("internet_traffic")
//                ).stream())
//                .collect(Collectors.toList());
//
//        predictionService.trainAndPredict(sampleData);
//
//        Map<String, Object> response = new HashMap<>();
//        response.put("success", true);
//        response.put("message", "预测任务已启动");
//        response.put("data_count", dbData.size());
//        return ResponseEntity.ok(response);
//    }
//}