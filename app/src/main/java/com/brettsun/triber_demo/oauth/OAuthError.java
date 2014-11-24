package com.brettsun.triber_demo.oauth;

public class OAuthError {
    public static final int NO_STATUS_CODE = -1;

    public final int errorCode;
    public final String error;
    public final String errorReason;
    public final String description;

    public OAuthError(final int errorCode, final String error, final String errorReason,
                        final String description) {
        this.errorCode = errorCode;
        this.error = error;
        this.errorReason = errorReason;
        this.description = description;
    }
}
