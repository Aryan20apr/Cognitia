package com.intellidesk.cognitia.common;

public class Constants {
    public static final String PARAM_USER_ID = "userId";
    public static final String PARAM_TENANT_ID = "tenantId";
    public static final String PARAM_REQUEST_ID = "requestId";

    public static final String REDIS_TENANT_TOKEN_KEY_FMT = "tenant:%s:tokens:%s"; // tenantId, yyyy-MM
    public static final String REDIS_USER_TOKEN_KEY_FMT = "tenant:%s:user:%s:tokens:%s"; // tenantId, userId, yyyy-MM
    public static final String REDIS_REQUEST_ID_KEY_FMT = "request:%s:processed"; // requestId

    public static final String SAME_SITE_NONE = "None";
    public static final String SAME_SITE_LAX = "Lax";
    public static final String SAME_SITE_STRICT = "Strict";
}
