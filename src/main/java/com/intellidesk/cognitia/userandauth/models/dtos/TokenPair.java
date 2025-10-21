package com.intellidesk.cognitia.userandauth.models.dtos;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@AllArgsConstructor
@Getter
@Setter
public class TokenPair {
    String newAccessToken;
    String newRefreshToken;

}
