package com.example.demo.infra.spec;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.springframework.data.jpa.domain.Specification;

import com.example.demo.application.domain.setting.aggregate.Setting;

import jakarta.persistence.criteria.Predicate;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GetSettingsSpecification {

	private String dataType;
	private String type;
	private String name;
	private String activeFlag;

	public Specification<Setting> toSpecification() {

		return ((root, query, cb) -> {
			List<Predicate> predicates = new ArrayList<>();

			if (StringUtils.isNotBlank(dataType)) {
				predicates.add(cb.equal(root.get("dataType"), dataType));
			}

			if (StringUtils.isNotBlank(type)) {
				predicates.add(cb.like(root.get("type"), "%" + type + "%"));
			}

			if (StringUtils.isNotBlank(name)) {
				predicates.add(cb.like(root.get("name"), "%" + name + "%"));
			}

			if (StringUtils.isNotBlank(activeFlag)) {
				predicates.add(cb.equal(root.get("activeFlag"), activeFlag));
			} else {
				predicates.add(cb.equal(root.get("activeFlag"), "Y"));
			}

			Predicate[] predicateArray = new Predicate[predicates.size()];
			query.where(cb.and(predicates.toArray(predicateArray)));
			return query.getRestriction();
		});
	}

}
