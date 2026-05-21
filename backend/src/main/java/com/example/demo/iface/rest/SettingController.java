package com.example.demo.iface.rest;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.application.service.SettingCommandService;
import com.example.demo.application.service.SettingQueryService;
import com.example.demo.application.shared.command.CreateSettingCommand;
import com.example.demo.application.shared.command.UpdateSettingCommand;
import com.example.demo.application.shared.dto.SettingQueried;
import com.example.demo.iface.dto.req.CreateSettingResource;
import com.example.demo.iface.dto.req.UpdateSettingResource;
import com.example.demo.iface.dto.res.SettingCreatedResource;
import com.example.demo.iface.dto.res.SettingDeletedResource;
import com.example.demo.iface.dto.res.SettingUpdatedResource;
import com.example.demo.iface.dto.res.SettingsSummaryGottenResource;
import com.example.demo.infra.mapper.SettingMapper;

import jakarta.validation.Valid;
import lombok.AllArgsConstructor;

/**
 * Setting API
 */
@RestController
@AllArgsConstructor
@RequestMapping("/api/v1/settings")
public class SettingController {

	private SettingMapper mapper;
	private SettingQueryService settingQueryService;
	private SettingCommandService settingCommandService;

	/**
	 * 新增 設定
	 * 
	 * @param resource
	 * @return ResponseEntity<SettingCreatedResource>
	 */
	@PostMapping("")
	public ResponseEntity<SettingCreatedResource> create(@Valid @RequestBody CreateSettingResource resource) {
		// 防腐處理 resource -> command
		CreateSettingCommand command = mapper.transformDto(resource);
		settingCommandService.create(command);
		return new ResponseEntity<>(new SettingCreatedResource("201", "成功新增一筆資料"), HttpStatus.CREATED);
	}

	/**
	 * 查詢設定
	 * 
	 * @param service    服務
	 * @param dataType   資料種類
	 * @param type       設定類別
	 * @param name       名稱
	 * @param activeFlag 是否生效
	 * @return ResponseEntity<List<SettingQueriedResource>>
	 */
	@GetMapping("/summary")
	public ResponseEntity<SettingsSummaryGottenResource> summary(@RequestParam(required = false) String dataType,
			@RequestParam(required = false) String type, @RequestParam(required = false) String name,
			@RequestParam(required = false) String activeFlag) {
		List<SettingQueried> data = settingQueryService.summary(dataType, type, name, activeFlag);
		return new ResponseEntity<>(new SettingsSummaryGottenResource("200", "查詢成功", data), HttpStatus.OK);
	}

	/**
	 * 修改 設定
	 * 
	 * @param id
	 * @param resource
	 * @return ResponseEntity<SettingUpdatedResource>
	 */
	@PutMapping("/{id}")
	public ResponseEntity<SettingUpdatedResource> update(@PathVariable Long id,
			@RequestBody UpdateSettingResource resource) {
		// 防腐處理 resource -> command
		UpdateSettingCommand command = mapper.transformDto(resource);
		settingCommandService.update(id, command);
		return new ResponseEntity<>(new SettingUpdatedResource("200", "成功更新一筆資料"), HttpStatus.CREATED);
	}

	/**
	 * 刪除特定設定
	 * 
	 * @param id
	 * @return ResponseEntity<SettingDeletedResource>
	 */
	@DeleteMapping("/{id}")
	public ResponseEntity<SettingDeletedResource> create(@PathVariable Long id) {
		settingCommandService.delete(id);
		return new ResponseEntity<>(new SettingDeletedResource("200", "成功刪除一筆資料"), HttpStatus.OK);
	}
}
