package com.example.demo.iface.rest;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.application.service.DocumentHistoryQueryService;
import com.example.demo.application.service.DocumentReversionCommandService;
import com.example.demo.application.shared.view.DocumentHistoryView;
import com.example.demo.iface.dto.res.DocumentHistoryGottenResource;
import com.example.demo.iface.dto.res.DocumentRevertedResource;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/documents")
@RequiredArgsConstructor
public class DocumentTimeTravelController {

	private final DocumentHistoryQueryService historyQueryService;
	private final DocumentReversionCommandService revertCommandService;

	/**
	 * 取得文檔時光機 (歷史軌跡)
	 */
	@GetMapping("/{id}/history")
	public ResponseEntity<DocumentHistoryGottenResource> getDocumentHistory(@PathVariable("id") String documentId) {
		List<DocumentHistoryView> responseData = historyQueryService.getDocumentHistory(documentId);
		return ResponseEntity.ok(new DocumentHistoryGottenResource("200", "查詢成功", responseData));
	}

	/**
	 * 穿越時空：將文檔回退至特定歷史版本
	 */
	@PostMapping("/{id}/revert/{auditId}")
	public ResponseEntity<DocumentRevertedResource> revertDocument(@PathVariable("id") String documentId,
			@PathVariable("auditId") String targetAuditId) {

		revertCommandService.revertToVersion(documentId, targetAuditId);
		return ResponseEntity.ok(new DocumentRevertedResource("200", "已退回成功"));
	}
}