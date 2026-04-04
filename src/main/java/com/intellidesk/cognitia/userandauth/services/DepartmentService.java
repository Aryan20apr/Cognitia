package com.intellidesk.cognitia.userandauth.services;

import java.util.List;
import java.util.UUID;

import com.intellidesk.cognitia.userandauth.models.dtos.DepartmentDTO;

public interface DepartmentService {

    DepartmentDTO create(String name);

    List<DepartmentDTO> getAll();

    DepartmentDTO update(UUID id, String name);

    void delete(UUID id);
}
