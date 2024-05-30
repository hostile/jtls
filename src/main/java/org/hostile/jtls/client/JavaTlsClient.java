package org.hostile.jtls.client;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.hostile.jtls.client.method.Method;
import org.hostile.jtls.config.JavaTlsConfig;
import org.hostile.jtls.cookie.Cookie;
import org.hostile.jtls.cookie.jar.CookieJar;
import org.hostile.jtls.downloader.BinaryDownloader;
import org.hostile.jtls.jna.NativeTlsLibrary;
import org.hostile.jtls.response.Response;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.stream.Collectors;

@Getter @Setter
public class JavaTlsClient {

    private static final Gson GSON = new Gson();

    private static final NativeTlsLibrary TLS_LIBRARY;
    private static final String TLS_VERSION;

    static {
        BinaryDownloader downloader = new BinaryDownloader();

        TLS_LIBRARY = downloader.downloadBinaries();
        TLS_VERSION = downloader.fetchVersionString();
    }

    private final JavaTlsConfig config;

    private final CookieJar cookieJar = new CookieJar();
    private final Map<String, String> headers = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);

    private final UUID sessionId = UUID.randomUUID();

    private int timeoutSeconds = 30;

    public JavaTlsClient(JavaTlsConfig config) {
        this.config = config;

        headers.put("User-Agent", String.format("tls-client/%s", TLS_VERSION));
        headers.put("Accept-Encoding", "gzip, deflate, br");
        headers.put("Accept", "*/*");
        headers.put("Connection", "keep-alive");
    }

    public void close() {
        JsonObject object = new JsonObject();

        object.addProperty("sessionId", this.sessionId.toString());

        TLS_LIBRARY.destroySession(GSON.toJson(object));
    }

    public Response executeRequest(
            @NotNull String method,
            @NotNull String url,
            @Nullable Object data,
            @Nullable Map<String, String> queryParams,
            @Nullable Map<String, String> headers,
            @Nullable List<Cookie> cookies,
            boolean allowRedirects,
            boolean insecureSkipVerify
    ) {
        if (queryParams != null && !queryParams.isEmpty()) {
            url += "?" + queryParams.entrySet().stream()
                    .map((entry) -> entry.getKey() + "=" + entry.getValue()).collect(Collectors.joining("&"));
        }

        if (headers == null) {
            headers = this.headers;
        } else {
            this.headers.forEach(headers::putIfAbsent);
        }

        if (cookies == null) {
            cookies = cookieJar;
        } else {
            cookies.addAll(this.cookieJar);
        }

        boolean isByteRequest = false;

        if (data != null) {
            if (data instanceof JsonElement) {
                headers.put("Content-Type", "application/json");
            } else if (!(data instanceof byte[])) {
                data = data.toString();
            } else {
                isByteRequest = true;
            }
        }

        JsonObject object = new JsonObject();

        object.addProperty("sessionId", this.sessionId.toString());
        object.addProperty("followRedirects", allowRedirects);
        object.addProperty("forceHttp1", config.isForceHttp1());
        object.addProperty("withDebug", config.isDebug());
        object.addProperty("catchPanics", this.config.isHandlePanics());
        object.add("headers", convertMap(headers, String.class));
        object.add("headerOrder", convertList(this.config.getHeaderOrder(), String.class));
        object.addProperty("insecureSkipVerify", insecureSkipVerify);
        object.addProperty("isByteRequest", isByteRequest);
        object.addProperty("additionalDecode", config.getAdditionalDecode());
        object.addProperty("proxyUrl", "");
        object.addProperty("requestUrl", url);
        object.addProperty("requestMethod", method);

        if (data instanceof JsonElement) {
            object.add("requestBody", (JsonElement) data);
        } else {
            object.addProperty("requestBody", isByteRequest ?
                    Base64.getEncoder().encodeToString((byte[]) data) : data == null ? "" : (String) data);
        }

        object.add("requestCookies",
                convertList(cookies.stream().map(Cookie::toJson).collect(Collectors.toList()), JsonElement.class));
        object.addProperty("timeoutSeconds", this.timeoutSeconds);

        if (config.getSslPinning() != null && !config.getSslPinning().isEmpty()) {
            object.add("certificatePinningHosts", convertMapWithList(config.getSslPinning()));
        }

        if (config.getClientIdentifier() != null) {
            JsonObject tlsObject = new JsonObject();

            tlsObject.addProperty("ja3String", config.getJa3String());
            tlsObject.add("h2Settings", convertMap(config.getH2Settings(), Integer.class));
            tlsObject.add("h2SettingsOrder", convertList(config.getH2SettingsOrder(), String.class));
            tlsObject.add("pseudoHeaderOrder", convertList(config.getPseudoHeaderOrder(), String.class));
            tlsObject.addProperty("connectionFlow", config.getConnectionFlow());
            tlsObject.add("priorityFrames", convertList(config.getPriorityFrames(), String.class));
            tlsObject.add("headerPriority", convertList(config.getHeaderPriority(), String.class));
            tlsObject.addProperty("certCompressionAlgo", config.getCertCompressionAlgo());
            tlsObject.add("supportedVersions", convertList(config.getSupportedVersions(), String.class));
            tlsObject.add("supportedSignatureAlgorithms", convertList(config.getSupportedSignatureAlgorithms(), String.class));
            tlsObject.add("supportedDelegatedCredentialsAlgorithms", convertList(config.getSupportedDelegatedCredentialsAlgorithms(), String.class));
            tlsObject.add("keyShareCurves", convertList(config.getKeyShareCurves(), String.class));

            object.add("customTlsClient", tlsObject);
        } else {
            object.addProperty("tlsClientIdentifier", "chrome_120");
            object.addProperty("withRandomTLSExtensionOrder", false);
        }

        JsonObject responseObject = GSON.fromJson(TLS_LIBRARY.request(GSON.toJson(object)), JsonObject.class);

        return new Response(responseObject, this);
    }

    public Request.RequestBuilder create() {
        return Request.builder().handler(this);
    }

    private <T> JsonObject convertMap(Map<String, T> map, Class<T> clazz) {
        JsonObject jsonObject = new JsonObject();

        map.forEach((key, value) -> {
            if (clazz.isAssignableFrom(Integer.class)) {
                jsonObject.addProperty(key, (Integer) value);
            } else if (clazz.isAssignableFrom(String.class)) {
                jsonObject.addProperty(key, (String) value);
            }
        });

        return jsonObject;
    }

    private JsonObject convertMapWithList(Map<String, List<String>> map) {
        JsonObject jsonObject = new JsonObject();

        map.forEach((key, value) -> {
            jsonObject.add(key, this.convertList(value, String.class));
        });

        return jsonObject;
    }

    private <T> JsonArray convertList(List<T> list, Class<T> clazz) {
        JsonArray jsonElements = new JsonArray();

        if (clazz.isAssignableFrom(String.class)) {
            list.forEach((element) -> jsonElements.add((String) element));
        } else if (clazz.isAssignableFrom(JsonElement.class)) {
            list.forEach((element) -> jsonElements.add((JsonElement) element));
        } else {
            throw new IllegalArgumentException(
                    String.format("Class %s is not an assignable type for JsonArray conversion!", clazz.getName()));
        }

        return jsonElements;
    }

    @Builder
    public static class Request {

        private final JavaTlsClient handler;

        private final Method method;
        private final String url;

        private final Object data;

        private final Map<String, String> queryParams;
        private final Map<String, String> headers;
        private final List<Cookie> cookies;

        @Builder.Default
        private boolean allowRedirects = true;

        private final boolean insecureSkipVerify;

        public Response complete() {
            return this.handler.executeRequest(method.getName(), url, data, queryParams, headers, cookies, allowRedirects, insecureSkipVerify);
        }

        public static class RequestBuilder {}
    }
}
