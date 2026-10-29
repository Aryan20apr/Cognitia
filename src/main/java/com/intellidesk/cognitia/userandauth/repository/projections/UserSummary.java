package com.intellidesk.cognitia.userandauth.repository.projections;

import java.util.UUID;

public interface UserSummary {
    UUID getId();
    String getName();
    String getEmail();
}
