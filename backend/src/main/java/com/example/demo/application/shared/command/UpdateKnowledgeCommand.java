package com.example.demo.application.shared.command;

import java.util.List;

/**
 * 修改知識庫文檔指令
 */
public record UpdateKnowledgeCommand(String title, String content, List<String> tags) {
}