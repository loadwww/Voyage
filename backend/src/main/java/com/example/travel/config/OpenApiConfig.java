package com.example.travel.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {
    @Bean
    public OpenAPI travelOpenApi() {
        return new OpenAPI().info(new Info()
                .title("Voyage 智能旅行规划 API")
                .version("2.0")
                .description("需求解析、景点召回排序、Floyd + TSP DP 行程规划、地图导航与旅游日记接口")
                .license(new License().name("MIT")));
    }
}
