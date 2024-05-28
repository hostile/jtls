package org.hostile.jtls.client.method;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter @AllArgsConstructor
public enum Method {

    GET("GET"),
    POST("POST"),
    PUT("PUT"),
    PATCH("PATCH"),
    HEAD("HEAD"),
    TRACE("TRACE"),
    DELETE("DELETE");

    private final String name;

}
