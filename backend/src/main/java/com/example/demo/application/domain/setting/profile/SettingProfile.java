package com.example.demo.application.domain.setting.profile;

/**
 * Domain 層的參數物件 (Parameter Object)
 * <p>
 * 專門用於封裝建立與更新 Setting 時所需的核心屬性
 * </p>
 */
public record SettingProfile(String dataType, String type, String name, String code, String value, String description,
		Integer priorityNo) {
}