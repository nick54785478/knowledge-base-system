package com.example.demo.iface.dto.req;

import jakarta.validation.constraints.NotBlank;

public record ChangeCategoryResource(
	    @NotBlank(message = "新分類不能為空")
	    String newCategory
	) {}