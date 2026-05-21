package com.example.demo.iface.dto.req;

import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 知識庫建立請求載體 (Web 層專用)
 */
public record CreateKnowledgeResource(
		@NotBlank(message = "標題不能為空") @Size(max = 255, message = "標題長度不能超過 255 字元") String title,

		@NotBlank(message = "分類不能為空") String category,

		@NotBlank(message = "內文不能為空") String content,

		String author,

		List<String> tags) {
}