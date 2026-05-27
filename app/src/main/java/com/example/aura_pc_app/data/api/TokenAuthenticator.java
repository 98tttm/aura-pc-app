package com.example.aura_pc_app.data.api;

import android.util.Log;

import com.example.aura_pc_app.utils.Constants;

import java.io.IOException;

import okhttp3.Authenticator;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.Route;

/**
 * OkHttp Authenticator that handles HTTP 401 responses by silently
 * refreshing the access token and retrying the original request.
 *
 * <p>Flow: Request → 401 → refresh token → retry with new token.</p>
 * <p>Safety: retries are capped at {@link Constants#MAX_TOKEN_RETRY}
 * to prevent infinite loops.</p>
 */
public class TokenAuthenticator implements Authenticator {

    private static final String TAG = "TokenAuthenticator";
    private static final String HEADER_AUTHORIZATION = "Authorization";
    private static final String TOKEN_PREFIX = "Bearer ";

    private final TokenManager tokenManager;

    public TokenAuthenticator(TokenManager tokenManager) {
        this.tokenManager = tokenManager;
    }

    @Override
    public Request authenticate(Route route, Response response) throws IOException {

        Log.w(TAG, "401 Unauthorized → " + response.request().url());

        if (responseCount(response) >= Constants.MAX_TOKEN_RETRY) {
            Log.e(TAG, "Max retry reached, clearing session");
            tokenManager.clearTokens();
            return null;
        }

        String refreshToken = tokenManager.getRefreshToken();
        if (refreshToken == null || refreshToken.isEmpty()) {
            Log.e(TAG, "No refresh token available, login required");
            return null;
        }

        // TODO: Replace with real refresh-token API call:
        //   Call<TokenResponse> call = refreshApi.refresh(refreshToken);
        //   TokenResponse body = call.execute().body();
        //   newAccessToken = body.getAccessToken();
        String newAccessToken = "refreshed_" + System.currentTimeMillis();
        String newRefreshToken = "rf_" + System.currentTimeMillis();

        tokenManager.saveAccessToken(newAccessToken);
        tokenManager.saveRefreshToken(newRefreshToken);

        Log.d(TAG, "Token refreshed successfully, retrying request");

        return response.request().newBuilder()
                .header(HEADER_AUTHORIZATION, TOKEN_PREFIX + newAccessToken)
                .build();
    }

    /**
     * Counts prior responses chained to this one to detect retry loops.
     */
    private int responseCount(Response response) {
        int count = 1;
        while ((response = response.priorResponse()) != null) {
            count++;
        }
        return count;
    }
}
