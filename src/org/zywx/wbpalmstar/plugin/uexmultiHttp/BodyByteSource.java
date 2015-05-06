package org.zywx.wbpalmstar.plugin.uexmultiHttp;

import java.io.ByteArrayInputStream;
import java.io.InputStream;


public class BodyByteSource implements BodySource {

    private String mFileName;
    private byte[] mBytes;
    public BodyByteSource(String fileName, byte[] bytes) {
        mFileName = fileName;
        mBytes = bytes;
    }

    public long getLength() {
        return mBytes.length;
    }

    public String getFileName() {
        return mFileName;
    }

    public InputStream createInputStream() {
        return new ByteArrayInputStream(mBytes);
    }

}
