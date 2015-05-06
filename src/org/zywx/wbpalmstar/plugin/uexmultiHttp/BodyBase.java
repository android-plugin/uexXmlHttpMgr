package org.zywx.wbpalmstar.plugin.uexmultiHttp;


public abstract class BodyBase extends Body {

    private String mName;
    private String mContentType;
    private String mCharSet;
    private String mTransferEncoding;

 
    public BodyBase(String name, String contentType, String charSet, String transferEncoding) {
        if (name == null) {
            throw new IllegalArgumentException("Name must not be null");
        }
        mName = name;
        mContentType = contentType;
        mCharSet = charSet;
        mTransferEncoding = transferEncoding;
    }

    @Override
    public String getName() { 
        return mName; 
    }

    @Override
    public String getContentType() {
        return mContentType;
    }

    @Override
    public String getCharSet() {
        return mCharSet;
    }

    @Override
    public String getTransferEncoding() {
        return mTransferEncoding;
    }

    public void setCharSet(String charSet) {
        mCharSet = charSet;
    }

    public void setContentType(String contentType) {
        mContentType = contentType;
    }

    public void setName(String name) {
        if (name == null) {
            throw new IllegalArgumentException("Name must not be null");
        }
        mName = name;
    }

    public void setTransferEncoding(String transferEncoding) {
        mTransferEncoding = transferEncoding;
    }

}
