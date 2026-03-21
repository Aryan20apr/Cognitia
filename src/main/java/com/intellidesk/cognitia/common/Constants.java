package com.intellidesk.cognitia.common;

import java.util.UUID;

public class Constants {
    public static final UUID PLATFORM_TENANT_ID = UUID.fromString("00000000-0000-0000-0000-000000000000");

    public static final String PARAM_USER_ID = "userId";
    public static final String PARAM_TENANT_ID = "tenantId";
    public static final String PARAM_REQUEST_ID = "requestId";

    public static final String REDIS_TENANT_TOKEN_KEY_FMT = "tenant:%s:tokens:%s"; // tenantId, yyyy-MM
    public static final String REDIS_USER_TOKEN_KEY_FMT = "tenant:%s:user:%s:tokens:%s"; // tenantId, userId, yyyy-MM
    public static final String REDIS_REQUEST_ID_KEY_FMT = "request:%s:processed"; // requestId

    public static final String SAME_SITE_NONE = "None";
    public static final String SAME_SITE_LAX = "Lax";
    public static final String SAME_SITE_STRICT = "Strict";

    public static final String TEMPLATE_OTP = "otp";
    public static final String TEMPLATE_QUOTA_WARNING = "quota-warning";
    public static final String TEMPLATE_PAYMENT_SUCCESS = "payment-success";
    public static final String TEMPLATE_PAYMENT_FAILED = "payment-failed";
    public static final String TEMPLATE_REFUND_PROCESSED = "refund-processed";

    public static final String DATE_FORMAT_DISPLAY = "MMM d, yyyy";
    public static final String CURRENCY_INR = "INR";
    public static final String CURRENCY_SYMBOL_INR = "\u20B9";
    public static final String FALLBACK_NA = "N/A";

    public static final String INVITATION_ACCEPT_ENDPOINT = "/invite/accept?token=";
}
