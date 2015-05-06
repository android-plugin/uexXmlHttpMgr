package org.zywx.wbpalmstar.plugin.uexmultiHttp;

import java.io.IOException;
import java.io.InputStream;


public interface BodySource {
    long getLength();
    String getFileName();
    InputStream createInputStream() throws IOException;

}
