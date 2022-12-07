package com.jiaoay.biometric;

import androidx.annotation.Nullable;

import java.util.Arrays;

/**
 * A container for data associated with a biometric authentication error, which may be handled by
 * the client application's callback.
 */
public class BiometricErrorData {
    /**
     * An integer ID associated with this error.
     */
    @BiometricPrompt.AuthenticationError private final int mErrorCode;

    /**
     * A human-readable message that describes the error.
     */
    @Nullable private final CharSequence mErrorMessage;

    public BiometricErrorData(int errorCode, @Nullable CharSequence errorMessage) {
        mErrorCode = errorCode;
        mErrorMessage = errorMessage;
    }

    @BiometricPrompt.AuthenticationError
    public int getErrorCode() {
        return mErrorCode;
    }

    @Nullable
    public CharSequence getErrorMessage() {
        return mErrorMessage;
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(new Object[] {mErrorCode, convertToString(mErrorMessage)});
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if (obj instanceof BiometricErrorData) {
            final BiometricErrorData other = (BiometricErrorData) obj;
            return mErrorCode == other.mErrorCode && isErrorMessageEqualTo(other.mErrorMessage);
        }
        return false;
    }

    /**
     * Checks if a given error message is equivalent to the one for this object.
     *
     * @param otherMessage A message to compare to the error message for this object.
     * @return Whether the error message for this object and {@code otherMessage} are equivalent.
     */
    private boolean isErrorMessageEqualTo(@Nullable CharSequence otherMessage) {
        final String errorString = convertToString(mErrorMessage);
        final String otherString = convertToString(otherMessage);
        return (errorString == null && otherString == null)
                || (errorString != null && errorString.equals(otherString));
    }

    /**
     * Converts a nullable {@link CharSequence} message to an equivalent {@link String}.
     *
     * @param message The message to be converted.
     * @return A string matching the given message, or {@code null} if message is {@code null}.
     */
    @Nullable
    private static String convertToString(@Nullable CharSequence message) {
        return message != null ? message.toString() : null;
    }
}
