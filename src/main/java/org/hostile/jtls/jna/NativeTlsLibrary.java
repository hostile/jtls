package org.hostile.jtls.jna;

import com.sun.jna.Library;
import com.sun.jna.win32.StdCallLibrary;

public interface NativeTlsLibrary extends Library {

    String request(String data);

    String freeMemory(String data);

    String destroySession(String data);
}
