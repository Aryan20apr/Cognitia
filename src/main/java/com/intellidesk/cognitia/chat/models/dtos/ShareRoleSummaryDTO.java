package com.intellidesk.cognitia.chat.models.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShareRoleSummaryDTO {
    private Integer id;
    private String name;
}
