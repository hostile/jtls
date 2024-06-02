package org.hostile.jtls.downloader;

import com.sun.jna.Native;
import com.sun.jna.NativeLibrary;
import lombok.SneakyThrows;
import org.hostile.jtls.jna.NativeTlsLibrary;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;

public class BinaryDownloader {

    private static final String REPOSITORY_RELEASES_URL = "https://github.com/bogdanfinn/tls-client/releases";
    private final String releaseString;

    public BinaryDownloader() {
        this.releaseString = fetchVersionString();
    }

    public BinaryDownloader(String releaseString) {
        this.releaseString = "v" + releaseString;
    }

    @SneakyThrows
    public String fetchVersionString() {
        Document document = Jsoup.connect(REPOSITORY_RELEASES_URL).get();

        return document.select("a[class=\"Link--primary Link\"]").get(0).text();
    }

    @SneakyThrows
    public NativeTlsLibrary downloadBinaries() {
        File directory = new File("./bin/");

        if (!directory.isDirectory()) {
            directory.mkdir();
        }

        String systemArchitecture = System.getProperty("os.arch");
        String normalizedPlatform = getPlatform();

        String architectureTag = !normalizedPlatform.equals("windows") && (systemArchitecture.equals("aarch64") || systemArchitecture.equals("arm64")) ?
                "arm64" : systemArchitecture.replaceAll("[^0-9]", "");

        return Jsoup.connect(String.format(REPOSITORY_RELEASES_URL + "/expanded_assets/%s", this.releaseString)).get()
                .select("a")
                .stream()
                .filter(element -> element.attr("href") != null &&
                        element.attr("href").contains(normalizedPlatform) && element.attr("href").contains(architectureTag))
                .map(this::downloadAndInject)
                .filter(Objects::nonNull)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Failed to load native library!"));
    }

    private String getPlatform() {
        String platform = System.getProperty("os.name").toLowerCase();

        if (platform.contains("windows")) {
            return "windows";
        } else if (platform.contains("darwin") || platform.contains("mac")) {
            return "darwin";
        } else {
            return "linux";
        }
    }

    @SneakyThrows
    private NativeTlsLibrary downloadAndInject(Element element) {
        String fileName = element.attr("href").split(this.releaseString + "/")[1];
        Path path = Paths.get(String.format("bin/%s", fileName));

        NativeLibrary.addSearchPath(fileName, path.toFile().getAbsolutePath());

        if (!path.toFile().isFile()) {
            InputStream in = new URL(String.format(
                    "https://github.com%s", element.attr("href")
            )).openStream();

            Files.copy(in, path);
        }

        try {
            return Native.load(path.toFile().getAbsolutePath(), NativeTlsLibrary.class);
        } catch (Exception exc) {
            return null;
        }
    }
}
