package com.example.demo.infra.repository;

import java.util.List;

import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;

import com.example.demo.application.domain.setting.aggregate.Setting;
import com.example.demo.application.shared.enums.YesNo;

public interface SettingRepository extends JpaRepository<Setting, Long> {

	List<Setting> findByDataTypeAndActiveFlag(String dataType, YesNo activeFlag);

	List<Setting> findByDataTypeAndTypeAndActiveFlag(String dataType, String type, YesNo activeFlag);

	List<Setting> findAll(Specification<Setting> specification);

	Setting findByCode(String code);
}
