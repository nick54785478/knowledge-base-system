package com.example.demo.application.service;

import org.springframework.stereotype.Service;

import com.example.demo.application.domain.setting.aggregate.Setting;
import com.example.demo.application.domain.setting.profile.SettingProfile;
import com.example.demo.application.shared.command.CreateSettingCommand;
import com.example.demo.application.shared.command.UpdateSettingCommand;
import com.example.demo.infra.repository.SettingRepository;
import com.example.demo.infra.shared.exception.ValidationException;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@AllArgsConstructor
public class SettingCommandService {

	private SettingRepository settingRepository;

	/**
	 * 建立設定
	 * 
	 * @param command {@link CreateSettingCommand}
	 */
	public void create(CreateSettingCommand command) {

		// Command 轉換為 Domain 參數物件 (這層轉換保護了 Domain 不受外層污染)
		SettingProfile profile = new SettingProfile(command.dataType(), command.type(), command.name(), command.code(),
				command.value(), command.description(), command.priorityNo());
		// 進行新增動作
		Setting setting = Setting.create(profile);
		settingRepository.save(setting);
	}

	/**
	 * 修改設定
	 * 
	 * @param settingId Setting ID
	 * @param command   {@link UpdateSettingCommand}
	 */
	public void update(Long settingId, UpdateSettingCommand command) {
		// Command 轉換為 Domain 參數物件 (這層轉換保護了 Domain 不受外層污染)
		SettingProfile profile = new SettingProfile(command.dataType(), command.type(), command.name(), command.code(),
				command.value(), command.description(), command.priorityNo());
		// 檢查資料
		settingRepository.findById(settingId).ifPresentOrElse(setting -> {
			setting.update(profile, command.activeFlag());
			settingRepository.save(setting);
		}, () -> {
			throw new ValidationException("VALIDATE_FAILED", "查無此資料，更新失敗");
		});
	}

	/**
	 * 刪除特定 id 的 Setting 資料
	 * 
	 * @param id Setting id
	 */
	public void delete(Long id) {
		settingRepository.findById(id).ifPresentOrElse(setting -> {
			setting.delete();
			settingRepository.save(setting);
		}, () -> {
			log.error("查無此資料，ID:{} 刪除失敗 ", id);
			throw new ValidationException("VALIDATE_FAILED", "查無此資料，刪除失敗");
		});
	}

}
