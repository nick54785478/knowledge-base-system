package com.example.demo.infra.mapper;

import java.util.List;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.example.demo.application.domain.setting.aggregate.Setting;
import com.example.demo.application.shared.command.CreateSettingCommand;
import com.example.demo.application.shared.command.UpdateSettingCommand;
import com.example.demo.application.shared.dto.OptionQueried;
import com.example.demo.application.shared.dto.SettingQueried;
import com.example.demo.config.MapStructConfiguration;
import com.example.demo.iface.dto.req.CreateSettingResource;
import com.example.demo.iface.dto.req.UpdateSettingResource;

@Mapper(componentModel = "spring", config = MapStructConfiguration.class)
public interface SettingMapper {

	List<SettingQueried> transformDto(List<Setting> settingList);

	CreateSettingCommand transformDto(CreateSettingResource resource);
	
	UpdateSettingCommand transformDto(UpdateSettingResource resource);
	
	@Mapping(source = "name", target = "label")
	@Mapping(source = "code", target = "value")
	OptionQueried transformDto(Setting aggregateRoot);
	
	@Mapping(source = "type", target = "label")
	@Mapping(source = "type", target = "value")
	OptionQueried mapTypeOptionWithType(Setting aggregateRoot);
	
}
