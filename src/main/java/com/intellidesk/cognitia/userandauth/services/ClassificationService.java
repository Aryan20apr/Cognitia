package com.intellidesk.cognitia.userandauth.services;

import java.util.List;
import java.util.UUID;

import com.intellidesk.cognitia.userandauth.models.dtos.ClassificationLevelDTO;

public interface ClassificationService {

    ClassificationLevelDTO create(String name, Integer rank);

    List<ClassificationLevelDTO> getAll();

    ClassificationLevelDTO update(UUID id, String name, Integer rank);

    void delete(UUID id);
}
