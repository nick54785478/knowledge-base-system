package com.example.demo.iface.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.example.demo.infra.shared.exception.ResourceNotFoundException;
import com.example.demo.infra.shared.exception.ValidationException;

/**
 * 全域例外處理器
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

	/**
	 * ValidationException 例外處理
	 */
	@ExceptionHandler(ValidationException.class)
	public ResponseEntity<BaseExceptionResponse> handleValidationException(ValidationException e) {
		return ResponseEntity.status(HttpStatus.UNPROCESSABLE_CONTENT)
				.body(new BaseExceptionResponse(e.getCode(), e.getMessage()));
	}

	/**
	 * ResourceNotFoundException 例外處理
	 */
	@ExceptionHandler(ResourceNotFoundException.class)
	public ResponseEntity<BaseExceptionResponse> handleResourceNotFoundException(ResourceNotFoundException e) {
		return ResponseEntity.status(HttpStatus.BAD_REQUEST)
				.body(new BaseExceptionResponse(e.getCode(), e.getMessage()));
	}

	/**
	 * 例外回傳物件
	 * 
	 * @param code    錯誤碼
	 * @param message 訊息
	 */
	record BaseExceptionResponse(String code, String message) {

	}
}