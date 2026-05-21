package com.example.demo.iface.dto.res;

import java.util.List;

import com.example.demo.application.shared.dto.OptionQueried;

public record OptionQueriedResource(String code, String message, List<OptionQueried> data) {

}
