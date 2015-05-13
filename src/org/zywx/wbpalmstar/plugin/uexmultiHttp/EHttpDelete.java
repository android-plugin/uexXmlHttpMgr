package org.zywx.wbpalmstar.plugin.uexmultiHttp;

import java.io.File;
import java.net.SocketTimeoutException;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.cookie.SM;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;
import org.json.JSONObject;
import org.zywx.wbpalmstar.widgetone.dataservice.WWidgetData;

import android.os.Process;

public class EHttpDelete extends Thread implements HttpTask {
	
	private int mXmlHttpID;
	private int mTimeOut;
	private EUExXmlHttpMgr mXmlHttpMgr;
	private String mUrl;
	private boolean mRunning;
	private boolean mCancelled;
	private HttpClient mHttpClient;
	private String mCertPassword;
	private String mCertPath;
	private boolean mHasLocalCert;
	private String mRedirects;
	private boolean mFromRedirects;
	private Hashtable<String, String> mHttpHead;
	private HttpDelete mHttpDelete;
	private int responseCode = -1;
	private String responseMessage = "";
	private Header[] headers;

	public EHttpDelete(String inXmlHttpID, String inUrl, int timeout,
			EUExXmlHttpMgr euExXmlHttpMgr) {
		mXmlHttpID = Integer.parseInt(inXmlHttpID);
		mTimeOut = timeout;
		mXmlHttpMgr = euExXmlHttpMgr;
		mUrl = inUrl;
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
		;
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
		mHttpDelete = new HttpDelete(mUrl);
		String cookie = mXmlHttpMgr.getCookie(curUrl);
		if (null != cookie) {
			mHttpDelete.addHeader(SM.COOKIE, cookie);
		}
		addHeaders();
		try {
			mXmlHttpMgr.printHeader(-1, mXmlHttpID, curUrl, true, mHttpDelete.getAllHeaders());
			HttpResponse response = mHttpClient.execute(mHttpDelete);
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
			mHttpDelete.abort();
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
		if (null != mHttpDelete) {
			Set<Entry<String, String>> entrys = mHttpHead.entrySet();
			for (Map.Entry<String, String> entry : entrys) {

				mHttpDelete.addHeader(entry.getKey(), entry.getValue());
			}
		}
	}

	@Override
	public void cancel() {
		mCancelled = true;
		try {
			interrupt();
		} catch (Exception e) {
			e.printStackTrace();
		}
		mTimeOut = 0;
		mUrl = null;
		mRunning = false;
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
		
	}

	@Override
	public void setInputStream(File file) {
		;
	}

	@Override
	public void setAppVerifyHeader(WWidgetData curWData) {
		mHttpHead.put(
				XmlHttpUtil.KEY_APPVERIFY,
				XmlHttpUtil.getAppVerifyValue(curWData,
						System.currentTimeMillis()));
	}

}
