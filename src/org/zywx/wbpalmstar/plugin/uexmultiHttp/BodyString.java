package org.zywx.wbpalmstar.plugin.uexmultiHttp;

import java.io.OutputStream;
import java.io.IOException;
import org.apache.http.util.EncodingUtils;


public class BodyString extends BodyBase {

    public static final String DEFAULT_CONTENT_TYPE = "text/plain";
    public static final String DEFAULT_CHARSET = System.getProperty("file.encoding");
    public static final String DEFAULT_TRANSFER_ENCODING = "8bit";
    private byte[] mContent;
    private String mValue;

    public BodyString(String name, String value, String charset) {
        super(name, DEFAULT_CONTENT_TYPE, charset == null ? DEFAULT_CHARSET : charset,
            DEFAULT_TRANSFER_ENCODING);
        if (value == null) {
            throw new IllegalArgumentException("Value may not be null");
        }
        if (value.indexOf(0) != -1) {
            throw new IllegalArgumentException("NULs may not be present in string parts");
        }
        mValue = value;
    }

    public BodyString(String name, String value) {
        this(name, value, null);
    }

    private byte[] getContent() {
        if (mContent == null) {
            mContent = EncodingUtils.getBytes(mValue, getCharSet());
        }
        return mContent;
    }
 
    @Override
    protected void sendData(OutputStream out, EMultiEntity callback) throws IOException {
        out.write(getContent());
    }

    @Override
    protected long lengthOfData() {
        return getContent().length;
    }

    @Override
    public void setCharSet(String charSet) {
        super.setCharSet(charSet);
        mContent = null;
    }

}
