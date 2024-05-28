package org.hostile.jtls.config;

import lombok.Builder;
import lombok.Getter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Getter
@Builder
public class JavaTlsConfig {

    private final String clientIdentifier;
    private final String ja3String;

    private Map<String, Integer> h2Settings;
    private List<String> h2SettingsOrder;

    private List<String> supportedSignatureAlgorithms;
    private List<String> supportedDelegatedCredentialsAlgorithms;
    private List<String> supportedVersions;
    private List<String> keyShareCurves;

    private String certCompressionAlgo;
    private String additionalDecode;

    private List<String> pseudoHeaderOrder;
    private Integer connectionFlow;

    private List<String> priorityFrames;

    @Builder.Default
    private List<String> headerOrder = new ArrayList<>();

    private List<String> headerPriority;
    private boolean randomTlsOrder;

    private boolean forceHttp1;
    private boolean handlePanics;
    private boolean debug;

    @Builder.Default
    private boolean autoLoadCookiesFromResponse = true;

    private Map<String, List<String>> sslPinning;

    @Builder.Default
    private String proxy = "";
}
