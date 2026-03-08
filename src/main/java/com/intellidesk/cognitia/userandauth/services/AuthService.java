package com.intellidesk.cognitia.userandauth.services;

public interface AuthService {

    void verifySignupOtp(String email, String otp);

    void resendOtp(String email, String purpose);

    void activateAccount(String token);

    void forgotPassword(String email);

    void resetPassword(String email, String otp, String newPassword);

    boolean isEmailVerified(String email);
}
