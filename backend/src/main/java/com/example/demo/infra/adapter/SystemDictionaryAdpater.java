package com.example.demo.infra.adapter;

import org.springframework.stereotype.Component;

import com.example.demo.application.domain.setting.aggregate.Setting;
import com.example.demo.application.port.SystemDictionaryPort;
import com.example.demo.infra.repository.SettingRepository;

import lombok.AllArgsConstructor;

/**
 * 系統分類字典
 */
@Component
@AllArgsConstructor
class SystemDictionaryAdpater implements SystemDictionaryPort {

	private SettingRepository settingRepository;

	@Override
	public String getCategoryNameByCode(String code) {
		Setting setting = settingRepository.findByCode(code);
		return setting == null ? "" : setting.getName();
	}

}
