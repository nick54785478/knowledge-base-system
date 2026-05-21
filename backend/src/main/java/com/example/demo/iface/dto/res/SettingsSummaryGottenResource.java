package com.example.demo.iface.dto.res;

import java.util.List;

import com.example.demo.application.shared.dto.SettingQueried;

public record SettingsSummaryGottenResource(String code, String message, List<SettingQueried> data) {

}
