package com.example.demo.controller;

import com.example.demo.model.PreData;
import com.example.demo.service.PreDataService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/predata")
public class PreDataController {

    private final PreDataService preDataService;

    public PreDataController(PreDataService preDataService) {
        this.preDataService = preDataService;
    }

    // 获取全量数据
    @GetMapping
    public ResponseEntity<Map<String, Object>> getAll() {
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", preDataService.findAll());
        return ResponseEntity.ok(response);
    }

    // 清空数据库表(使用TRUNCATE)
    @DeleteMapping("/clear")
    public ResponseEntity<Map<String, Object>> clearAll() {
        preDataService.truncateTable(); // 改为调用truncate方法
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "数据库表已清空，ID计数器已重置");
        return ResponseEntity.ok(response);
    }

//    // 清空数据库表
//    @DeleteMapping("/clear")
//    public ResponseEntity<Map<String, Object>> clearAll() {
//        preDataService.deleteAll();
//        Map<String, Object> response = new HashMap<>();
//        response.put("success", true);
//        response.put("message", "数据库表已清空");
//        return ResponseEntity.ok(response);
//    }

    // 文件批量导入
    @PostMapping("/upload")
    public ResponseEntity<Map<String, Object>> uploadFile(@RequestParam("file") MultipartFile file) {
        Map<String, Object> response = new HashMap<>();
        try {
            List<PreData> dataList = parseCsvFile(file);
            preDataService.saveAll(dataList);

            response.put("success", true);
            response.put("insertedCount", dataList.size());
            response.put("data", dataList);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("error", "文件处理失败: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    // CSV解析核心逻辑
    private List<PreData> parseCsvFile(MultipartFile file) throws Exception {
        List<PreData> dataList = new java.util.ArrayList<>();

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(file.getInputStream()))) {

            // 跳过CSV表头
            reader.readLine();

            String line;
            while ((line = reader.readLine()) != null) {
                String[] values = line.split(",");
                if (values.length < 4) {
                    throw new IllegalArgumentException("CSV列格式错误，需要4列数据");
                }

                PreData data = new PreData();
                data.setWaterLevel(parseDouble(values[0].trim()));
                data.setSandContent(parseDouble(values[1].trim()));
                data.setDewPoint(parseDouble(values[2].trim()));
                data.setInternetTraffic(parseDouble(values[3].trim()));

                dataList.add(data);
            }
        }
        return dataList;
    }

    // 安全数值转换
    private Double parseDouble(String value) {
        if (value == null || value.isEmpty()) return 0.0;
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }
}