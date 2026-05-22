package com.example.demo.config.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Configuration
public class PortfolioSecurityConfiguration {

	@Value("${portfolio.token:UNSET}")
    private String portfolioToken;
	
	@PostConstruct
    public void validateStartup() {
        
        if ("UNSET".equals(portfolioToken)) {
        	log.error("啟動失敗：未提供 Portfolio Token，系統拒絕啟動！");    
        	// 拋出 Exception，讓 Spring Boot 執行優雅停機
            throw new IllegalStateException("未經授權的執行環境");
        }
    }
}
