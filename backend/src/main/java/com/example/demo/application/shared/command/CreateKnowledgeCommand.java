package com.example.demo.application.shared.command;

import java.util.List;

/**
 * 新增知識庫文檔指令
 */
public record CreateKnowledgeCommand(String category, String title, String content, String author, List<String> tags) {
}