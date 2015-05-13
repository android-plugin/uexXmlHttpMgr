package org.zywx.wbpalmstar.plugin.uexmultiHttp;

import java.io.File;
import java.io.FileInputStream;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.cookie.SM;
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.entity.StringEntity;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;
import org.json.JSONObject;
import org.zywx.wbpalmstar.base.BUtility;
import org.zywx.wbpalmstar.widgetone.dataservice.WWidgetData;

import android.os.Process;

public class EHttpPut extends Thread implements HttpTask, HttpClientListener {
	private boolean mRunning;
	private boolean mCancelled;
	private String mUrl;
	private int mTimeOut;
	private String mBody;
	private File mOnlyFile;
	private EUExXmlHttpMgr mXmlHttpMgr;
	private boolean mHasLocalCert;
	private String mCertPassword;
	private String mCertPath;
	private int mXmlHttpID;
	private Hashtable<String, String> mHttpHead;
	private ArrayList<HPair> mMultiData;
	private WWidgetData curWData = null;
	private HttpClient mHttpClient;
	private String mRedirects;
	private boolean mFromRedirects;
	private HttpPut mHttpPut;
	
	static final int BODY_TYPE_TEXT = 0;
	static final int BODY_TYPE_FILE = 1;
	private int responseCode = -1;
	private String responseMessage = "";
	private Header[] headers;

	public EHttpPut(String inXmlHttpID, String inUrl, int timeout,
			EUExXmlHttpMgr euExXmlHttpMgr) {
		mUrl = inUrl;
		mTimeOut = timeout;
		mXmlHttpMgr = euExXmlHttpMgr;
		mXmlHttpID = Integer.parseInt(inXmlHttpID);
		initNecessaryHeader();
	}

	private void initNecessaryHeader() {
		mHttpHead = new Hashtable<String, String>();
		mHttpHead.put("Accept", "*/*");
		mHttpHead.put("Charset", HTTP.UTF_8);
		mHttpHead.put("User-Agent", EHttpGet.UA);
		mHttpHead.put("Connection", "Keep-Alive");
		mHttpHead.put("Accept-Encoding", "gzip, deflate");
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
				int wtp = mXmlHttpMgr.getWidgetType();
				inValue = BUtility.makeRealPath(inValue, wp, wtp);
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

	private void doInBackground() {
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
		if (curUrl.startsWith("https")) {
			if (mHasLocalCert) {
				mHttpClient = Http.getHttpsClientWithCert(mCertPassword,
						mCertPath, mTimeOut, mXmlHttpMgr.getContext());
			} else {
				mHttpClient = Http.getHttpsClient(mTimeOut);
			}
		} else {
			mHttpClient = Http.getHttpClient(mTimeOut);
		}
		if (null == mHttpClient) {
//			mXmlHttpMgr.callBack(mXmlHttpID, "error:Exception!");
			return;
		}
		mHttpPut = new HttpPut(curUrl);
		HttpEntity multiEn = null;
		if (null != mOnlyFile) {
			multiEn = createInputStemEntity();
		} else if (null != mMultiData) {
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
		if (null != multiEn) {
			mHttpPut.setEntity(multiEn);
		}
		String cookie = mXmlHttpMgr.getCookie(curUrl);
		if (null != cookie) {
			mHttpPut.addHeader(SM.COOKIE, cookie);
		}
		if(null != curWData) {
			mHttpPut.addHeader(XmlHttpUtil.KEY_APPVERIFY, XmlHttpUtil.getAppVerifyValue(curWData, System.currentTimeMillis()));
			curWData = null;
		}
		addHeaders();
		try {
			mXmlHttpMgr.printHeader(-1, mXmlHttpID, curUrl, true, mHttpPut.getAllHeaders());
			HttpResponse response = mHttpClient.execute(mHttpPut);
			responseCode = response.getStatusLine().getStatusCode();
			responseMessage = response.getStatusLine().getReasonPhrase();
			headers = response.getAllHeaders();
			mXmlHttpMgr.printHeader(responseCode, mXmlHttpID, curUrl, false, headers);
			switch (responseCode) {
			case HttpStatus.SC_OK:
				HttpEntity httpEntity = response.getEntity();
				String charSet = EntityUtils.getContentCharSet(httpEntity);
				if (null == charSet) {
					charSet = HTTP.UTF_8;
				}
				byte[] arrayOfByte = XmlHttpUtil.toByteArray(httpEntity);
				result = new String(arrayOfByte, charSet);
				httpEntity.consumeContent();
				isSuccess = true;
				break;
			case HttpStatus.SC_MOVED_PERMANENTLY:
			case HttpStatus.SC_MOVED_TEMPORARILY:
			case HttpStatus.SC_TEMPORARY_REDIRECT:
				Header location = response.getFirstHeader("Location");
				String reUrl = location.getValue();
				if (null != reUrl && reUrl.length() > 0) {
					mRedirects = reUrl;
					mFromRedirects = true;
					handleCookie(curUrl, response);
					doInBackground();
					return;
				}
				break;
			default:
				break;
			}
			handleCookie(curUrl, response);
		} catch (Exception e) {
			isSuccess = false;
			if (e instanceof SocketTimeoutException) {
				result = "timeout"; // 网络连接超时。
			} else {
				result = "net work error";
			}
		} finally {
			mHttpPut.abort();
			mHttpClient.getConnectionManager().shutdown();
		}
		mXmlHttpMgr.onFinish(mXmlHttpID);
		if (mCancelled) {
			return;
		}
		mXmlHttpMgr.printResult(mXmlHttpID, curUrl, result);
		if (isSuccess) {
			JSONObject jsonObject = new JSONObject();
			try {
				if (headers != null && headers.length != 0) {
					JSONObject jsonHeaders = new JSONObject();
					for (int i = 0; i < headers.length; i++) {
						jsonHeaders.put(headers[i].getName(),
								headers[i].getValue());
					}
					jsonObject.put(EUExXmlHttpMgr.PARAMS_JSON_KEY_HEADERS,
							jsonHeaders);
				}
				jsonObject.put(EUExXmlHttpMgr.PARAMS_JSON_KEY_STATUSCODE,
						responseCode);
				jsonObject.put(EUExXmlHttpMgr.PARAMS_JSON_KEY_STATUSMESSAGE,
						responseMessage);
				jsonObject
						.put(EUExXmlHttpMgr.PARAMS_JSON_KEY_RESPONSEERROR, "");
			} catch (Exception e) {
			}
			mXmlHttpMgr.callBack(mXmlHttpID, result, responseCode,
					jsonObject.toString());
		} else {
			mXmlHttpMgr.errorCallBack(mXmlHttpID, result, responseCode, "");
		}
		return;
	}

	private HttpEntity createMultiEntity() {
		EMultiEntity multiEn = null;
		try {
			multiEn = new EMultiEntity();
			multiEn.addHttpClientListener(this);
			for (HPair pair : mMultiData) {
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

	private HttpEntity createFormEntity() {
		HttpEntity entry = null;
		try {
			List<BasicNameValuePair> postData = new ArrayList<BasicNameValuePair>();
			for (HPair pair : mMultiData) {
				postData.add(new BasicNameValuePair(pair.key, pair.value));
			}
			entry = new UrlEncodedFormEntity(postData, HTTP.UTF_8);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return entry;
	}

	private boolean containOctet() {
		for (HPair pair : mMultiData) {
			if (pair.type == BODY_TYPE_FILE) {
				return true;
			}
		}
		return false;
	}

	private HttpEntity createInputStemEntity() {
		HttpEntity entry = null;
		try {
			FileInputStream instream = new FileInputStream(mOnlyFile);
			entry = new InputStreamEntity(instream, mOnlyFile.length());
		} catch (Exception e) {
			e.printStackTrace();
		}
		return entry;
	}

	private void handleCookie(String url, HttpResponse response) {
		Header[] setCookie = response.getHeaders(SM.SET_COOKIE);
		for (Header cokie : setCookie) {
			String str = cokie.getValue();
			mXmlHttpMgr.setCookie(url, str);
		}
		Header[] Cookie = response.getHeaders(SM.COOKIE);
		for (Header cokie : Cookie) {
			String str = cokie.getValue();
			mXmlHttpMgr.setCookie(url, str);
		}
		Header[] Cookie2 = response.getHeaders(SM.COOKIE2);
		for (Header cokie : Cookie2) {
			String str = cokie.getValue();
			mXmlHttpMgr.setCookie(url, str);
		}
	}

	private void addHeaders() {
		if (null != mHttpPut) {
			Set<Entry<String, String>> entrys = mHttpHead.entrySet();
			for (Map.Entry<String, String> entry : entrys) {

				mHttpPut.addHeader(entry.getKey(), entry.getValue());
			}
		}
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
	public void cancel() {
		mCancelled = true;
		if (null != mMultiData) {
			mMultiData.clear();
		}
		if (null != mHttpPut) {
			mHttpPut.abort();
		}
		if (null != mHttpClient) {
			mHttpClient.getConnectionManager().shutdown();
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
		mBody = null;
	}

	@Override
	public void setCertificate(String cPassWord, String cPath) {
		mHasLocalCert = true;
		mCertPassword = cPassWord;
		mCertPath = cPath;
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
	public void setBody(String body) {
		mBody = body;
	}

	@Override
	public void setInputStream(File file) {
		mOnlyFile = file;
	}

	@Override
	public void setAppVerifyHeader(WWidgetData curWData) {
		this.curWData = curWData;
	}

	@Override
	public void onProgressChanged(float newProgress) {
		int progress = (int) newProgress;
		mXmlHttpMgr.progressCallBack(mXmlHttpID, progress);
	}

}
