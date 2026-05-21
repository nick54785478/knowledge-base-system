package com.example.demo.iface.rest;

import java.net.URI;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import com.example.demo.application.service.KnowledgeCommandService;
import com.example.demo.application.shared.command.ChangeDocumentCategoryCommand;
import com.example.demo.application.shared.command.CreateKnowledgeCommand;
import com.example.demo.application.shared.command.UpdateKnowledgeCommand;
import com.example.demo.iface.dto.req.ChangeCategoryResource;
import com.example.demo.iface.dto.req.CreateKnowledgeResource;
import com.example.demo.iface.dto.req.UpdateKnowledgeResource;
import com.example.demo.iface.dto.res.KnowledgeCreatedResource;
import com.example.demo.infra.mapper.KnowledgeMapper;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 知識庫命令控制器 (Driving Adapter / 外部適配器)
 * <p>
 * 扮演系統的「防腐閘口」。負責將外部的 HTTP 協議、JSON 結構與驗證邏輯， 轉換為純粹的應用層指令
 * (Command)，確保核心業務邏輯不受外部框架污染。
 * </p>
 *
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/knowledge-documents")
@RequiredArgsConstructor
public class KnowledgeCommandController {

	private final KnowledgeCommandService commandService;
	private final KnowledgeMapper mapper;

	/**
	 * 建立新的知識庫文檔
	 *
	 * @param resource 來自前端的 JSON 請求 (已透過 @Valid 進行基本格式驗證)
	 * @return HTTP 201 Created，並在 Header 回傳新資源的 Location，Body 回傳 UUID
	 */
	@PostMapping
	public ResponseEntity<KnowledgeCreatedResource> createDocument(
			@Valid @RequestBody CreateKnowledgeResource resource) {

		log.info("接收到建立文檔請求: {}", resource.title());

		// 防腐處理 (Anti-Corruption):
		CreateKnowledgeCommand command = mapper.transform(resource);

		// 呼叫應用層服務執行業務邏輯，Controller 不參與任何商業規則判斷
		String documentId = commandService.createDocument(command);

		/*
		 * URI 的意義 對後端的意義：這是一種優雅的放手。後端不用在 POST API
		 * 裡面把整篇文章的內容（包含關聯的標籤、作者細節）再回傳一次。後端只說：「建好了，地址在這。」
		 * 
		 * 對前端的意義：前端拿到了這個 Location
		 * Header，它不需要自己去「瞎猜」或「拼湊」後續查詢的網址。如果前端在建立文章後，需要立刻跳轉到「文章預覽頁」，它可以直接拿這個 URI 去發送 GET
		 * 請求（也就是打去你的 Elasticsearch Query 端）把資料拉出來。這達成了前後端的極度解耦。
		 */
		URI location = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}").buildAndExpand(documentId)
				.toUri();

		return ResponseEntity.created(location).body(new KnowledgeCreatedResource("201", "新增成功", documentId));
	}

	/**
	 * 修改知識庫文檔
	 */
	@PutMapping("/{id}")
	public ResponseEntity<KnowledgeCreatedResource> updateDocument(@PathVariable String id,
			@Valid @RequestBody UpdateKnowledgeResource resource) {

		log.info("接收到更新文檔請求，ID: {}", id);

		UpdateKnowledgeCommand command = mapper.transform(resource);
		commandService.updateDocument(id, command);

		return ResponseEntity.ok(new KnowledgeCreatedResource("200", "更新成功", id));
	}

	/**
	 * 刪除知識庫文檔
	 */
	@DeleteMapping("/{id}")
	public ResponseEntity<KnowledgeCreatedResource> deleteDocument(@PathVariable String id) {

		log.info("接收到刪除文檔請求，ID: {}", id);

		commandService.deleteDocument(id);

		// 刪除成功回傳 200 OK
		// (純 RESTful 派別可能會回傳 204 No Content 且不帶 Body，這裡配合信封模式)
		return ResponseEntity.ok(new KnowledgeCreatedResource("200", "刪除成功", id));
	}

	/**
	 * 變更知識庫文檔分類 (觸發向量資料庫 Metadata 非同步更新)
	 */
	@PatchMapping("/{id}/category")
	public ResponseEntity<KnowledgeCreatedResource> changeCategory(@PathVariable String id,
			@Valid @RequestBody ChangeCategoryResource resource) {

		log.info("接收到變更文檔分類請求，ID: {}, 新分類: {}", id, resource.newCategory());

		// 轉換為應用層指令
		ChangeDocumentCategoryCommand command = new ChangeDocumentCategoryCommand(id, resource.newCategory());
		commandService.changeCategory(id, command);

		return ResponseEntity.ok(new KnowledgeCreatedResource("200", "分類變更成功，系統正在非同步更新 AI 向量庫", id));
	}
}