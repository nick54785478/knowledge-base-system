package com.example.demo.application.shared.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SettingQueried {

	private Long id;

	private String dataType; // 資料種類

	private String type; // 種類

	private String name; // 名稱

	private String code;

	private String value;

	private String description; // 敘述

	private Integer priorityNo; // 順序號(從 1 開始)

	private String activeFlag; // 是否有效
}
