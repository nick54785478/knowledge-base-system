package com.example.demo.infra.debezium.resolver;

import java.util.Arrays;
import java.util.function.Function;
import java.util.function.Predicate;

import com.example.demo.application.domain.audit.aggregate.vo.DocumentOperationType;
import com.fasterxml.jackson.databind.JsonNode;

import lombok.RequiredArgsConstructor;

/**
 * Debezium 操作類型解析器 (Operation Resolver)
 * <p>
 * 負責解析 Debezium CDC 傳遞的 JSON 節點，判斷其代表的資料庫異動類型 (CREATE, UPDATE, DELETE)。 本設計運用
 * Enum 實作輕量級的規則引擎 (Lightweight Rule Engine)，
 * 將「判斷條件」與「解析邏輯」封裝為獨立規則，依序評估以推導出正確的操作類型， 藉此消除繁雜的 if-else 分支，提升程式碼的擴充性與可讀性。
 * </p>
 */
@RequiredArgsConstructor
public enum DebeziumOperationResolver {

	/**
	 * 規則 1 (顯式解析)：若 JSON 內明確帶有 'op' 欄位，則直接映射其對應的操作代碼
	 */
	EXPLICIT_OP(node -> !node.path("op").asText("").isEmpty(), node -> mapExplicitOp(node.path("op").asText())),

	/**
	 * 規則 2 (隱式推斷)：若無 'op' 欄位，且「沒有 before 狀態、只有 after 狀態」，推斷為新增
	 */
	IMPLICIT_CREATE(node -> isNullOrMissing(node.path("before")) && !isNullOrMissing(node.path("after")),
			node -> DocumentOperationType.CREATE),

	/**
	 * 規則 3 (隱式推斷)：若無 'op' 欄位，且「只有 before 狀態、沒有 after 狀態」，推斷為刪除
	 */
	IMPLICIT_DELETE(node -> !isNullOrMissing(node.path("before")) && isNullOrMissing(node.path("after")),
			node -> DocumentOperationType.DELETE),

	/**
	 * 規則 4 (隱式推斷)：若無 'op' 欄位，且「同時存在 before 與 after 狀態」，推斷為修改
	 */
	IMPLICIT_UPDATE(node -> !isNullOrMissing(node.path("before")) && !isNullOrMissing(node.path("after")),
			node -> DocumentOperationType.UPDATE// 👈 這裡
	);

	/**
	 * 規則觸發條件
	 */
	private final Predicate<JsonNode> condition;

	/**
	 * 條件成立時的操作類型解析邏輯
	 */
	private final Function<JsonNode, DocumentOperationType> typeResolver;

	/**
	 * 解析方法：依序評估各項規則，找出第一項符合條件的規則並執行解析
	 *
	 * @param dataNode 包含 Debezium 異動資訊的 JSON 節點
	 * @return 解析後的操作類型 (CREATE, UPDATE, DELETE)，若無法辨識則回傳 "UNKNOWN"
	 */
	public static DocumentOperationType determineType(JsonNode dataNode) {
		return Arrays.stream(values()).filter(rule -> rule.condition.test(dataNode)).findFirst()
				.map(rule -> rule.typeResolver.apply(dataNode)).orElse(DocumentOperationType.UNKNOWN); // 💡 使用 Enum
																										// 代替字串
	}

	/**
	 * 輔助方法：判斷 JSON 節點是否為空或遺失
	 * 
	 * @param node JSON 節點
	 */
	private static boolean isNullOrMissing(JsonNode node) {
		return node.isMissingNode() || node.isNull();
	}

	/**
	 * 輔助方法：將 Debezium 官方定義的 op 代碼映射為可讀的業務操作類型
	 * 
	 * @param op op 代碼
	 */
	private static DocumentOperationType mapExplicitOp(String op) { // 👈 回傳型別要改
		return switch (op) {
		case "c", "r" -> DocumentOperationType.CREATE;
		case "u" -> DocumentOperationType.UPDATE;
		case "d" -> DocumentOperationType.DELETE;
		default -> DocumentOperationType.UNKNOWN;
		};
	}
}