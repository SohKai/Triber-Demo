package com.brettsun.triber_demo.instagram.api;

/**
 * Listener interface for callbacks from the API. More or less rolls up Volley's default listeners
 * into one as we need to handle the case where the API responds successfully but with a bad meta component.
 */
public interface InstagramListener<T> {
    public void onResponse(final T response);
    public void onErrorResponse(final InstagramError error);
}
