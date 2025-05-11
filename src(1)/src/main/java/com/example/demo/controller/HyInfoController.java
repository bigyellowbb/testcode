package com.example.demo.controller;

import com.example.demo.model.HyInfo;
import com.example.demo.service.HyInfoService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/hyinfo")
public class HyInfoController {

    private final HyInfoService hyInfoService;

    public HyInfoController(HyInfoService hyInfoService) {
        this.hyInfoService = hyInfoService;
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> create(@RequestBody HyInfo hyInfo) {
        HyInfo saved = hyInfoService.save(hyInfo);
        return ResponseEntity.status(HttpStatus.CREATED).body(createResponse(saved));
    }

    private Map<String, Object> createResponse(HyInfo data) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", data);
        return response;
    }

@GetMapping
public ResponseEntity<Map<String, Object>> getHyInfo(
        @RequestParam(defaultValue = "1") int currentPage, // 前端传递的是1-based页码
        @RequestParam(defaultValue = "10") int pageSize,
        @RequestParam(required = false) String hyName // 新增搜索参数
) {
    // 转换为0-based页码
    PageRequest pageable = PageRequest.of(currentPage - 1, pageSize);
    Page<HyInfo> pageResult = hyInfoService.findByHyNameContaining(hyName, pageable);

    // 构建标准分页响应[6,7](@ref)
    Map<String, Object> response = new HashMap<>();
    response.put("success", true);
    response.put("data", pageResult.getContent());
    response.put("currentPage", currentPage);
    response.put("totalPages", pageResult.getTotalPages()); // 补充总页数
    response.put("totalItems", pageResult.getTotalElements());
    return ResponseEntity.ok(response);
}

    @PostMapping("/upload")
    public ResponseEntity<Map<String, Object>> uploadFile(@RequestParam("file") MultipartFile file) {
        Map<String, Object> response = new HashMap<>();
        try {
            // 1. 读取文件内容
            List<HyInfo> dataList = parseFileToHyInfo(file);

            // 2. 批量保存到数据库
            hyInfoService.saveAll(dataList); // 需在 Service 层添加此方法

            response.put("success", true);
            response.put("message", "成功插入 " + dataList.size() + " 条数据");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "文件处理失败: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }
    // 新增更新和删除接口
    @PutMapping("/{id}")
    public ResponseEntity<Map<String, Object>> update(@PathVariable Integer id, @RequestBody HyInfo hyInfo) {
        hyInfo.setId(id);
        HyInfo updated = hyInfoService.save(hyInfo);
        return ResponseEntity.ok(createResponse(updated));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> delete(@PathVariable Integer id) {
        hyInfoService.delete(id);
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        return ResponseEntity.ok(response);
    }

    // 文件解析方法
    private List<HyInfo> parseFileToHyInfo(MultipartFile file) throws Exception {
        List<HyInfo> dataList = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream()))) {
            String line;
            boolean isFirstLine = true;

            while ((line = reader.readLine()) != null) {
                // 跳过表头
                if (isFirstLine) {
                    isFirstLine = false;
                    continue;
                }

                // 解析 CSV 行数据（假设列顺序与字段对应）
                String[] values = line.split(",");
                if (values.length < 10) {
                    throw new IllegalArgumentException("CSV 文件列数不足");
                }

                HyInfo info = new HyInfo();
                info.setHyName(values[0].trim());
                info.setWaterLevel(Double.parseDouble(values[1].trim()));
                info.setSandContent(Double.parseDouble(values[2].trim()));
                info.setDewPoint(Double.parseDouble(values[3].trim()));
                info.setPrecipitation(Double.parseDouble(values[4].trim()));
                info.setSeaPressure(Double.parseDouble(values[5].trim()));
                info.setTemperature(Double.parseDouble(values[6].trim()));
                info.setVisibility(Double.parseDouble(values[7].trim()));
                info.setWindSpeed(Double.parseDouble(values[8].trim()));
                info.setInternetTraffic(Double.parseDouble(values[9].trim()));

                dataList.add(info);
            }
        }
        return dataList;
    }
    // Double 解析
    private Double parseDoubleSafe(String value) {
        if (value == null || value.trim().isEmpty()) return 0.0;
        try {
            return Double.parseDouble(value.trim());
        } catch (NumberFormatException e) {
            return 0.0; // 或记录日志
        }
    }
}