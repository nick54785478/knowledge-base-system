package com.example.demo.application.domain.setting.aggregate;

import com.example.demo.application.domain.setting.profile.SettingProfile;
import com.example.demo.application.shared.enums.YesNo;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Entity
@Getter
@ToString
@Table(name = "setting")
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
public class Setting {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "data_type")
	private String dataType; // 資料類型，如: dropdown 或

	@Column(name = "type")
	private String type; // 種類

	@Column(name = "name")
	private String name; // 名稱，如: UI Label Name

	@Column(name = "code")
	private String code; // 代碼，存 DB 的資料

	@Column(name = "value")
	private String value; // 值

	@Column(name = "description")
	private String description; // 敘述

	@Column(name = "priority_no")
	private Integer priorityNo; // 順序號(從 1 開始)

	@Enumerated(EnumType.STRING)
	@Column(name = "active_flag")
	private YesNo activeFlag = YesNo.Y; // 是否有效

	/**
	 * 建立一筆 Setting
	 * 
	 * @param po {@link Setting}
	 */
	public static Setting create(SettingProfile profile) {
		Setting setting = new Setting();
		setting.dataType = profile.dataType();
		setting.type = profile.type();
		setting.name = profile.name();
		setting.value = profile.value();
		setting.code = profile.code();
		setting.description = profile.description();
		setting.priorityNo = profile.priorityNo();
		setting.activeFlag = YesNo.Y;
		return setting;
	}

	/**
	 * 修改一筆 Setting
	 * 
	 * @param profile    {@link SettingProfile}
	 * @param activeFlag 是否生效
	 */
	public void update(SettingProfile profile, String activeFlag) {
		this.dataType = profile.dataType();
		this.type = profile.type();
		this.name = profile.name();
		this.value = profile.value();
		this.code = profile.code();
		this.description = profile.description();
		this.priorityNo = profile.priorityNo();
		this.activeFlag = YesNo.valueOf(activeFlag);
	}

	/**
	 * 刪除 (更改 activeFlag = 'N')
	 */
	public void delete() {
		this.activeFlag = YesNo.N;
	}
}