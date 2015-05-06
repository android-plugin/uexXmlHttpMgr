package org.zywx.wbpalmstar.plugin.uexmultiHttp;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import org.apache.http.util.EncodingUtils;


public class BodyFile extends BodyBase {

    public static final String DEFAULT_CONTENT_TYPE = "application/octet-stream";
    public static final String DEFAULT_CHARSET = System.getProperty("file.encoding");
    public static final String DEFAULT_TRANSFER_ENCODING = "binary";
    protected static final String FILE_NAME = "; filename=";
    private static final byte[] FILE_NAME_BYTES = EncodingUtils.getAsciiBytes(FILE_NAME);
    private BodySource mSource;

    public BodyFile(String name, BodySource partSource, String contentType, String charset) {
        
        super(name, contentType == null ? DEFAULT_CONTENT_TYPE : contentType, 
            charset == null ? DEFAULT_CHARSET : charset, DEFAULT_TRANSFER_ENCODING
        );

        if (partSource == null) {
            throw new IllegalArgumentException("Source may not be null");
        }
        mSource = partSource;
    }
        
    public BodyFile(String name, BodySource partSource) {
        this(name, partSource, null, null);
    }

    public BodyFile(String name, File file)throws FileNotFoundException {
        this(name, new BodyFileSource(file), null, null);
    }

    public BodyFile(String name, File file, String contentType, String charset)throws FileNotFoundException {
        this(name, new BodyFileSource(file), contentType, charset);
    }

    public BodyFile(String name, String fileName, File file)throws FileNotFoundException {
        this(name, new BodyFileSource(fileName, file), null, null);
    }

    public BodyFile(String name, String fileName, File file, String contentType, String charset)throws FileNotFoundException {
        this(name, new BodyFileSource(fileName, file), contentType, charset);
    }
 
    @Override
    protected void sendDispositionHeader(OutputStream out)throws IOException {
        super.sendDispositionHeader(out);
        String filename = mSource.getFileName();
        if (filename != null) {
            out.write(FILE_NAME_BYTES);
            out.write(QUOTE_BYTES);
            out.write(EncodingUtils.getAsciiBytes(filename));
            out.write(QUOTE_BYTES);
        }
    }

    @Override
    protected void sendData(OutputStream out, EMultiEntity callback) throws IOException {
        if (lengthOfData() == 0) {
            return;
        }
        long dlen = lengthOfData();
        byte[] tmp;
        if(dlen > (1024 * 1024)){
        	tmp = new byte[8192];
        }else{
        	tmp = new byte[4096];
        }
        InputStream instream = mSource.createInputStream();
        try {
            int len;
            while ((len = instream.read(tmp)) >= 0) {
                out.write(tmp, 0, len);
                callback.onReadLength(len);
            }
        } finally {
            instream.close();
        }
    }

    protected BodySource getSource() {

        return mSource;
    }

    @Override
    protected long lengthOfData() {

        return mSource.getLength();
    }    

}
