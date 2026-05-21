package com.example.demo.iface.dto.res;

import java.util.List;

import com.example.demo.application.shared.view.DocumentHistoryView;

public record DocumentHistoryGottenResource(String code, String message, List<DocumentHistoryView> data) {

}
