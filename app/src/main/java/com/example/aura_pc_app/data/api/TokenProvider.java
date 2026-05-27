package com.example.aura_pc_app.data.api;

/**
 * Abstraction for reading the current access token.
 * Implemented by {@link TokenManager}; easily faked in unit tests.
 */
public interface TokenProvider {
    String getAccessToken();
}
