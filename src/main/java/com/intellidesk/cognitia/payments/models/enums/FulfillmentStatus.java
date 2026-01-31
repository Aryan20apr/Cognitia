package com.intellidesk.cognitia.payments.models.enums;

/**
 * Tracks whether a payment order has been consumed/fulfilled for its intended purpose.
 * Used to prevent replay attacks (reusing the same payment for multiple fulfillments).
 */
public enum FulfillmentStatus {

    UNFULFILLED,

    FULFILLED,

    EXPIRED
}
