package com.example.demo.iface.rest;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.application.service.OptionQueryService;
import com.example.demo.application.shared.dto.OptionQueried;
import com.example.demo.iface.dto.res.OptionQueriedResource;

import lombok.AllArgsConstructor;

@RestController
@AllArgsConstructor
@RequestMapping("/api/v1/options")
public class OptionController {

	private OptionQueryService optionQueryService;

	/**
	 * 查詢 DataType 相關的 Setting Type (下拉式選單)
	 * 
	 * @param dataType 資料種類
	 * @return ResponseEntity<List<OptionQueriedResource>>
	 */
	@GetMapping("/{dataType}")
	public ResponseEntity<OptionQueriedResource> getSettingTypeOptions(@PathVariable String dataType) {
		List<OptionQueried> options = optionQueryService.getSettingTypeOptions(dataType);
		return new ResponseEntity<>(new OptionQueriedResource("200", "查詢成功", options), HttpStatus.OK);
	}

	/**
	 * 查詢 DataType 相關的 Setting Type (下拉式選單)
	 * 
	 * @param dataType 資料種類
	 * @return ResponseEntity<List<OptionQueriedResource>>
	 */
	@GetMapping("/{dataType}/type")
	public ResponseEntity<OptionQueriedResource> getSettingTypes(@PathVariable String dataType) {
		List<OptionQueried> options = optionQueryService.getSettingTypes(dataType);
		return new ResponseEntity<>(new OptionQueriedResource("200", "查詢成功", options), HttpStatus.OK);
	}

	/**
	 * 透過 DataType 及 Type 查詢相關設定 (下拉式選單)
	 * 
	 * @param dataType 資料種類
	 * @param type     設定種類
	 * @return ResponseEntity<List<OptionQueriedResource>>
	 */
	@GetMapping("/{dataType}/{type}")
	public ResponseEntity<OptionQueriedResource> getOptionsByDataTypeAndType(@PathVariable String dataType,
			@PathVariable String type) {
		List<OptionQueried> options = optionQueryService.getDropdownOptions(dataType, type);
		return new ResponseEntity<>(new OptionQueriedResource("200", "查詢成功", options), HttpStatus.OK);
	}
}
