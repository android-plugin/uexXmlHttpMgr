package org.zywx.wbpalmstar.plugin.uexmultiHttp;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.zip.GZIPInputStream;

import javax.net.ssl.HttpsURLConnection;

import org.apache.http.HttpStatus;
import org.apache.http.cookie.SM;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.ByteArrayBuffer;
import org.json.JSONObject;
import org.zywx.wbpalmstar.widgetone.dataservice.WWidgetData;

import android.os.Build;
import android.os.Process;
import android.util.Log;

public class EHttpGet extends Thread implements HttpTask {

	static final int F_SHEM_ID_HTTP = 0;
	static final int F_SHEM_ID_HTTPS = 1;

	static final int BODY_TYPE_TEXT = 0;
	static final int BODY_TYPE_FILE = 1;

	public static final String UA = "Mozilla/5.0 (Linux; U; Mobile; "
			+ "Android " + Build.VERSION.RELEASE + ";" + Build.MODEL
			+ " Build/FRF91 )";

	private URL mClient;
	private HttpURLConnection mConnection;
	private InputStream mInStream;
	private int mShemeId;
	private String mUrl;
	private String mRedirects;
	private boolean mFromRedirects;
	private int mTimeOut;
	private boolean mRunning;
	private boolean mCancelled;
	private String mXmlHttpID;
	private String mCertPassword;
	private String mCertPath;
	private boolean mHasLocalCert;
	private EUExXmlHttpMgr mXmlHttpMgr;
	private Hashtable<String, String> mHttpHead;
	private int responseCode = -1;
	private String responseMessage = "";
	private String responseError = "";
	private Map<String, List<String>> headers;
	private InputStream mErrorInStream;

	// private String mBody;

	public EHttpGet(String inXmlHttpID, String url, int timeout,
			EUExXmlHttpMgr meUExXmlHttpMgr) {
		setName("SoTowerMobile-HttpGet");
		mXmlHttpID = inXmlHttpID;
		mTimeOut = timeout;
		mXmlHttpMgr = meUExXmlHttpMgr;
		mUrl = url;
		mShemeId = url.startsWith("https") ? F_SHEM_ID_HTTPS : F_SHEM_ID_HTTP;
		initNecessaryHeader();
	}

	@Override
	public void setData(int inDataType, String inKey, String inValue) {
		;
	}

	@Override
	public void setCertificate(String cPassWord, String cPath) {
		mHasLocalCert = true;
		mCertPassword = cPassWord;
		mCertPath = cPath;
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
		if (Build.VERSION.SDK_INT < 8) {
			System.setProperty("http.keepAlive", "false");
		}
		Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);
		doInBackground();
	}

	private void doInBackground() {
		if (mCancelled) {
			return;
		}
		String result = "";
		String curUrl;
		boolean isSuccess = false;
		if (null == mUrl) {
			return;
		}
		if (mFromRedirects && null != mRedirects) {
			// 若为请求地址为重定向地址，则需要判断地址类型从而保证地址为可访问的绝对地址
			if (mRedirects.startsWith("http") || mRedirects.startsWith("https")) {
				curUrl = mRedirects;
			} else if (mRedirects.startsWith("/")) {
				// 返回地址为相对地址，需要处理
				if (mClient != null) {
					curUrl = mClient.getProtocol() + "://" + mClient.getHost()
							+ mRedirects;
				} else {
					// 为了防止某些特殊情况mClient为null
					curUrl = mRedirects;
				}
			} else {
				curUrl = mRedirects;
			}
		} else {
			curUrl = mUrl;
		}
		boolean https = false;
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
			mConnection.setRequestMethod("GET");
			mConnection.setRequestProperty("Accept-Encoding", "gzip, deflate");
			String cookie = null;
			cookie = mXmlHttpMgr.getCookie(curUrl);
			if (null != cookie) {
				mConnection.setRequestProperty(SM.COOKIE, cookie);
			}
			mConnection.setUseCaches(false);
			addHeaders();
			mConnection.setReadTimeout(mTimeOut);
			mConnection.setConnectTimeout(mTimeOut);
			mConnection.setInstanceFollowRedirects(false);
			mXmlHttpMgr.printHeader(-1, mXmlHttpID, curUrl, true,
					mConnection.getRequestProperties());
			mConnection.connect();
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
					Log.i("xmlHttpMgr", "redirect url " + responseCode);
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
            e.printStackTrace();
			isSuccess=false;
			if ((e instanceof IOException) && https) {
				result = EUExXmlHttpMgr.CONNECT_FAIL_AUTHENTICATION;
			} else if (e instanceof SocketTimeoutException) {
				result = EUExXmlHttpMgr.CONNECT_FAIL_TIMEDOUT;
			} else {
				result = EUExXmlHttpMgr.CONNECT_FAIL_CONNECTION_FAILURE;
			}
		} finally {
			try {
				if (null != mInStream) {
					mInStream.close();
				}
				if (null != mErrorInStream) {
					mErrorInStream.close();
				}
				if (null != mConnection) {
					mConnection.disconnect();
				}
			} catch (Exception e) {
			}
		}
		if (mCancelled) {
			return;
		}
		mXmlHttpMgr.printResult(mXmlHttpID, curUrl, result);
		mXmlHttpMgr.onFinish(mXmlHttpID);
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
		} else {
			setCookies = headers.get("set-cookie");
			if (null != setCookies) {
				for (String v : setCookies) {
					mXmlHttpMgr.setCookie(url, v);
				}
			}
		}
		List<String> Cookie = headers.get(SM.COOKIE);
		if (null != Cookie) {
			for (String v : Cookie) {
				mXmlHttpMgr.setCookie(url, v);
			}
		} else {
			Cookie = headers.get("cookie");
			if (null != Cookie) {
				for (String v : Cookie) {
					mXmlHttpMgr.setCookie(url, v);
				}
			}
		}
		List<String> Cookie2 = headers.get(SM.COOKIE2);
		if (null != Cookie2) {
			for (String v : Cookie2) {
				mXmlHttpMgr.setCookie(url, v);
			}
		} else {
			Cookie2 = headers.get("cookie2");
			if (null != Cookie2) {
				for (String v : Cookie2) {
					mXmlHttpMgr.setCookie(url, v);
				}
			}
		}
	}

	@Override
	public void cancel() {
		mCancelled = true;
		try {
			if (null != mInStream) {
				mInStream.close();
			}
			if (null != mErrorInStream) {
				mErrorInStream.close();
			}
			interrupt();
		} catch (Exception e) {
			e.printStackTrace();
		}
		mTimeOut = 0;
		mUrl = null;
		mRunning = false;
		mConnection = null;
		mClient = null;
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

	@Override
	public void setInputStream(File file) {
		;
	}

	@Override
	public void setBody(String body) {
		// mBody = body;
	}

	private void addHeaders() {
		if (null != mConnection) {
			Set<Entry<String, String>> entrys = mHttpHead.entrySet();
			for (Map.Entry<String, String> entry : entrys) {

				mConnection
						.setRequestProperty(entry.getKey(), entry.getValue());
			}
		}
	}

	private void initNecessaryHeader() {
		mHttpHead = new Hashtable<String, String>();
		mHttpHead.put("Accept", "*/*");
		mHttpHead.put("Charset", HTTP.UTF_8);
		mHttpHead.put("User-Agent", UA);
		mHttpHead.put("Connection", "Keep-Alive");
		mHttpHead.put("Accept-Encoding", "gzip, deflate");
	}

	@Override
	public void setAppVerifyHeader(WWidgetData curWData) {
		mHttpHead.put(
				XmlHttpUtil.KEY_APPVERIFY,
				XmlHttpUtil.getAppVerifyValue(curWData,
						System.currentTimeMillis()));
		mHttpHead.put(XmlHttpUtil.XMAS_APPID,curWData.m_appId);
	}
}
