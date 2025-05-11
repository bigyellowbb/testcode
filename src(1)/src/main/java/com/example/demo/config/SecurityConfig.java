package com.example.demo.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // 禁用CSRF保护
                .csrf(csrf -> csrf.disable())

                // 设置无状态会话
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // 启用CORS配置
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))

                // 配置权限规则：允许所有请求
                .authorizeHttpRequests(auth -> auth
                        .anyRequest().permitAll() // 所有请求都不需要认证
                );

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();

        // 允许的源
        config.setAllowedOrigins(Arrays.asList(
                "http://localhost:8080",
                "http://localhost:8081",
                "http://your-production-domain.com"
        ));

        // 允许的方法
        config.setAllowedMethods(Arrays.asList(
                "GET", "POST", "PUT", "DELETE", "OPTIONS"
        ));

        // 允许的头部
        config.setAllowedHeaders(List.of("*"));

        // 允许凭证
        config.setAllowCredentials(true);

        // 预检请求缓存时间
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}

//package com.example.demo.config;
//
//import com.example.demo.filter.JwtAuthenticationFilter;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//import org.springframework.http.HttpMethod;
//import org.springframework.security.config.http.SessionCreationPolicy;
//import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
//import org.springframework.security.crypto.password.PasswordEncoder;
//import org.springframework.security.config.annotation.web.builders.HttpSecurity;
//import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
//import org.springframework.security.web.SecurityFilterChain;
//import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
//import org.springframework.web.cors.CorsConfiguration;
//import org.springframework.web.cors.CorsConfigurationSource;
//import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
//import org.springframework.web.servlet.config.annotation.CorsRegistry;
//import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
//
//import java.util.Arrays;
//import java.util.List;
//
//@Configuration
//@EnableWebSecurity
//public class SecurityConfig {
//
//    private final JwtUtils jwtUtils;
//
//    public SecurityConfig(JwtUtils jwtUtils) {
//        this.jwtUtils = jwtUtils;
//    }
//
//    @Bean
//    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
//        http        // ███ 新增会话状态配置（关键修复）
//                .sessionManagement(session ->
//                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
////                .csrf(csrf -> csrf.disable())
////                .authorizeHttpRequests(auth -> auth
//                .csrf(csrf -> csrf.disable())
//                // 启用CORS配置（关键添加）
//                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
//                // 添加JWT过滤器（关键修改）
//                .addFilterBefore(new JwtAuthenticationFilter(jwtUtils), UsernamePasswordAuthenticationFilter.class)
//                .authorizeHttpRequests(auth -> auth
//                        // 按精确度从高到低排列
//                        .requestMatchers("/register", "/login").permitAll()
//                        .requestMatchers("/current-user").authenticated()
//                        .requestMatchers("/predata", "/predata/upload","/predata/clear").permitAll()
//                        .requestMatchers(HttpMethod.GET, "/predata/**").permitAll()
//                        .requestMatchers("/hyinfo", "/hyinfo/upload").permitAll()
//                        .requestMatchers("/user","/user/**").permitAll()
//                        // 特殊权限接口
//                        .requestMatchers("/hyinfo/*/vmd").hasRole("guest")
//                        .requestMatchers("/lstm/configure")
//
//                        .requestMatchers("/lstm/predict", "/lstm/predict/upload")
//                        .requestMatchers("/lstm/results").permitAll()
//
//                        // ███ 分离HTTP方法权限（细化控制）
//                        .requestMatchers(HttpMethod.PUT, "/hyinfo/**").hasAnyRole("admin", "editor")
//                        .requestMatchers(HttpMethod.DELETE, "/hyinfo/**").hasRole("admin")
//
//                        // 通用规则放最后
//                        .requestMatchers(HttpMethod.GET, "/hyinfo/**").permitAll()
//                        .requestMatchers(HttpMethod.PUT, "/hyinfo/**").permitAll()
//                        .requestMatchers(HttpMethod.DELETE, "/hyinfo/**").permitAll()
//                        .anyRequest().authenticated()
//                ).addFilterBefore(new JwtAuthenticationFilter(jwtUtils), UsernamePasswordAuthenticationFilter.class);
////                        .requestMatchers("/predata", "/predata/upload","/predata/clear").permitAll()
////                        .requestMatchers(HttpMethod.GET, "/predata/**").permitAll()
////                        .requestMatchers("/hyinfo", "/hyinfo/upload").permitAll()
////                        .requestMatchers("/user","/user/**","/current-user").permitAll()
////                        .requestMatchers(HttpMethod.GET, "/hyinfo/**").permitAll()
////                        .requestMatchers(HttpMethod.PUT, "/hyinfo/**").permitAll()
////                        .requestMatchers(HttpMethod.DELETE, "/hyinfo/**").permitAll()
////                        .requestMatchers("/register", "/login","/current-user").permitAll()
////                        // 修正路径匹配模式
////                        .requestMatchers("/hyinfo/*/vmd").hasAnyAuthority("ROLE_guest") // 使用单星号*
////                        .requestMatchers(HttpMethod.GET, "/hyinfo/**/decomposition").permitAll()
////                        .anyRequest().authenticated()
////                )
//
//        return http.build();
//    }
//    @Bean
//    public PasswordEncoder passwordEncoder() {
//        return new BCryptPasswordEncoder();
//    }
////     新增全局CORS配置
////    @Configuration
////    public class CorsConfig implements WebMvcConfigurer {
////        @Override
////        public void addCorsMappings(CorsRegistry registry) {
////            registry.addMapping("/**")
////                    .allowedOrigins("http://localhost:8081") // 前端实际地址
////                    .allowedMethods("GET", "POST", "PUT", "DELETE")
////                    .allowedHeaders("*");
////        }
////    }
//    // 新增CORS配置源
//    private CorsConfigurationSource corsConfigurationSource() {
//        CorsConfiguration config = new CorsConfiguration();
//
//        // ███ 新增OPTIONS方法支持（预检请求必需）
//        config.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
//
//        // ███ 增加预检缓存配置（性能优化）
//        config.setMaxAge(3600L);
//
//        config.setAllowCredentials(true);
//        // ███ 显式设置凭证许可（解决403核心问题）
//        config.setAllowCredentials(true);
//
//        // ███ 规范化的域名配置（防止localhost遗漏）
//        config.setAllowedOrigins(Arrays.asList(
//                "http://localhost:8080",
//                "http://localhost:8081",
//                "http://your-production-domain.com"
//        ));
////        config.setAllowedOrigins(Arrays.asList(
////                "http://localhost:8080", // Vue开发服务器默认端口
////                "http://localhost:8081"  // 你的实际前端地址
////        ));
//        config.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE"));
//        config.setAllowedHeaders(List.of("*"));
//
//        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
//        source.registerCorsConfiguration("/**", config);
//        return source;
//    }
////        config.addAllowedOrigin("http://localhost:8081"); // 前端实际运行地址
////        config.addAllowedHeader("*");
////        config.addAllowedMethod("*");
////
////        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
////        source.registerCorsConfiguration("/**", config);
////        return source;
////    }
//
//
//}