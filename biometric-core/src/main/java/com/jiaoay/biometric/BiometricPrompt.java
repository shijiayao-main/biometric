package com.jiaoay.biometric;

import androidx.annotation.IntDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * A class that manages a system-provided biometric prompt. On devices running Android 9.0 (API 28)
 * and above, this will show a system-provided authentication prompt, using one of the device's
 * supported biometric modalities (fingerprint, iris, face, etc). Prior to Android 9.0, this will
 * instead show a custom fingerprint authentication dialog. The prompt will persist across
 * configuration changes unless explicitly canceled. For security reasons, the prompt will be
 * dismissed when the client application is no longer in the foreground.
 *
 * <p>To persist authentication across configuration changes, developers should (re)create the
 * prompt every time the activity/fragment is created. Instantiating the prompt with a new
 * callback early in the fragment/activity lifecycle (e.g. in {@code onCreate()}) will allow the
 * ongoing authentication session's callbacks to be received by the new fragment/activity instance.
 * Note that {@code cancelAuthentication()} should not be called, and {@code authenticate()} does
 * not need to be invoked during activity/fragment creation.
 */
public class BiometricPrompt {

    /**
     * There is no error, and the user can successfully authenticate.
     */
    public static final int BIOMETRIC_SUCCESS = 0;

    /**
     * The hardware is unavailable. Try again later.
     */
    public static final int ERROR_HW_UNAVAILABLE = 1;

    /**
     * The sensor was unable to process the current image.
     */
    public static final int ERROR_UNABLE_TO_PROCESS = 2;

    /**
     * The current operation has been running too long and has timed out.
     *
     * <p>This is intended to prevent programs from waiting for the biometric sensor indefinitely.
     * The timeout is platform and sensor-specific, but is generally on the order of ~30 seconds.
     */
    public static final int ERROR_TIMEOUT = 3;

    /**
     * The operation can't be completed because there is not enough device storage remaining.
     */
    public static final int ERROR_NO_SPACE = 4;

    /**
     * The operation was canceled because the biometric sensor is unavailable. This may happen when
     * the user is switched, the device is locked, or another pending operation prevents it.
     */
    public static final int ERROR_CANCELED = 5;

    /**
     * The operation was canceled because the API is locked out due to too many attempts. This
     * occurs after 5 failed attempts, and lasts for 30 seconds.
     */
    public static final int ERROR_LOCKOUT = 7;

    /**
     * The operation failed due to a vendor-specific error.
     *
     * <p>This error code may be used by hardware vendors to extend this list to cover errors that
     * don't fall under one of the other predefined categories. Vendors are responsible for
     * providing the strings for these errors.
     *
     * <p>These messages are typically reserved for internal operations such as enrollment but may
     * be used to express any error that is not otherwise covered. In this case, applications are
     * expected to show the error message, but they are advised not to rely on the message ID, since
     * this may vary by vendor and device.
     */
    public static final int ERROR_VENDOR = 8;

    /**
     * The operation was canceled because {@link #ERROR_LOCKOUT} occurred too many times. Biometric
     * authentication is disabled until the user unlocks with their device credential (i.e. PIN,
     * pattern, or password).
     */
    public static final int ERROR_LOCKOUT_PERMANENT = 9;

    /**
     * The user canceled the operation.
     *
     * <p>Upon receiving this, applications should use alternate authentication, such as a password.
     * The application should also provide the user a way of returning to biometric authentication,
     * such as a button.
     */
    public static final int ERROR_USER_CANCELED = 10;

    /**
     * The user does not have any biometrics enrolled.
     */
    public static final int ERROR_NO_BIOMETRICS = 11;

    /**
     * The device does not have the required authentication hardware.
     */
    public static final int ERROR_HW_NOT_PRESENT = 12;

    /**
     * The user pressed the negative button.
     */
    public static final int ERROR_NEGATIVE_BUTTON = 13;

    /**
     * The device does not have pin, pattern, or password set up.
     */
    public static final int ERROR_NO_DEVICE_CREDENTIAL = 14;

    /**
     * A security vulnerability has been discovered with one or more hardware sensors. The
     * affected sensor(s) are unavailable until a security update has addressed the issue.
     */
    public static final int ERROR_SECURITY_UPDATE_REQUIRED = 15;

    /**
     * An error code that may be returned during authentication.
     */
    @IntDef({
            ERROR_HW_UNAVAILABLE,
            ERROR_UNABLE_TO_PROCESS,
            ERROR_TIMEOUT,
            ERROR_NO_SPACE,
            ERROR_CANCELED,
            ERROR_LOCKOUT,
            ERROR_VENDOR,
            ERROR_LOCKOUT_PERMANENT,
            ERROR_USER_CANCELED,
            ERROR_NO_BIOMETRICS,
            ERROR_HW_NOT_PRESENT,
            ERROR_NEGATIVE_BUTTON,
            ERROR_NO_DEVICE_CREDENTIAL
    })
    @Retention(RetentionPolicy.SOURCE)
    @interface AuthenticationError {
    }

    /**
     * Authentication type reported by {@link AuthenticationResult} when the user authenticated via
     * an unknown method.
     *
     * <p>This value may be returned on older Android versions due to partial incompatibility
     * with a newer API. It does NOT necessarily imply that the user authenticated with a method
     * other than those represented by {@link #AUTHENTICATION_RESULT_TYPE_DEVICE_CREDENTIAL} and
     * {@link #AUTHENTICATION_RESULT_TYPE_BIOMETRIC}.
     */
    public static final int AUTHENTICATION_RESULT_TYPE_UNKNOWN = -1;

    /**
     * Authentication type reported by {@link AuthenticationResult} when the user authenticated by
     * entering their device PIN, pattern, or password.
     */
    public static final int AUTHENTICATION_RESULT_TYPE_DEVICE_CREDENTIAL = 1;

    /**
     * Authentication type reported by {@link AuthenticationResult} when the user authenticated by
     * presenting some form of biometric (e.g. fingerprint or face).
     */
    public static final int AUTHENTICATION_RESULT_TYPE_BIOMETRIC = 2;

    /**
     * The authentication type that was used, as reported by {@link AuthenticationResult}.
     */
    @IntDef({
            AUTHENTICATION_RESULT_TYPE_UNKNOWN,
            AUTHENTICATION_RESULT_TYPE_DEVICE_CREDENTIAL,
            AUTHENTICATION_RESULT_TYPE_BIOMETRIC
    })
    @Retention(RetentionPolicy.SOURCE)
    public @interface AuthenticationResultType {
    }
}
