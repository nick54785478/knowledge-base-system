package com.example.demo.application.service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.example.demo.application.domain.setting.aggregate.Setting;
import com.example.demo.application.shared.dto.OptionQueried;
import com.example.demo.application.shared.enums.YesNo;
import com.example.demo.infra.mapper.SettingMapper;
import com.example.demo.infra.repository.SettingRepository;

import lombok.AllArgsConstructor;

@Service
@AllArgsConstructor
public class OptionQueryService {

	private SettingMapper mapper;
	private SettingRepository settingRepository;

	/**
	 * 透過 DataType 查詢相關特定 Setting Type 下拉式選單
	 * 
	 * 當 type 重複的資料被過濾掉只能留一個，將 name, code 設置進 OptionQueried
	 * 
	 * @param dataType 資料種類
	 * @return List<OptionQueried> 下拉式清單
	 */
	public List<OptionQueried> getSettingTypeOptions(String dataType) {
		List<Setting> settingList = settingRepository.findByDataTypeAndActiveFlag(dataType, YesNo.Y);
		// 利用 Set 來記錄已經處理過的 type
		Set<String> seenTypes = new HashSet<>();

		return settingList.stream()
				// 1. 過濾：Set.add() 如果發現已存在會回傳 false，藉此過濾掉重複的 type (保留第一筆)
				.filter(setting -> seenTypes.add(setting.getType()))
				// 2. 轉換：將 Setting 轉換成 OptionQueried，並明確設置 name 與 code
				.map(setting -> {
					return mapper.transformDto(setting);
				})
				// 3. 收集成 List
				.collect(Collectors.toList());
	}
	
	/**
	 * 透過 DataType 查詢相關特定 Setting Type 下拉式選單(這只取 Type 的值)
	 * 
	 * 當 type 重複的資料被過濾掉只能留一個
	 * 
	 * @param dataType 資料種類
	 * @return List<OptionQueried> 下拉式清單
	 */
	public List<OptionQueried> getSettingTypes(String dataType) {
		List<Setting> settingList = settingRepository.findByDataTypeAndActiveFlag(dataType, YesNo.Y);
		// 利用 Set 來記錄已經處理過的 type
		Set<String> seenTypes = new HashSet<>();

		return settingList.stream()
				// 1. 過濾：Set.add() 如果發現已存在會回傳 false，藉此過濾掉重複的 type (保留第一筆)
				.filter(setting -> seenTypes.add(setting.getType()))
				// 2. 轉換：將 Setting 轉換成 OptionQueried，並明確設置 name 與 code
				.map(setting -> {
					return mapper.mapTypeOptionWithType(setting);
				})
				// 3. 收集成 List
				.collect(Collectors.toList());
	}

	/**
	 * 透過 DataType 及 Type 查詢相關特定下拉式選單
	 * 
	 * @param dataType 資料種類
	 * @param type     種類
	 * @return List<OptionQueried> 下拉式清單
	 */
	public List<OptionQueried> getDropdownOptions(String dataType, String type) {
		List<Setting> settingList = settingRepository.findByDataTypeAndTypeAndActiveFlag(dataType, type, YesNo.Y);
		return settingList.stream().map(mapper::transformDto).toList();				
	}

}
