package com.intellidesk.cognitia.userandauth.models.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Data
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class PermissionDTO {
    
    Integer id;
    String name;
}
