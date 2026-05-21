package com.example.demo.application.shared.command;

public record ChangeDocumentCategoryCommand(String documentId, String newCategory) {
}