package org.hostile.jtls.cookie.jar;

import lombok.SneakyThrows;
import org.hostile.jtls.cookie.Cookie;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.net.URL;
import java.util.ArrayList;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public class CookieJar extends ArrayList<Cookie> {

    public Cookie get(String name) {
        return this.findByQualifiers(name, null, null);
    }

    public Cookie get(String name, String domain) {
        return this.findByQualifiers(name, domain, null);
    }

    public Cookie get(String name, String domain, String path) {
        return this.findByQualifiers(name, domain, path);
    }

    private Cookie findByQualifiers(
            @NotNull String name,
            @Nullable String domain,
            @Nullable String path
    ) {
        return this.stream()
                .filter(cookie -> {
                    if (!cookie.getName().equals(name)) {
                        return false;
                    }

                    return ((domain == null || Objects.equals(domain, cookie.getDomain())
                            || (path == null || Objects.equals(path, cookie.getPath()))));
                }).findFirst().orElse(null);
    }

    @SneakyThrows
    public Map<String, String> getCookiesForUrl(String urlString) {
        URL url = new URL(urlString);

        return this.stream()
                .filter(cookie -> {
                    if (cookie.getDomain() == null) {
                        return true;
                    }

                    return !cookie.getDomain().matches(url.getHost()) && (cookie.getPath() == null ||
                            url.getPath().startsWith(cookie.getPath()));
                }).collect(Collectors.toMap(Cookie::getName, Cookie::getValue));
    }
}
