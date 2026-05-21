package com.example.demo.application.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.example.demo.application.domain.setting.aggregate.Setting;
import com.example.demo.application.shared.dto.SettingQueried;
import com.example.demo.application.shared.enums.YesNo;
import com.example.demo.infra.mapper.SettingMapper;
import com.example.demo.infra.repository.SettingRepository;
import com.example.demo.infra.spec.GetSettingsSpecification;

import lombok.AllArgsConstructor;

@Service
@AllArgsConstructor
public class SettingQueryService {

	private SettingMapper mapper;
	private SettingRepository settingRepository;

	/**
	 * 根據條件查詢 Setting
	 * 
	 * @param dataType   資料種類
	 * @param type       設定類別
	 * @param name       名稱
	 * @param activeFlag 是否生效
	 * @return List<SettingQueried> 設定清單
	 */
	public List<SettingQueried> summary(String dataType, String type, String name, String activeFlag) {
		GetSettingsSpecification specifiaction = new GetSettingsSpecification(dataType, type, name, activeFlag);
		List<Setting> settingList = settingRepository.findAll(specifiaction.toSpecification());
		return mapper.transformDto(settingList);
	}

	/**
	 * 透過 DataType 查詢相關 Setting 清單
	 * 
	 * @param dataType 資料種類
	 * @return List<SettingQueried> 設定清單
	 */
	public List<SettingQueried> getSettingByDataType(String dataType) {
		List<Setting> settingList = settingRepository.findByDataTypeAndActiveFlag(dataType, YesNo.Y);
		return mapper.transformDto(settingList);
	}

}
