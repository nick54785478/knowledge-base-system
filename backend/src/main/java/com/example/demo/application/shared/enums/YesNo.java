package com.example.demo.application.shared.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;


@AllArgsConstructor
public enum YesNo {

	Y("Y"), N("N");
	
	@Getter
	private final String value;
	
}
