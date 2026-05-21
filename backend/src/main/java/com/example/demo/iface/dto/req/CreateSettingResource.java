package com.example.demo.iface.dto.req;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;

@Valid
public record CreateSettingResource(String dataType, // 資料種類
		String type, // 種類
		String name, // 名稱
		String code, // 代碼
		String value, // 值
		String description, // 敘述
		@Min(value = 0) Integer priorityNo // 順序號
) {
}
