/**
 * OpenTok Java SDK
 * Copyright (C) 2018 TokBox, Inc.
 * http://www.tokbox.com
 *
 * Licensed under The MIT License (MIT). See LICENSE file for more information.
 */
package com.opentok.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.opentok.ArchiveProperties;
import com.opentok.constants.DefaultApiUrl;
import com.opentok.constants.Version;
import com.opentok.exception.OpenTokException;
import com.opentok.exception.RequestException;
import org.asynchttpclient.*;
import org.asynchttpclient.Realm.AuthScheme;
import org.asynchttpclient.filter.FilterContext;
import org.asynchttpclient.filter.FilterException;
import org.asynchttpclient.filter.RequestFilter;
import org.asynchttpclient.proxy.ProxyServer;

import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.SocketAddress;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

public class HttpClientAsync extends DefaultAsyncHttpClient {

    private final String apiUrl;
    private final int apiKey;

    private HttpClientAsync(Builder builder) {
        super(builder.config);
        this.apiKey = builder.apiKey;
        this.apiUrl = builder.apiUrl;
    }

    public CompletableFuture<String> createSession(Map<String, Collection<String>> params) {
        Map<String, List<String>> paramsWithList = null;
        if (params != null) {
            paramsWithList = new HashMap<>();
            for (Entry<String, Collection<String>> entry : params.entrySet()) {
                paramsWithList.put(entry.getKey(), new ArrayList<>(entry.getValue()));
            }
        }

        return this.preparePost(this.apiUrl + "/session/create")
                .setFormParams(paramsWithList)
                .addHeader("Accept", "application/json") // XML version is deprecated
                .execute().toCompletableFuture()
                .thenApply(response -> {
                    try {
                        return ClientResponseUtils.parseCreateSessionResponse(response);
                    } catch (RequestException ex) {
                        throw new CompletionException(ex);
                    }
                });
    }

    public CompletableFuture<String> getArchive(final String archiveId) {
        String url = this.apiUrl + "/v2/project/" + this.apiKey + "/archive/" + archiveId;
        return this.prepareGet(url).execute().toCompletableFuture()
                .thenApply(response -> {
                    try {
                        return ClientResponseUtils.parseGetArchiveResponse(archiveId, response);
                    } catch (RequestException ex) {
                        throw new CompletionException(ex);
                    }
                });
    }

    public CompletableFuture<String> getArchives(int offset, int count) {
        String url = this.apiUrl + "/v2/project/" + this.apiKey + "/archive";
        if (offset != 0 || count != 1000) {
            url += "?";
            if (offset != 0) {
                url += ("offset=" + Integer.toString(offset) + '&');
            }
            if (count != 1000) {
                url += ("count=" + Integer.toString(count));
            }
        }

        return this.prepareGet(url).execute().toCompletableFuture()
                .thenApply(response -> {
                    try {
                        return ClientResponseUtils.parseGetArchivesResponse(response);
                    } catch (RequestException ex) {
                        throw new CompletionException(ex);
                    }
                });
    }

    public CompletableFuture<String> getArchives(String sessionId) {
        String url = this.apiUrl + "/v2/project/" + this.apiKey + "/archive?sessionId=" + sessionId;

        return this.prepareGet(url).execute().toCompletableFuture()
                .thenApply(response -> {
                    try {
                        return ClientResponseUtils.parseGetArchivesResponse(response);
                    } catch (RequestException ex) {
                        throw new CompletionException(ex);
                    }
                });
    }

    public CompletableFuture<String> startArchive(String sessionId, ArchiveProperties properties) {
        String requestBody = null;
        String url = this.apiUrl + "/v2/project/" + this.apiKey + "/archive";

        JsonNodeFactory nodeFactory = JsonNodeFactory.instance;
        ObjectNode requestJson = nodeFactory.objectNode();
        requestJson.put("sessionId", sessionId);
        requestJson.put("hasVideo", properties.hasVideo());
        requestJson.put("hasAudio", properties.hasAudio());
        requestJson.put("outputMode", properties.outputMode().toString());
        if (properties.layout() != null) {
            ObjectNode layout = requestJson.putObject("layout");
            layout.put("type", properties.layout().getType().toString());
            layout.put("stylesheet", properties.layout().getStylesheet());
        }
        if (properties.name() != null) {
            requestJson.put("name", properties.name());
        }
        try {
            requestBody = new ObjectMapper().writeValueAsString(requestJson);
        } catch (JsonProcessingException e) {
            CompletableFuture<String> future = new CompletableFuture<>();
            future.completeExceptionally(new OpenTokException("Could not start an OpenTok Archive. The JSON body encoding failed.", e));
            return future;
        }
        return this.preparePost(url)
                .setBody(requestBody)
                .setHeader("Content-Type", "application/json")
                .execute().toCompletableFuture()
                .thenApply(response -> {
                    try {
                        return ClientResponseUtils.parseStartArchiveResponse(sessionId, response);
                    } catch (RequestException ex) {
                        throw new CompletionException(ex);
                    }
                });
    }

    public CompletableFuture<String> stopArchive(String archiveId) {
        String url = this.apiUrl + "/v2/project/" + this.apiKey + "/archive/" + archiveId + "/stop";
        return this.preparePost(url).execute().toCompletableFuture()
                .thenApply(response -> {
                    try {
                        return ClientResponseUtils.parseStopArchiveResponse(archiveId, response);
                    } catch (RequestException ex) {
                        throw new CompletionException(ex);
                    }
                });
    }

    public CompletableFuture<String> deleteArchive(String archiveId) {
        String url = this.apiUrl + "/v2/project/" + this.apiKey + "/archive/" + archiveId;
        return this.prepareDelete(url).execute().toCompletableFuture()
                .thenApply(response -> {
                    try {
                        return ClientResponseUtils.parseDeleteArchiveResponse(archiveId, response);
                    } catch (RequestException ex) {
                        throw new CompletionException(ex);
                    }
                });
    }

    public static enum ProxyAuthScheme {
        BASIC,
        DIGEST,
        NTLM,
        SPNEGO,
        KERBEROS
    }

    public static class Builder {
        private final int apiKey;
        private final String apiSecret;
        private Proxy proxy;
        private ProxyAuthScheme proxyAuthScheme;
        private String principal;
        private String password;
        private String apiUrl;
        private AsyncHttpClientConfig config;

        public Builder(int apiKey, String apiSecret) {
            this.apiKey = apiKey;
            this.apiSecret = apiSecret;
        }

        public Builder apiUrl(String apiUrl) {
            this.apiUrl = apiUrl;
            return this;
        }

        public Builder proxy(Proxy proxy) {
            proxy(proxy, null, null, null);
            return this;
        }

        public Builder proxy(Proxy proxy, ProxyAuthScheme proxyAuthScheme, String principal, String password) {
            this.proxy = proxy;
            this.proxyAuthScheme = proxyAuthScheme;
            this.principal = principal;
            this.password = password;
            return this;
        }

        public HttpClientAsync build() {
            DefaultAsyncHttpClientConfig.Builder configBuilder = new DefaultAsyncHttpClientConfig.Builder()
                    .setUserAgent("Opentok-Java-SDK/" + Version.VERSION + " JRE/" + System.getProperty("java.version"))
                    .addRequestFilter(new TokenAuthRequestFilter(this.apiKey, this.apiSecret));
            if (this.apiUrl == null) {
                this.apiUrl = DefaultApiUrl.DEFAULT_API_URI;
            }

            if (this.proxy != null) {
                configBuilder.setProxyServer(createProxyServer(this.proxy, this.proxyAuthScheme, this.principal, this.password));
            }

            this.config = configBuilder.build();
            // NOTE: not thread-safe, config could be modified by another thread here?
            HttpClientAsync client = new HttpClientAsync(this);
            return client;
        }

        // credit: https://github.com/AsyncHttpClient/async-http-client/blob/b52a8de5d6a862b5d1652d62f87ce774cbcff156/src/main/java/com/ning/http/client/ProxyServer.java#L99-L127
        static ProxyServer createProxyServer(final Proxy proxy, ProxyAuthScheme proxyAuthScheme, String principal, String password) {
            switch (proxy.type()) {
                case DIRECT:
                    return null;
                case SOCKS:
                    throw new IllegalArgumentException("Only DIRECT and HTTP Proxies are supported!");
            }

            final SocketAddress sa = proxy.address();

            if (!(sa instanceof InetSocketAddress)) {
                throw new IllegalArgumentException("Only Internet Address sockets are supported!");
            }

            InetSocketAddress isa = (InetSocketAddress) sa;

            final String isaHost = isa.isUnresolved() ? isa.getHostName() : isa.getAddress().getHostAddress();
            ProxyServer.Builder builder = new ProxyServer.Builder(isaHost, isa.getPort());

            if (principal != null) {
                AuthScheme authScheme = null;
                switch (proxyAuthScheme) {
                    case BASIC:
                        authScheme = AuthScheme.BASIC;
                        break;
                    case DIGEST:
                        authScheme = AuthScheme.DIGEST;
                        break;
                    case NTLM:
                        authScheme = AuthScheme.NTLM;
                        break;
                    case KERBEROS:
                        authScheme = AuthScheme.KERBEROS;
                        break;
                    case SPNEGO:
                        authScheme = AuthScheme.SPNEGO;
                        break;
                }

                Realm.Builder rb = new Realm.Builder(principal, password);
                rb.setScheme(authScheme);

                builder.setRealm(rb.build());
            }

            return builder.build();
        }
    }

    static class TokenAuthRequestFilter implements RequestFilter {

        private final int apiKey;
        private final String apiSecret;
        private final String authHeader = "X-OPENTOK-AUTH";

        public TokenAuthRequestFilter(int apiKey, String apiSecret) {
            this.apiKey = apiKey;
            this.apiSecret = apiSecret;
        }

        @Override
        public <T> FilterContext<T> filter(FilterContext<T> ctx) throws FilterException {
            try {
                return new FilterContext.FilterContextBuilder<T>(ctx)
                        .request(new RequestBuilder(ctx.getRequest())
                                .addHeader(authHeader, TokenGenerator.generateToken(apiKey, apiSecret))
                                .build())
                        .build();
            } catch (OpenTokException e) {
                e.printStackTrace();
                return null;
            }
        }
    }
}
