package com.example.demo.config.init;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import com.example.demo.application.domain.checkpoint.aggregate.SystemCheckpoint;
import com.example.demo.infra.repository.SystemCheckpointRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class PortfolioSecurityValidator implements ApplicationRunner {

	private final SystemCheckpointRepository checkpointRepository;

	@Value("${portfolio.token:UNSET}")
	private String portfolioToken;

	@Override
	public void run(ApplicationArguments args) {
		// 1. 若環境變數根本沒設，直接擋下
		if ("UNSET".equals(portfolioToken)) {
			abortStartup("未提供 Portfolio Token (環境變數缺失)");
		}

		// 2. 從資料庫撈取預期的授權碼
		String expectedToken = checkpointRepository.findById("PORTFOLIO_LICENSE").map(SystemCheckpoint::getConfigValue)
				.orElse(null);

		// 3. 防禦：如果資料庫根本沒這筆資料 (代表是被別人 clone 去跑的新資料庫)
		if (expectedToken == null) {
			abortStartup("系統未經初始化，找不到有效授權憑證");
		}

		// 4. 比對授權碼
		if (!expectedToken.equals(portfolioToken)) {
			abortStartup("Portfolio Token 驗證失敗，憑證不符");
		}

		log.info("[Security] 系統授權驗證成功，RAG 智能大腦已啟動。");
	}

	private void abortStartup(String reason) {
		log.error("啟動終止：{}", reason);
		throw new IllegalStateException("啟動失敗，請確認啟動環境配置。");
	}
}