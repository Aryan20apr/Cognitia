package com.intellidesk.cognitia.userandauth.models.dtos;

import lombok.Data;

@Data
public class LoginRequestDTO {
    String email;
    String password;
    String deviceId;
    String ip;
    String userAgent;
}
