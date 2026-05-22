package com.example.demo.infra.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.demo.application.domain.checkpoint.aggregate.SystemCheckpoint;

public interface SystemCheckpointRepository extends JpaRepository<SystemCheckpoint, String> {
}
