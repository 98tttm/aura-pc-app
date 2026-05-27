package com.example.aura_pc_app;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import com.example.aura_pc_app.data.api.AuthInterceptor;
import com.example.aura_pc_app.data.api.TokenProvider;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;

/**
 * Unit tests for {@link AuthInterceptor}.
 * Uses OkHttp {@link MockWebServer} to verify header injection behaviour.
 */
public class AuthInterceptorTest {

    private MockWebServer server;

    @Before
    public void setUp() throws IOException {
        server = new MockWebServer();
        server.start();
    }

    @After
    public void tearDown() throws IOException {
        server.shutdown();
    }

    @Test
    public void withToken_shouldAddAuthorizationHeader() throws Exception {
        TokenProvider provider = () -> "test_jwt_123";

        server.enqueue(new MockResponse().setResponseCode(200));

        OkHttpClient client = new OkHttpClient.Builder()
                .addInterceptor(new AuthInterceptor(provider))
                .build();

        client.newCall(new Request.Builder()
                .url(server.url("/api/products"))
                .build()
        ).execute();

        RecordedRequest recorded = server.takeRequest();
        String header = recorded.getHeader("Authorization");

        assertNotNull("Authorization header must be present", header);
        assertEquals("Bearer test_jwt_123", header);
    }

    @Test
    public void withoutToken_shouldNotAddHeader() throws Exception {
        TokenProvider provider = () -> null;

        server.enqueue(new MockResponse().setResponseCode(200));

        OkHttpClient client = new OkHttpClient.Builder()
                .addInterceptor(new AuthInterceptor(provider))
                .build();

        client.newCall(new Request.Builder()
                .url(server.url("/api/products"))
                .build()
        ).execute();

        RecordedRequest recorded = server.takeRequest();
        assertNull("No Authorization header expected", recorded.getHeader("Authorization"));
    }

    @Test
    public void withEmptyToken_shouldNotAddHeader() throws Exception {
        TokenProvider provider = () -> "";

        server.enqueue(new MockResponse().setResponseCode(200));

        OkHttpClient client = new OkHttpClient.Builder()
                .addInterceptor(new AuthInterceptor(provider))
                .build();

        client.newCall(new Request.Builder()
                .url(server.url("/api/products"))
                .build()
        ).execute();

        RecordedRequest recorded = server.takeRequest();
        assertNull("No Authorization header expected for empty token",
                recorded.getHeader("Authorization"));
    }
}
