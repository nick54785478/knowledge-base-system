package com.example.demo.infra.mapper;

import org.mapstruct.Mapper;

import com.example.demo.application.shared.command.CreateKnowledgeCommand;
import com.example.demo.application.shared.command.UpdateKnowledgeCommand;
import com.example.demo.config.config.MapStructConfiguration;
import com.example.demo.iface.dto.req.CreateKnowledgeResource;
import com.example.demo.iface.dto.req.UpdateKnowledgeResource;

@Mapper(componentModel = "spring", config = MapStructConfiguration.class)
public interface KnowledgeMapper {

	CreateKnowledgeCommand transform(CreateKnowledgeResource resource);

	UpdateKnowledgeCommand transform(UpdateKnowledgeResource resource);
}
