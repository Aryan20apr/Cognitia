package com.intellidesk.cognitia.userandauth.services;

public interface AuthService {

    Boolean verifyOtp(String email, String otp);

    void resendOtp(String email);

    void activateAccount(String token);

    void forgotPassword(String email);

    void resetPassword(String email, String otp, String newPassword);

    boolean isEmailVerified(String email);

    String validateInviteToken(String token);

    void acceptInvite(String token, String newPassword);
}
