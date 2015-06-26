package org.zywx.wbpalmstar.plugin.uexmultiHttp;

import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPInputStream;

import javax.net.ssl.HttpsURLConnection;

import org.apache.http.HttpEntity;
import org.apache.http.HttpStatus;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.cookie.SM;
import org.apache.http.entity.StringEntity;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.ByteArrayBuffer;
import org.json.JSONObject;
import org.zywx.wbpalmstar.base.BUtility;
import org.zywx.wbpalmstar.widgetone.dataservice.WWidgetData;

import android.os.Process;

public class ECusHttpPost extends Thread implements HttpTask,
		HttpClientListener {

	static final int F_SHEM_ID_HTTP = 0;
	static final int F_SHEM_ID_HTTPS = 1;

	static final int BODY_TYPE_TEXT = 0;
	static final int BODY_TYPE_FILE = 1;

	static final String UA = "Mozilla/5.0 (Windows NT 5.1; rv:12.0) Gecko/20100101 Firefox/12.0";

	private int mTimeOut;
	private boolean mRunning;
	private boolean mCancelled;
	private String mUrl;
	private int mXmlHttpID;
	private EUExXmlHttpMgr mXmlHttpMgr;
	private URL mClient;
	private HttpURLConnection mConnection;
	private ArrayList<HPair> mFormData;
	private String mCertPassword;
	private String mCertPath;
	private InputStream mInStream;
	private DataOutputStream mOutStream;
	private boolean mHasLocalCert;
	private String mHeaders;
	private String mBody;
	private String mRedirects;
	private int mShemeId;
	private boolean mFromRedirects;

	public ECusHttpPost(String inXmlHttpID, String url, int timeout,
			EUExXmlHttpMgr xmlHttpMgr) {
		mUrl = url;
		mTimeOut = timeout;
		mXmlHttpMgr = xmlHttpMgr;
		mXmlHttpID = Integer.parseInt(inXmlHttpID);
		mShemeId = url.startsWith("https") ? F_SHEM_ID_HTTPS : F_SHEM_ID_HTTP;
		setName("SoTowerMobile-HttpPost");
	}

	@Override
	public void setData(int inDataType, String inKey, String inValue) {
		if (null == inKey || inKey.length() == 0) {
			inKey = "";
		}
		if (null == inValue || inValue.length() == 0) {
			inValue = "";
		}
		if (null == mFormData) {
			mFormData = new ArrayList<HPair>();
		}
		try {
			if (BODY_TYPE_FILE == inDataType) {
				String wp = mXmlHttpMgr.getWidgetPath();
				int wtp = mXmlHttpMgr.getWidgetType();
				inValue = BUtility.makeRealPath(inValue, wp, wtp);
			}
			if (checkData(inKey, inValue)) {
				return;
			}
			HPair en = new HPair(inDataType, inKey, inValue);
			mFormData.add(en);
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
		for (HPair pair : mFormData) {
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

	boolean run = false;
	private int responseCode = -1;
	private String responseMessage = "";
	private String responseError = "";
	private Map<String, List<String>> headers;
	private InputStream mErrorInStream;

	protected void doInBackground() {
		if (mCancelled) {
			return;
		}
		String result = "";
		final String curUrl;
		if (null == mUrl) {
			return;
		}
		if (mFromRedirects && null != mRedirects) {
			curUrl = mRedirects;
		} else {
			curUrl = mUrl;
		}
		boolean isSuccess = false;
		boolean https = false;
		HttpEntity multiEn = null;
		if (null != mFormData) {
			if (!containOctet()) {
				multiEn = createFormEntity();
			} else {
				multiEn = createMultiEntity();
				if (null == multiEn) {
//					mXmlHttpMgr.callBack(mXmlHttpID, "error:file not found!");
				}
			}
		} else if (null != mBody) {
			multiEn = createStringEntity();
		}
		try {
			mClient = new URL(curUrl);
			switch (mShemeId) {
			case F_SHEM_ID_HTTP:
				mConnection = (HttpURLConnection) mClient.openConnection();
				break;
			case F_SHEM_ID_HTTPS:
				mConnection = (HttpsURLConnection) mClient.openConnection();
				javax.net.ssl.SSLSocketFactory ssFact = null;
				if (mHasLocalCert) {
					ssFact = Http.getSSLSocketFactoryWithCert(mCertPassword,
							mCertPath, mXmlHttpMgr.getContext());
				} else {
					ssFact = Http.getSSLSocketFactory();
				}
				((HttpsURLConnection) mConnection).setSSLSocketFactory(ssFact);
				((HttpsURLConnection) mConnection)
						.setHostnameVerifier(new HX509HostnameVerifier());
				https = true;
				break;
			}
			String cookie = null;
			cookie = mXmlHttpMgr.getCookie(curUrl);
			if (null != cookie) {
				mConnection.setRequestProperty(SM.COOKIE, cookie);
			}
			mConnection.setRequestMethod("POST");
			mConnection.setRequestProperty("Accept-Encoding", "gzip, deflate");
			mConnection.setUseCaches(false);
			mConnection.setDoInput(true);
			mConnection.setDoOutput(true);
			mConnection.setRequestProperty("Connection", "Keep-Alive");
			mConnection.setRequestProperty("Charset", HTTP.UTF_8);
			mConnection.setRequestProperty("User-Agent", UA);
			mConnection.setReadTimeout(mTimeOut);
			mConnection.setConnectTimeout(mTimeOut);
			mConnection.setInstanceFollowRedirects(false);
			addHeaders();
			mXmlHttpMgr.printHeader(-1, mXmlHttpID, curUrl, true,
					mConnection.getRequestProperties());
			mConnection.connect();
			mOutStream = new DataOutputStream(mConnection.getOutputStream());
			multiEn.writeTo(mOutStream);
			responseCode = mConnection.getResponseCode();
			responseMessage = mConnection.getResponseMessage();
			headers = mConnection.getHeaderFields();
			mXmlHttpMgr.printHeader(responseCode, mXmlHttpID, curUrl, false,
					headers);
			switch (responseCode) {
			case HttpStatus.SC_OK:
				byte[] bResult = toByteArray(mConnection);
				result = new String(bResult, HTTP.UTF_8);
				break;
			case HttpStatus.SC_MOVED_PERMANENTLY:
			case HttpStatus.SC_MOVED_TEMPORARILY:
			case HttpStatus.SC_TEMPORARY_REDIRECT:
				List<String> urls = headers.get("Location");
				if (null != urls && urls.size() > 0) {
					mRedirects = urls.get(0);
					mFromRedirects = true;
					handleCookie(curUrl, headers);
					doInBackground();
					return;
				}
				break;
			default:
				byte[] bError = toErrorByteArray(mConnection);
				responseError = new String(bError, HTTP.UTF_8);
				break;
			}
			handleCookie(curUrl, headers);
			isSuccess = true;
		} catch (Exception e) {
			isSuccess = false;
			if ((e instanceof IOException) && https) {
				result = EUExXmlHttpMgr.CONNECT_FAIL_AUTHENTICATION;
			} else if (e instanceof SocketTimeoutException) {
				result = EUExXmlHttpMgr.CONNECT_FAIL_TIMEDOUT;
			} else {
				result = EUExXmlHttpMgr.CONNECT_FAIL_CONNECTION_FAILURE;
			}
		} finally {
			try {
				if (null != mConnection) {
					mConnection.disconnect();
				}
			} catch (Exception e) {
			}
		}
		mXmlHttpMgr.onFinish(mXmlHttpID);
		if (mCancelled) {
			return;
		}
		mXmlHttpMgr.printResult(mXmlHttpID, curUrl, result);
		if (isSuccess) {
			JSONObject jsonObject = new JSONObject();
			try {
				if (headers != null && !headers.isEmpty()) {
					JSONObject jsonHeaders = XmlHttpUtil
							.getJSONHeaders(headers);
					jsonObject.put(EUExXmlHttpMgr.PARAMS_JSON_KEY_HEADERS,
							jsonHeaders);
				}
				jsonObject.put(EUExXmlHttpMgr.PARAMS_JSON_KEY_STATUSCODE,
						responseCode);
				jsonObject.put(EUExXmlHttpMgr.PARAMS_JSON_KEY_STATUSMESSAGE,
						responseMessage);
				jsonObject.put(EUExXmlHttpMgr.PARAMS_JSON_KEY_RESPONSEERROR,
						responseError);
			} catch (Exception e) {
			}
			mXmlHttpMgr.callBack(mXmlHttpID, result, responseCode,
					jsonObject.toString());
		} else {
			mXmlHttpMgr.errorCallBack(mXmlHttpID, result, responseCode, "");
		}
		return;
	}

	private void handleCookie(String url, Map<String, List<String>> headers) {
		if (null == headers) {
			return;
		}
		List<String> setCookies = headers.get(SM.SET_COOKIE);
		if (null != setCookies) {
			for (String v : setCookies) {
				mXmlHttpMgr.setCookie(url, v);
			}
		}
		List<String> Cookie = headers.get(SM.COOKIE);
		if (null != Cookie) {
			for (String v : Cookie) {
				mXmlHttpMgr.setCookie(url, v);
			}
		}
		List<String> Cookie2 = headers.get(SM.COOKIE2);
		if (null != Cookie2) {
			for (String v : Cookie2) {
				mXmlHttpMgr.setCookie(url, v);
			}
		}
	}

	private boolean containOctet() {
		for (HPair pair : mFormData) {
			if (pair.type == BODY_TYPE_FILE) {
				return true;
			}
		}
		return false;
	}

	private HttpEntity createFormEntity() {
		HttpEntity entry = null;
		try {
			List<BasicNameValuePair> postData = new ArrayList<BasicNameValuePair>();
			for (HPair pair : mFormData) {
				postData.add(new BasicNameValuePair(pair.key, pair.value));
			}
			entry = new UrlEncodedFormEntity(postData, HTTP.UTF_8);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return entry;
	}

	private HttpEntity createMultiEntity() {
		EMultiEntity multiEn = null;
		try {
			multiEn = new EMultiEntity();
			multiEn.addHttpClientListener(this);
			for (HPair pair : mFormData) {
				Body bd = null;
				if (BODY_TYPE_FILE == pair.type) {
					bd = new BodyFile(pair.key, new File(pair.value));
				} else if (BODY_TYPE_TEXT == pair.type) {
					bd = new BodyString(pair.key, pair.value);
				}
				multiEn.addBody(bd);
			}
		} catch (Exception e) {

			return null;
		}
		return multiEn;
	}

	private HttpEntity createStringEntity() {
		HttpEntity entry = null;
		try {
			entry = new StringEntity(mBody, HTTP.UTF_8);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return entry;
	}

	@Override
	public void onProgressChanged(float newProgress) {
		int progress = (int) newProgress;
		mXmlHttpMgr.progressCallBack(mXmlHttpID, progress);
	}

	@Override
	public void cancel() {
		mCancelled = true;
		if (null != mFormData) {
			mFormData.clear();
		}
		if (null != mInStream) {
			try {
				mInStream.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		if (null != mErrorInStream) {
			try {
				mErrorInStream.close();
			} catch (Exception e) {
			}
		}
		try {
			interrupt();
		} catch (Exception e) {
			;
		}
		mTimeOut = 0;
		mUrl = null;
		mRunning = false;
		mCertPassword = null;
		mCertPath = null;
		mHeaders = null;
		mBody = null;
	}

	private byte[] toByteArray(HttpURLConnection conn) throws Exception {
		if (null == conn) {
			return new byte[] {};
		}
		mInStream = conn.getInputStream();
		if (mInStream == null) {
			return new byte[] {};
		}
		long len = conn.getContentLength();
		if (len > Integer.MAX_VALUE) {
			throw new Exception(
					"HTTP entity too large to be buffered in memory");
		}
		String contentEncoding = conn.getContentEncoding();
		boolean gzip = false;
		if (null != contentEncoding) {
			if ("gzip".equalsIgnoreCase(contentEncoding)) {
				mInStream = new GZIPInputStream(mInStream, 2048);
				gzip = true;
			}
		}
		ByteArrayBuffer buffer = XmlHttpUtil.getBuffer(gzip, mInStream);
		return buffer.toByteArray();
	}
	
	private byte[] toErrorByteArray(HttpURLConnection conn) throws Exception {
		if (null == conn) {
			return new byte[] {};
		}
		mErrorInStream = conn.getErrorStream();
		if (mErrorInStream == null) {
			return new byte[] {};
		}
		String contentEncoding = conn.getContentEncoding();
		boolean gzip = false;
		if (null != contentEncoding) {
			if ("gzip".equalsIgnoreCase(contentEncoding)) {
				mErrorInStream = new GZIPInputStream(mErrorInStream, 2048);
				gzip = true;
			}
		}
		ByteArrayBuffer buffer = XmlHttpUtil.getBuffer(gzip, mErrorInStream);
		return buffer.toByteArray();
	}

	@Override
	public void setHeaders(String headJson) {
		mHeaders = headJson;
	}

	@Override
	public void setBody(String body) {
		mBody = body;
	}

	private void addHeaders() {
		if (null != mHeaders) {
			try {
				JSONObject json = new JSONObject(mHeaders);
				Iterator<?> keys = json.keys();
				while (keys.hasNext()) {
					String key = (String) keys.next();
					String value = json.getString(key);
					mConnection.setRequestProperty(key, value);
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	@Override
	public void setInputStream(File file) {

	}

	@Override
	public void setAppVerifyHeader(WWidgetData curWData) {
		mConnection.setRequestProperty(
				XmlHttpUtil.KEY_APPVERIFY,
				XmlHttpUtil.getAppVerifyValue(curWData,
						System.currentTimeMillis()));
	}
}
