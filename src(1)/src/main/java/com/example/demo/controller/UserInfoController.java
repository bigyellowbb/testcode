package com.example.demo.controller;

import com.example.demo.config.JwtUtils;
import com.example.demo.model.Role;
import com.example.demo.model.UserInfo;
import com.example.demo.model.UserRole;
import com.example.demo.repository.RoleRepository;
import com.example.demo.repository.UserInfoRepository;
import com.example.demo.repository.UserRoleRepository;
import io.jsonwebtoken.Claims;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.Authentication;  // 核心Authentication接口
import org.springframework.security.core.context.SecurityContextHolder; // 安全上下文
//import  org.springframework.web.bind.annotation.RequestBody;
//import  org.springframework.web.bind.annotation.PostMapping;
//import  org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;


@RestController
@CrossOrigin("*")
public class UserInfoController {

    @Autowired
    private UserInfoRepository userInfoRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtils jwtUtils;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private UserRoleRepository userRoleRepository;

    // 注册接口
    @PostMapping("/register")
    public ResponseEntity<Map<String,Object>> register(@RequestBody UserInfo userInfo) {
        Map<String,Object> response = new HashMap<>();
        if (userInfoRepository.existsByUserName(userInfo.getUserName())) {
            response.put("code", 400);
            response.put("message", "该用户名已存在");
            return ResponseEntity.badRequest().body(response);
        }

        if (userInfoRepository.existsByUserEmail(userInfo.getUserEmail())) {
            response.put("code", 400);
            response.put("message", "此邮箱已存在");
            return ResponseEntity.badRequest().body(response);
        }

        userInfo.setUserPassword(passwordEncoder.encode(userInfo.getUserPassword()));

        try {
            userInfoRepository.save(userInfo);
            response.put("code", 200);
            response.put("message", "注册成功");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("code", 500);
            response.put("message", "出错了");
            return ResponseEntity.internalServerError().body(response);
        }
    }

    // 登录接口
    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(@RequestBody UserInfo loginRequest) {
        Map<String, Object> response = new HashMap<>();

        Optional<UserInfo> userOpt = userInfoRepository.findByUserName(loginRequest.getUserName());
        if (userOpt.isEmpty() || !passwordEncoder.matches(loginRequest.getUserPassword(), userOpt.get().getUserPassword())) {
            response.put("code", 401);
            response.put("message", "用户名或密码错误");
            return ResponseEntity.status(401).body(response);
        }

        UserInfo user = userOpt.get();
        String token = jwtUtils.generateToken(user.getUserName());

        Map<String, Object> data = new HashMap<>();
        data.put("id", user.getId());
        data.put("username", user.getUserName());
        data.put("email", user.getUserEmail());
        data.put("phone", user.getPhone() != null ? user.getPhone() : "");
        data.put("role", getRoleForUser(user.getId()));
        data.put("token", token);

        response.put("code", 200);
        response.put("data", data);

        return ResponseEntity.ok()
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .body(response);
    }

    @GetMapping("/current-user")
    public ResponseEntity<Map<String, Object>> getCurrentUser(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authHeader) {

        // JWT验证优先
        // 严格验证请求头格式
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(401).body(Map.of(
                    "code", 401,
                    "message", "Authorization header缺失或格式错误"
            ));
        }

        String token = authHeader.substring(7);
        try {
            if (!jwtUtils.validateToken(token)) {
                return ResponseEntity.status(401).body(Map.of(
                        "code", 401,
                        "message", "令牌无效或已过期"
                ));
            }

            Claims claims = jwtUtils.parseToken(token);
            return buildUserResponse(claims.getSubject());
        } catch (Exception e) {
            return ResponseEntity.status(401).body(Map.of(
                    "code", 401,
                    "message", "令牌解析失败: " + e.getMessage()
            ));
        }
    }

//        if (authHeader != null && authHeader.startsWith("Bearer ")) {
//            String token = authHeader.substring(7);
//            if (jwtUtils.validateToken(token)) {
//                Claims claims = jwtUtils.parseToken(token);
//                return buildUserResponse(claims.getSubject());
//            }
//        }
//
//        // 备用Session验证
//        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
//        return buildUserResponse(authentication.getName());
//    }
//
//    private ResponseEntity<Map<String, Object>> buildUserResponse(String username) {
//        Optional<UserInfo> userOpt = userInfoRepository.findByUserName(username);
//        if (userOpt.isEmpty()) {
//            return ResponseEntity.status(404).body(Map.of("code", 404, "message", "用户不存在"));
//        }
//
//        UserInfo user = userOpt.get();
//        Map<String, Object> data = new HashMap<>();
//        data.put("id", user.getId());
//        data.put("username", user.getUserName());
//        data.put("email", user.getUserEmail());
//        data.put("phone", user.getPhone() != null ? user.getPhone() : "");
//        data.put("role", getRoleForUser(user.getId()));
//
//        return ResponseEntity.ok(Map.of("code", 200, "data", data));
//    }

    @GetMapping("/users")
    public ResponseEntity<List<Map<String, Object>>> getAllUsers() {
        List<UserInfo> users = userInfoRepository.findAll();

        List<Map<String, Object>> result = users.stream()
                .map(user -> {
                    Map<String, Object> userMap = new HashMap<>();
                    userMap.put("id", user.getId());
                    userMap.put("username", user.getUserName());
                    userMap.put("role", getRoleForUser(user.getId()));
                    return userMap;
                })
                .collect(Collectors.toList());

        return ResponseEntity.ok(result);
    }

    // 关键修改点4：增强权限检查
    @PutMapping("/users/{userId}/role")
    public ResponseEntity<?> updateUserRole(
            @PathVariable Long userId,
            @RequestBody Map<String, String> request,
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authHeader) {

        try {
            String token = authHeader.replace("Bearer ", "");
            Claims claims = jwtUtils.parseToken(token);

            UserInfo currentUser = userInfoRepository.findByUserName(claims.getSubject())
                    .orElseThrow(() -> new UsernameNotFoundException("用户不存在"));

            // 严格角色格式验证
            if (!getRoleForUser(currentUser.getId()).equals("ROLE_ADMIN")) {
                return ResponseEntity.status(403).body(Map.of(
                        "code", 403,
                        "message", "权限不足，需要管理员权限"
                ));
            }

            String newRole = request.get("role");
            Role role = roleRepository.findByRoleName(newRole)
                    .orElseThrow(() -> new RuntimeException("角色不存在: " + newRole));

            UserInfo user = userInfoRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("目标用户不存在"));

            // 使用事务操作
            userRoleRepository.deleteByUserId(userId);
            UserRole userRole = new UserRole();
            userRole.setUser(user);
            userRole.setRole(role);
            userRoleRepository.save(userRole);

            return ResponseEntity.ok(Map.of(
                    "code", 200,
                    "message", "角色更新成功"
            ));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of(
                    "code", 500,
                    "message", "服务器错误: " + e.getMessage()
            ));
        }
    }

//    @PutMapping("/users/{userId}/role")
//    public ResponseEntity<?> updateUserRole(
//            @PathVariable Long userId,
//            @RequestBody Map<String, String> request,
//            @RequestHeader(HttpHeaders.AUTHORIZATION) String authHeader) {
//
//        String token = authHeader.replace("Bearer ", "");
//        Claims claims = jwtUtils.parseToken(token);
//        String username = claims.getSubject();
//
//        UserInfo currentUser = userInfoRepository.findByUserName(username)
//                .orElseThrow(() -> new UsernameNotFoundException("用户不存在"));
//
//        if (!getRoleForUser(currentUser.getId()).equals("ROLE_ADMIN")) {
//            return ResponseEntity.status(403).body(Map.of("code", 403, "message", "权限不足"));
//        }
//
//        String newRole = request.get("role");
//        Role role = roleRepository.findByRoleName(newRole)
//                .orElseThrow(() -> new RuntimeException("角色不存在"));
//
//        UserInfo user = userInfoRepository.findById(userId)
//                .orElseThrow(() -> new RuntimeException("用户不存在"));
//
//        userRoleRepository.deleteByUserId(userId);
//
//        UserRole userRole = new UserRole();
//        userRole.setUser(user);
//        userRole.setRole(role);
//        userRoleRepository.save(userRole);
//
//        return ResponseEntity.ok(Map.of("code", 200, "message", "角色更新成功"));
//    }

    // 关键修改点2：增强角色验证逻辑
    private String getRoleForUser(Long userId) {
        UserInfo user = userInfoRepository.findById(userId)
                .orElseThrow(() -> new UsernameNotFoundException("用户不存在"));

        List<UserRole> userRoles = userRoleRepository.findByUser(user);
        if (!userRoles.isEmpty()) {
            String rawRole = userRoles.get(0).getRole().getRoleName().toUpperCase();
            return rawRole.startsWith("ROLE_") ? rawRole : "ROLE_" + rawRole;
        }
        return "ROLE_USER";
    }

//    private String getRoleForUser(Long userId) {
//        UserInfo user = userInfoRepository.findById(userId).orElseThrow();
//        List<UserRole> userRoles = userRoleRepository.findByUser(user);
//        if (!userRoles.isEmpty()) {
//            return "ROLE_" + userRoles.get(0).getRole().getRoleName().toUpperCase();
//        }
//        return "ROLE_USER";
//    }
// 关键修改点3：统一用户数据构建方法
private ResponseEntity<Map<String, Object>> buildUserResponse(String username) {
    UserInfo user = userInfoRepository.findByUserName(username)
            .orElseThrow(() -> new UsernameNotFoundException("用户不存在"));

    Map<String, Object> data = new HashMap<>();
    data.put("id", user.getId());
    data.put("username", user.getUserName());
    data.put("email", user.getUserEmail());
    data.put("phone", user.getPhone() != null ? user.getPhone() : "");
    data.put("role", getRoleForUser(user.getId()));

    return ResponseEntity.ok(Map.of(
            "code", 200,
            "data", data
    ));
}


}

//@RestController
//@CrossOrigin("*")
//public class UserInfoController {
//
//    @Autowired
//    private UserInfoRepository userInfoRepository;
//
//    @Autowired
//    private PasswordEncoder passwordEncoder;
//
//    @Autowired
//    private JwtUtils jwtUtils; // 新增JWT工具注入
//
//    @Autowired
//    private RoleRepository roleRepository;
//
//    @Autowired
//    private UserRoleRepository userRoleRepository;
//
//    // 注册接口
//    @PostMapping("/register")
//    public ResponseEntity<Map<String,Object>> register(@RequestBody UserInfo userInfo){
//        Map<String,Object> response =new HashMap<>();
//        // 验证用户名
//        if (userInfoRepository.existsByUserName(userInfo.getUserName())) {
//            response.put("code", 400);
//            response.put("message", "该用户名已存在");
//            return ResponseEntity.badRequest().body(response);
//        }
//
//        // 验证邮箱
//        if (userInfoRepository.existsByUserEmail(userInfo.getUserEmail())) {
//            response.put("code", 400);
//            response.put("message", "此邮箱已存在");
//            return ResponseEntity.badRequest().body(response);
//        }
//
//        // 密码加密
//        userInfo.setUserPassword(passwordEncoder.encode(userInfo.getUserPassword()));
//
//        try {
//            userInfoRepository.save(userInfo);
//            response.put("code", 200);
//            response.put("message", "注册成功");
//            return ResponseEntity.ok(response);
//        } catch (Exception e) {
//            response.put("code", 500);
//            response.put("message", "出错了");
//            return ResponseEntity.internalServerError().body(response);
//        }
//    }
//
//    // 登录接口
//    @PostMapping("/login")
//    public ResponseEntity<Map<String, Object>> login(@RequestBody UserInfo loginRequest) {
//        Map<String, Object> response = new HashMap<>();
//
//        Optional<UserInfo> userOpt = userInfoRepository.findByUserName(loginRequest.getUserName());
//        if (userOpt.isEmpty() || !passwordEncoder.matches(loginRequest.getUserPassword(), userOpt.get().getUserPassword())) {
//            response.put("code", 401);
//            response.put("message", "用户名或密码错误");
//            return ResponseEntity.status(401).body(response);
//        }
//
//
//
//        UserInfo user = userOpt.get();
//        String token = jwtUtils.generateToken(user.getUserName()); // 生成JWT令牌
//
//        Map<String, Object> data = new HashMap<>();
//        data.put("id", user.getId());
//        data.put("username", user.getUserName());
//        data.put("email", user.getUserEmail());
//        data.put("phone", user.getPhone() != null ? user.getPhone() : ""); // 处理null值
//        data.put("role", getRoleForUser(user.getId()));
//        data.put("token", token); // 在响应体中返回token
//
//        response.put("code", 200);
//        response.put("data", data);
//
////        return ResponseEntity.ok(response);
//        // 在响应头中同时设置Authorization
//        return ResponseEntity.ok()
//                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
//                .body(response);
//    }
//    }
//
//    @GetMapping("/current-user")
//    public ResponseEntity<Map<String, Object>> getCurrentUser() {
//        @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authHeader) {
//
//            // JWT验证优先
//            if (authHeader != null && authHeader.startsWith("Bearer ")) {
//                String token = authHeader.substring(7);
//                if (jwtUtils.validateToken(token)) {
//                    Claims claims = jwtUtils.parseToken(token);
//                    return buildUserResponse(claims.getSubject());
//                }
//            }
//
//            // 备用Session验证
//            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
//            return buildUserResponse(authentication.getName());
//        }
////        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
////        String username = authentication.getName();
//        private ResponseEntity<Map<String, Object>> buildUserResponse(String username) {
//        Optional<UserInfo> userOpt = userInfoRepository.findByUserName(username);
//        if (userOpt.isEmpty()) {
//            return ResponseEntity.status(404).body(Map.of("code", 404, "message", "用户不存在"));
//        }
//
//        UserInfo user = userOpt.get();
//        Map<String, Object> response = new HashMap<>();
//        response.put("id", user.getId());
//        response.put("username", user.getUserName());
//        response.put("email", user.getUserEmail());
//        response.put("phone", user.getPhone() != null ? user.getPhone() : "");
//        response.put("role", getRoleForUser(user.getId()));
//
//        return ResponseEntity.ok(Map.of("code", 200, "data", response));
//    }
//        // 修改更新角色接口
//        @PutMapping("/users/{userId}/role")
//        public ResponseEntity<?> updateUserRole(@PathVariable Long userId,
//                @RequestBody Map<String, String> request,
//                Authentication authentication) {
//
//            // JWT验证
//            String token = authHeader.replace("Bearer ", "");
//            Claims claims = jwtUtils.parseToken(token);
//            String username = claims.getSubject();
//
//
//            // 检查当前用户是否是管理员
//            UserInfo currentUser = userInfoRepository.findByUserName(authentication.getName())
//                    .orElseThrow(() -> new UsernameNotFoundException("用户不存在"));
//
//            if (!getRoleForUser(currentUser.getId()).equals("admin")) {
//                return ResponseEntity.status(403).body(Map.of("code", 403, "message", "权限不足"));
//            }
//
//            String newRole = request.get("role");
//            Role role = roleRepository.findByRoleName(newRole)
//                    .orElseThrow(() -> new RuntimeException("角色不存在"));
//
//            // 更新用户角色
//            UserInfo user = userInfoRepository.findById(userId)
//                    .orElseThrow(() -> new RuntimeException("用户不存在"));
//
//            // 删除原有角色
//            userRoleRepository.deleteByUserId(userId);
//
//            // 添加新角色
//            UserRole userRole = new UserRole();
//            userRole.setUser(user);
//            userRole.setRole(role);
//            userRoleRepository.save(userRole);
//
//            return ResponseEntity.ok(Map.of("code", 200, "message", "角色更新成功"));
//        }
//
//
//
//    // 修改获取角色方法
//    private String getRoleForUser(Long userId) {
//        UserInfo user = userInfoRepository.findById(userId).orElseThrow();
//        List<UserRole> userRoles = userRoleRepository.findByUser(user);
//        if (!userRoles.isEmpty()) {
////            return userRoles.get(0).getRole().getRoleName();
////            return userRoles.get(0).getRole().getRoleName().toUpperCase(); // 确保返回大写角色名称
//            return "ROLE_" + userRoles.get(0).getRole().getRoleName().toUpperCase();
//        }
////        return "user";
//        return "ROLE_USER"; // 默认角色
//    }
//
//
//    @GetMapping("/users")
//    public ResponseEntity<List<Map<String, Object>>> getAllUsers() {
//        List<UserInfo> users = userInfoRepository.findAll();
//
//        List<Map<String, Object>> result = users.stream()
//                .map(user -> {
//                    Map<String, Object> userMap = new HashMap<>();
//                    userMap.put("id", user.getId());
//                    userMap.put("username", user.getUserName());
//                    userMap.put("role", getRoleForUser(user.getId()));
//                    return userMap;
//                })
//                .collect(Collectors.toList());
//
//        return ResponseEntity.ok(result);
//    }
//
//
//}
