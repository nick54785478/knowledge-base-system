package com.example.demo.application.domain.checkpoint.aggregate;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Table(name = "system_checkpoint")
@NoArgsConstructor
public class SystemCheckpoint {

	@Id
	private String configKey; // 例如: "PORTFOLIO_LICENSE"

	private String configValue;
}
