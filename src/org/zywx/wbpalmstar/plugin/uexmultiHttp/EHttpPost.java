package org.zywx.wbpalmstar.plugin.uexmultiHttp;

import android.os.Process;

import org.apache.commons.io.IOUtils;
import org.json.JSONObject;
import org.zywx.wbpalmstar.base.BDebug;
import org.zywx.wbpalmstar.base.BUtility;
import org.zywx.wbpalmstar.platform.certificates.Http;
import org.zywx.wbpalmstar.widgetone.dataservice.WWidgetData;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;
import java.util.zip.GZIPInputStream;

public class EHttpPost extends Thread implements HttpTask, HttpClientListener {
    private String boundary;
    private static final String LINE_FEED = "\r\n";
    private int mTimeOut;
    private boolean mRunning;
    private boolean mCancelled;
    private String mUrl;
    private String mXmlHttpID;
    private EUExXmlHttpMgr mXmlHttpMgr;
    private URL mURL;
    private HttpURLConnection mConnection;
    private String mCertPassword;
    private String mCertPath;
    private InputStream mInStream;
    private boolean mHasLocalCert;
    private String mBody;
    private String mRedirects;
    private File mOnlyFile;
    private ArrayList<HPair> mMultiData;
    private boolean mFromRedirects;
    private Hashtable<String, String> mHttpHead;
    private PrintWriter writer;
    private Map<String, List<String>> headers;

    static final int BODY_TYPE_TEXT = 0;
    static final int BODY_TYPE_FILE = 1;

    private WWidgetData curWData = null;
    private int responseCode = -1;
    private String responseMessage = "";
    private String responseError = "";
    private String charset = HTTPConst.UTF_8;
    private OutputStream outputStream;
    private InputStream mErrorInStream;

    private long mTotalSize = 0;
    private int mUploadedSize = 0;
    private int mOnDataCallbackId;
    private int mOnProgressCallbackId;

    public EHttpPost(String inXmlHttpID, String url, int timeout,
                     EUExXmlHttpMgr xmlHttpMgr) {
        setName("SoTowerMobile-HttpPost");
        mUrl = url;
        mTimeOut = timeout;
        mXmlHttpMgr = xmlHttpMgr;
        mXmlHttpID = inXmlHttpID;
        initNecessaryHeader();
        boundary = UUID.randomUUID().toString();;
    }

    @Override
    public void setData(int inDataType, String inKey, String inValue) {
        if (null == inKey || inKey.length() == 0) {
            inKey = "";
        }
        if (null == inValue || inValue.length() == 0) {
            inValue = "";
        }
        if (null == mMultiData) {
            mMultiData = new ArrayList<HPair>();
        }
        try {
            if (BODY_TYPE_FILE == inDataType) {
                String wp = mXmlHttpMgr.getWidgetPath();
//                int wtp = mXmlHttpMgr.getWidgetType();
//                inValue = BUtility.makeRealPath(inValue, wp, wtp);
                inValue=BUtility.getRealPathWithCopyRes(mXmlHttpMgr.mBrwView,inValue);
            }
            if (checkData(inKey, inValue)) {
                return;
            }
            HPair en = new HPair(inDataType, inKey, inValue);
            mMultiData.add(en);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void setCertificate(String cPassWord, String cPath) {
        mHasLocalCert = true;
        mCertPassword = cPassWord;
        mCertPath = cPath;
    }

    private boolean checkData(String key, String value) {
        for (HPair pair : mMultiData) {
            if (key.equals(pair.key)) {
                pair.value = value;
                return true;
            }
        }
        return false;
    }

    @Override
    public void send() {
        if (mRunning || mCancelled) {
            return;
        }
        mRunning = true;
        start();
    }

    @Override
    public void run() {
        if (mCancelled) {
            return;
        }
        Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);
        doInBackground();
    }

    protected void doInBackground() {
        if (mCancelled) {
            return;
        }
        String result = "";
        boolean isSuccess = false;
        final String curUrl;
        if (null == mUrl) {
            return;
        }
        if (mFromRedirects && null != mRedirects) {
            curUrl = mRedirects;
        } else {
            curUrl = mUrl;
        }
        try {
            mURL = new URL(curUrl);
            if (curUrl.startsWith("https")) {
                if (mHasLocalCert) {
                    mConnection = Http.getHttpsURLConnectionWithCert(mURL, mCertPassword,
                            mCertPath, mXmlHttpMgr.getContext());
                } else {
                    mConnection = Http.getHttpsURLConnection(curUrl);
                }
            } else {
                mConnection = (HttpURLConnection) mURL.openConnection();
            }
            mConnection.setConnectTimeout(mTimeOut);
            mConnection.setUseCaches(false);
            mConnection.setDoOutput(true);
            mConnection.setDoInput(true);
            addHeaders(curUrl);
        } catch (MalformedURLException e) {
            if (BDebug.DEBUG) {
                e.printStackTrace();
            }
        } catch (Exception e) {
            if (BDebug.DEBUG) {
                e.printStackTrace();
            }
        }

        try {

            //设置总大小
            calculateTotalSize();
            if (mTotalSize!=0) {
                mConnection.setChunkedStreamingMode(4096);
            }

            outputStream = mConnection.getOutputStream();
            writer = new PrintWriter(new OutputStreamWriter(outputStream, charset));
            if (null != mOnlyFile) {
                 createInputStemEntity();
            } else if (null != mMultiData) {
                if (!containOctet()) {
                    createFormEntity();
                } else {
                    //含有文件
                    createMultiEntity();
                }
            } else if (null != mBody) {
                writer.write(mBody);
            }

            result=finish(curUrl);
            handleCookie(curUrl);
            isSuccess = true;
        } catch (Exception e) {
            if (BDebug.DEBUG){
                e.printStackTrace();
            }
            isSuccess = false;
            if (e instanceof SocketTimeoutException) {
                result = EUExXmlHttpMgr.CONNECT_FAIL_TIMEDOUT;
            } else {
                result = EUExXmlHttpMgr.CONNECT_FAIL_CONNECTION_FAILURE;
            }
        } finally {
            if (null != mInStream) {
                try {
                    mInStream.close();
                } catch (Exception e) {
                    if (BDebug.DEBUG) {
                        e.printStackTrace();
                    }
                }
            }
            if (mConnection!=null) {
                mConnection.disconnect();
                mConnection=null;
            }

        }
        mXmlHttpMgr.onFinish(mXmlHttpID);
        if (mCancelled) {
            return;
        }
        mXmlHttpMgr.printResult(mXmlHttpID, curUrl, result);
        callbackResult(isSuccess, result);
        return;
    }

    private void callbackResult(boolean isSuccess, String result) {
        if (isSuccess) {
            JSONObject jsonObject = new JSONObject();
            try {
                if (headers != null && !headers.isEmpty()) {
                    JSONObject jsonHeaders = new JSONObject();
                    for (Map.Entry<String, List<String>> header : headers.entrySet()) {
                        if (header.getKey()!=null) {
                            jsonHeaders.put(header.getKey(),
                                    header.getValue());
                        }
                    }
                    jsonObject.put(EUExXmlHttpMgr.PARAMS_JSON_KEY_HEADERS,
                            jsonHeaders);
                }
                jsonObject.put(EUExXmlHttpMgr.PARAMS_JSON_KEY_STATUSCODE,
                        responseCode);
                jsonObject.put(EUExXmlHttpMgr.PARAMS_JSON_KEY_STATUSMESSAGE,
                        responseMessage);
                jsonObject
                        .put(EUExXmlHttpMgr.PARAMS_JSON_KEY_RESPONSEERROR, responseError);
            } catch (Exception e) {
                if (BDebug.DEBUG){
                    e.printStackTrace();
                }
            }
            mXmlHttpMgr.callBack(mXmlHttpID, result, responseCode,
                    jsonObject,mOnDataCallbackId);
        } else {
            mXmlHttpMgr.errorCallBack(mXmlHttpID, result, responseCode, "",mOnDataCallbackId
                    ,mOnProgressCallbackId);
        }
    }

    private String finish(String curUrl) throws Exception {
        String response = "";
        if (mMultiData!=null&&containOctet()) {
            writer.flush();
            writer.append("--" + boundary + "--").append(LINE_FEED);
        }
        writer.close();
        responseCode = mConnection.getResponseCode();
        responseMessage = mConnection.getResponseMessage();
        headers=mConnection.getHeaderFields();
        mXmlHttpMgr.printHeader(responseCode, mXmlHttpID, curUrl, false, headers);
        switch (responseCode) {
            case HttpURLConnection.HTTP_OK:

                byte[] bResult = toByteArray(mConnection);
                response = BUtility.transcoding(new String(bResult, HTTPConst.UTF_8));

                mConnection.disconnect();
                break;
            case HttpURLConnection.HTTP_MOVED_PERM:
            case HttpURLConnection.HTTP_MOVED_TEMP:
            case 307:
                String reUrl = mConnection.getHeaderField("Location");
                if (null != reUrl && reUrl.length() > 0) {
                    mRedirects = reUrl;
                    mFromRedirects = true;
                    handleCookie(curUrl);
                    doInBackground();
                    return "";
                }
                break;
            default:
                byte[] bError = toErrorByteArray(mConnection);
                responseError = BUtility.transcoding(new String(bError, HTTPConst.UTF_8));
                break;
        }
        writer.close();
        return response;
    }

    private byte[] toByteArray(HttpURLConnection conn) throws Exception {
        if (null == conn) {
            return new byte[]{};
        }
        mInStream = conn.getInputStream();
        if (mInStream == null) {
            return new byte[]{};
        }
        long len = conn.getContentLength();
        if (len > Integer.MAX_VALUE) {
            throw new Exception(
                    "HTTP entity too large to be buffered in memory");
        }
        String contentEncoding = conn.getContentEncoding();
        if (null != contentEncoding) {
            if ("gzip".equalsIgnoreCase(contentEncoding)) {
                mInStream = new GZIPInputStream(mInStream, 2048);
            }
        }
        return IOUtils.toByteArray(mInStream);
    }

    private byte[] toErrorByteArray(HttpURLConnection conn) throws Exception {
        if (null == conn) {
            return new byte[]{};
        }
        mErrorInStream = conn.getErrorStream();
        if (mErrorInStream == null) {
            return new byte[]{};
        }
        String contentEncoding = conn.getContentEncoding();
        boolean gzip = false;
        if (null != contentEncoding) {
            if ("gzip".equalsIgnoreCase(contentEncoding)) {
                mErrorInStream = new GZIPInputStream(mErrorInStream, 2048);
            }
        }
        return IOUtils.toByteArray(mErrorInStream);
    }

    private void handleCookie(String url) {

        Map<String,List<String>> map=mConnection.getHeaderFields();
        Set<String> set=map.keySet();
        for (Iterator iterator = set.iterator(); iterator.hasNext();) {
            String key = (String) iterator.next();
            // 由equals改为equalsIgnoreCase，为了兼容不同的服务返回Set-Cookie的各种大小写
            if (HTTPConst.SET_COOKIE.equalsIgnoreCase(key)) {
                List<String> list = map.get(key);

                for (String setCookie : list) {
                    mXmlHttpMgr.setCookie(url, setCookie);
                }
            }
        }
        mXmlHttpMgr.setCookie(url, mConnection.getHeaderField(HTTPConst.COOKIE));
        mXmlHttpMgr.setCookie(url, mConnection.getHeaderField(HTTPConst.COOKIE2));
    }

    private boolean containOctet() {
        for (HPair pair : mMultiData) {
            if (pair.type == BODY_TYPE_FILE) {
                return true;
            }
        }
        return false;
    }

    /**
     * Adds a form field to the request
     *
     * @param name  field name
     * @param value field value
     */
    public void addFormField(String name, String value) {
        writer.append(("--" + boundary)).append(LINE_FEED);
        writer.append("Content-Disposition: form-data; name=\"" + name + "\"")
                .append(LINE_FEED);

        writer.append("Content-Type: text/plain; charset=" + charset).append(
                LINE_FEED);
        writer.append(LINE_FEED);
        writer.append(value).append(LINE_FEED);
        writer.flush();
    }

    public void addFilePart(String fieldName, File uploadFile)
            throws IOException {
        String fileName = uploadFile.getName();
        writer.append("--" + boundary).append(LINE_FEED);
        writer.append(
                "Content-Disposition: form-data; name=\"" + fieldName
                        + "\"; filename=\"" + fileName + "\"")
                .append(LINE_FEED);
        writer.append(
                "Content-Type: "
                        + URLConnection.guessContentTypeFromName(fileName))
                .append(LINE_FEED);
        writer.append("Content-Transfer-Encoding: binary").append(LINE_FEED);
        writer.append(LINE_FEED);
        writer.flush();

        flushOutputStream(uploadFile);
        writer.append(LINE_FEED);
        writer.flush();
    }

    private void flushOutputStream(File uploadFile) throws IOException {
        FileInputStream inputStream = new FileInputStream(uploadFile);
        byte[] buffer = new byte[4096];
        int bytesRead = -1;
        while ((bytesRead = inputStream.read(buffer)) != -1) {
            outputStream.write(buffer, 0, bytesRead);
            mUploadedSize+=bytesRead;
            onProgressChanged(100*(float) mUploadedSize/(float) mTotalSize);
        }
        outputStream.flush();
        inputStream.close();
    }


    private void createFormEntity() {
        try {
            writer.write(getQuery(mMultiData));
        } catch (UnsupportedEncodingException e) {
            if (BDebug.DEBUG) {
                e.printStackTrace();
            }
        }
    }

    private String getQuery(List<HPair> params) throws UnsupportedEncodingException
    {
        StringBuilder result = new StringBuilder();
        boolean first = true;

        for (HPair pair : params)
        {
            if (first)
                first = false;
            else
                result.append("&");

            result.append(URLEncoder.encode(pair.key,charset));
            result.append("=");
            result.append(URLEncoder.encode(pair.value,charset));
        }

        return result.toString();
    }

    private void createInputStemEntity() throws IOException {
        flushOutputStream(mOnlyFile);
    }

    private void calculateTotalSize(){
        if (null != mOnlyFile) {
            mTotalSize=mOnlyFile.length();
        } else if (null != mMultiData) {
            for (HPair pair : mMultiData) {
                if (BODY_TYPE_FILE == pair.type) {
                    File itemFile=new File(pair.value);
                    mTotalSize+=itemFile.length();
                }
            }
        }

    }

    private void createMultiEntity() {
         try {
            for (HPair pair : mMultiData) {
                if (BODY_TYPE_FILE == pair.type) {
                    addFilePart(pair.key,new File(pair.value));
                } else if (BODY_TYPE_TEXT == pair.type) {
                    addFormField(pair.key, pair.value);
                }
            }
        } catch (Exception e) {
        }
    }

    @Override
    public void onProgressChanged(float newProgress) {
        int progress = (int) newProgress;
        mXmlHttpMgr.progressCallBack(mXmlHttpID, progress,mOnProgressCallbackId);
    }

    @Override
    public void cancel() {
        mCancelled = true;
        if (null != mMultiData) {
            mMultiData.clear();
        }
        if (null != mInStream) {
            try {
                mInStream.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        if (null != mConnection) {
            mConnection.disconnect();
            mConnection = null;
        }
        try {
            interrupt();
        } catch (Exception e) {

        }
        mTimeOut = 0;
        mUrl = null;
        mRunning = false;
        mCertPassword = null;
        mCertPath = null;
        mBody = null;
    }


    @Override
    public void setBody(String body) {

        mBody = body;
    }

    @Override
    public void setInputStream(File file) {
        mOnlyFile = file;
    }

    @Override
    public void setHeaders(String headJson) {
        try {
            JSONObject json = new JSONObject(headJson);
            Iterator<?> keys = json.keys();
            while (keys.hasNext()) {
                String key = (String) keys.next();
                String value = json.getString(key);
                mHttpHead.put(key, value);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void addHeaders(String curUrl) {
        String cookie = mXmlHttpMgr.getCookie(curUrl);
        if (null != cookie) {
            mConnection.setRequestProperty(HTTPConst.COOKIE, cookie);
        }
        Set<Entry<String, String>> entrys = mHttpHead.entrySet();
        for (Map.Entry<String, String> entry : entrys) {

            mConnection.setRequestProperty(entry.getKey(), entry.getValue());
        }
        if (null != curWData) {
            mConnection.setRequestProperty(XmlHttpUtil.KEY_APPVERIFY, XmlHttpUtil.getAppVerifyValue(curWData, System.currentTimeMillis()));
            mConnection.setRequestProperty(XmlHttpUtil.XMAS_APPID, curWData.m_appId);
        }
        if (null != mMultiData&&containOctet()){
            mConnection.setRequestProperty("Content-Type","multipart/form-data;boundary="+boundary);
        }
    }

    private void initNecessaryHeader() {
        mHttpHead = new Hashtable<String, String>();
        mHttpHead.put("Accept", "*/*");
        mHttpHead.put("Charset", charset);
        mHttpHead.put("User-Agent", EHttpGet.UA);
        mHttpHead.put("Connection", "Keep-Alive");
        mHttpHead.put("Accept-Encoding", "gzip, deflate");
    }

    @Override
    public void setAppVerifyHeader(WWidgetData curWData) {
        this.curWData = curWData;
    }

    @Override
    public void setCallbackId(int onDataCallbackId, int onProgressCallbackId) {
        this.mOnDataCallbackId=onDataCallbackId;
        this.mOnProgressCallbackId=onProgressCallbackId;
    }
}
