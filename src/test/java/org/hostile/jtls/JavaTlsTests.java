package org.hostile.jtls;

import org.hostile.jtls.client.JavaTlsClient;
import org.hostile.jtls.client.method.Method;
import org.hostile.jtls.config.JavaTlsConfig;
import org.hostile.jtls.downloader.BinaryDownloader;
import org.hostile.jtls.jna.NativeTlsLibrary;
import org.junit.jupiter.api.Test;

public class JavaTlsTests {

    @Test
    public void testBinaryLoading() {
        BinaryDownloader downloader = new BinaryDownloader();
        downloader.downloadBinaries();
    }

    @Test
    public void testNetworkRequest() {
        JavaTlsClient client = new JavaTlsClient(JavaTlsConfig.builder().build());

        client.create().method(Method.GET).url("https://hostile.org").build().complete();
    }
}
