package com.example.demo.infra.shared.exception;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 用於定義檢核失敗的 Exception
 */
@Data
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class ValidationException extends RuntimeException {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private final String code;

	private final String message;
}