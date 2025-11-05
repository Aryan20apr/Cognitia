package com.intellidesk.cognitia.analytics.models.enums;

public   enum EnforcementMode {
        HARD, // deny when exceeded
        SOFT, // allow but mark and bill
        HYBRID // allow until credit exhausted, otherwise deny
    }