package com.example.demo.iface.rest;

import java.util.List;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.application.service.RagAssistantQueryService;
import com.example.demo.application.service.RagAssistantQueryService.RagResponse;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;

@Slf4j
@RestController
@RequestMapping("/api/v1/assistant")
@RequiredArgsConstructor
public class RagAssistantController {

	private final RagAssistantQueryService ragService;

	/**
	 * AI 知識問答 API
	 * 
	 * @param q 使用者的自然語言問題
	 */
	@GetMapping("/ask")
	public ResponseEntity<RagResponse> askAssistant(@RequestParam(name = "q") String question,
			@RequestParam List<String> tags) {
		if (question == null || question.trim().isEmpty()) {
			return ResponseEntity.badRequest().build();
		}

		RagResponse response = ragService.askQuestion(question, tags);
		return ResponseEntity.ok(response);
	}

	/**
	 * AI 串流問答 API (SSE) 前端可直接使用 EventSource 或 fetch API 接收
	 */
	@GetMapping(value = "/ask-stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
	public Flux<String> askStreaming(@RequestParam(name = "q") String question, @RequestParam List<String> tags) {
		return ragService.askQuestionStreaming(question, tags).onBackpressureBuffer() // 緩衝處理，防止前端接收過慢
				.doOnError(e -> log.error("串流輸出發生異常", e));
	}
}