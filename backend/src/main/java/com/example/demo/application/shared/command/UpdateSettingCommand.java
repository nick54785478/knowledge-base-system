package com.example.demo.application.shared.command;

public record UpdateSettingCommand(String dataType, // 資料種類
		String type, // 種類
		String name, // 名稱
		String code, // 代碼
		String value, // 值
		String description, // 敘述
		Integer priorityNo, // 順序號(從 1 開始)
		String activeFlag) {
}
