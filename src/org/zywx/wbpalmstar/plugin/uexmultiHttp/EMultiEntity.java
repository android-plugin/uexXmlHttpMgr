package org.zywx.wbpalmstar.plugin.uexmultiHttp;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import org.apache.http.Header;
import org.apache.http.entity.AbstractHttpEntity;
import org.apache.http.message.BasicHeader;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EncodingUtils;


public class EMultiEntity extends AbstractHttpEntity {

    private static final String MULTIPART_FORM_CONTENT_TYPE = "multipart/form-data";
    public static final String MULTIPART_BOUNDARY = "http.method.multipart.boundary";
    private static byte[] MULTIPART_CHARS = EncodingUtils.getAsciiBytes("-_1234567890abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ");
    private List<Body> mBodys = new ArrayList<Body>();
    private byte[] mMultipartBoundary;
    private HttpParams mHttpParams;
    private boolean mContentConsumed;
    private HttpClientListener mClientListener;
    private long mContentLenth;
    private long mCount;
    
 
    public EMultiEntity(Body body, HttpParams params) {      
      if (body == null) {
          throw new IllegalArgumentException("parts cannot be null");
      }
      if (params == null) {
          throw new IllegalArgumentException("params cannot be null");
      }
      mBodys.add(body);
      mHttpParams = params;
    }
    
    public EMultiEntity(Body body) {
      setContentType(MULTIPART_FORM_CONTENT_TYPE);
      if (body == null) {
          throw new IllegalArgumentException("parts cannot be null");
      }
      mBodys.add(body);
    }
    
    public EMultiEntity(){
    	setContentType(MULTIPART_FORM_CONTENT_TYPE);
    }
    
    public void addBody(Body body){
    	if(null != body){
    		mBodys.add(body);
    	}
    }

    protected byte[] getMultipartBoundary() {
        if (mMultipartBoundary == null) {
            String temp = null;
            if (mHttpParams != null) {
              temp = (String) mHttpParams.getParameter(MULTIPART_BOUNDARY);
            }
            if (temp != null) {
                mMultipartBoundary = EncodingUtils.getAsciiBytes(temp);
            } else {
                mMultipartBoundary = generateMultipartBoundary();
            }
        }
        return mMultipartBoundary;
    }
    
    private static byte[] generateMultipartBoundary() {
        Random rand = new Random();
        byte[] bytes = new byte[rand.nextInt(11) + 30]; // a random size from 30 to 40
        for (int i = 0; i < bytes.length; i++) {
            bytes[i] = MULTIPART_CHARS[rand.nextInt(MULTIPART_CHARS.length)];
        }
        return bytes;
    }

    public boolean isRepeatable() {
        for (int i = 0; i < mBodys.size(); i++) {
            if (!mBodys.get(i).isRepeatable()) {
                return false;
            }
        }
        return true;
    }

    public void writeTo(OutputStream out) throws IOException {
    	mContentLenth = getContentLength();
        Body.sendParts(out, mBodys, getMultipartBoundary(), this);
        if(null != mClientListener){
        	mClientListener.onProgressChanged(100);
        }
    }
 
    @Override
    public Header getContentType() {
      StringBuffer buffer = new StringBuffer(MULTIPART_FORM_CONTENT_TYPE);
      buffer.append("; boundary=");
      buffer.append(EncodingUtils.getAsciiString(getMultipartBoundary()));
      return new BasicHeader(HTTP.CONTENT_TYPE, buffer.toString());

    }


    public long getContentLength() {
        try {
            return Body.getLengthOfParts(mBodys, getMultipartBoundary());            
        } catch (Exception e) {
           
            return 0;
        }
    }    
 
    public InputStream getContent() throws IOException, IllegalStateException {
          if(!isRepeatable() && mContentConsumed ) {
              throw new IllegalStateException("Content has been consumed");
          }
          mContentConsumed = true;
          
          ByteArrayOutputStream baos = new ByteArrayOutputStream();
          Body.sendParts(baos, mBodys, mMultipartBoundary, this);
          ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
          return bais;
    }
  
    public boolean isStreaming() {
        return false;
    }
    
    public void addHttpClientListener(HttpClientListener inList){
    	mClientListener = inList;
    }
    
    public void onReadLength(long newLength){
    	if(null != mClientListener){
    		mCount += newLength;
    		float per = ((float)mCount / mContentLenth) * 100;
    		mClientListener.onProgressChanged(per);
    	}
    }
}
