package org.hostile.jtls.response;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.hostile.jtls.client.JavaTlsClient;
import org.hostile.jtls.cookie.Cookie;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Response {

    private final Map<String, List<String>> headers = new HashMap<>();

    private final List<Cookie> cookies = new ArrayList<>();

    private final String body;

    private final int status;

    public Response(JsonObject object, JavaTlsClient client) {
        this.body = new String(object.get("body").getAsString().getBytes(), StandardCharsets.UTF_8);
        this.status = object.get("status").getAsInt();

        object.get("cookies").getAsJsonObject().entrySet().forEach((entry) -> {
            cookies.add(new Cookie(entry.getKey(), entry.getValue().getAsString()));
        });

        object.get("headers").getAsJsonObject().entrySet().forEach((entry) -> {
            List<String> headers = new ArrayList<>();

            entry.getValue().getAsJsonArray().forEach(element -> {
                headers.add(element.getAsString());
            });

            this.headers.put(entry.getKey(), headers);
        });

        if (client.getConfig().isAutoLoadCookiesFromResponse()) {
            this.cookies.forEach(cookie -> client.getCookieJar().add(cookie));
        }
    }

    public String text() {
        return this.body;
    }

    public <T> T json(Class<T> clazz) {
        return new Gson().fromJson(this.body, clazz);
    }

    public int status() {
        return this.status;
    }

    public Map<String, List<String>> headers() {
        return this.headers;
    }

    public List<Cookie> cookies() {
        return this.cookies;
    }
}
