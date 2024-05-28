package org.hostile.jtls.cookie;

import com.google.gson.JsonObject;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Cookie {

    private final String name;
    private final String value;

    private String domain;
    private String path;

    private long expires;

    public Cookie(String name, String value) {
        this.name = name;
        this.value = value;
    }

    public JsonObject toJson() {
        JsonObject object = new JsonObject();

        object.addProperty("domain", this.domain);
        object.addProperty("expires", this.expires);

        object.addProperty("name", this.name);
        object.addProperty("path", this.path);
        object.addProperty("value", this.value);

        return object;
    }
}
