package org.zywx.wbpalmstar.plugin.uexmultiHttp;

import java.io.File;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.http.Header;
import org.apache.http.HttpHost;
import org.zywx.wbpalmstar.base.BUtility;
import org.zywx.wbpalmstar.engine.EBrowserView;
import org.zywx.wbpalmstar.engine.universalex.EUExBase;
import org.zywx.wbpalmstar.engine.universalex.EUExCallback;
import org.zywx.wbpalmstar.engine.universalex.EUExUtil;
import org.zywx.wbpalmstar.widgetone.dataservice.WWidgetData;

import android.content.Context;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;

public class EUExXmlHttpMgr extends EUExBase {

	public static final String CONNECT_FAIL_AUTHENTICATION = "Authentication needed";
	public static final String CONNECT_FAIL_TIMEDOUT = "The request timed out";
	public static final String CONNECT_FAIL_CONNECTION_FAILURE = "A connection failure occurred";
	public static final String PARAMS_JSON_KEY_HEADERS = "responseHeaders";
	public static final String PARAMS_JSON_KEY_STATUSCODE = "responseStatusCode";
	public static final String PARAMS_JSON_KEY_STATUSMESSAGE = "responseStatusMessage";
	public static final String PARAMS_JSON_KEY_RESPONSESTRING = "responseString";
	public static final String PARAMS_JSON_KEY_RESPONSEERROR = "responseError";
	public static final String onFunction = "uexXmlHttpMgr.onData";
	public static final String post_onFunction = "uexXmlHttpMgr.onPostProgress";

	private Hashtable<Object, HttpTask> mXmlHttpMap;
	private Hashtable<String, Boolean> mNeedVerifyMap;
	private boolean mPrintLog = false;

	public static final String getCookie_onFunction = "uexXmlHttpMgr.cbGetCookie";

	private WWidgetData mCurWData;

	public EUExXmlHttpMgr(Context context, EBrowserView inParent) {
		super(context, inParent);
		mCurWData = inParent.getCurrentWidget();
	}

	public void open(String[] parm) {
		if (parm.length < 4) {
			return;
		}
		if (null == mXmlHttpMap) {
			mXmlHttpMap = new Hashtable<Object, HttpTask>();
		}
		if (null == mNeedVerifyMap) {
			mNeedVerifyMap = new Hashtable<String, Boolean>();
		}
		String opCode = parm[0];
		String inMethod = parm[1];
		String inUrl = parm[2];
		String inTimeout = parm[3];
		int timeout = 1000 * 30;
		try {
			if(inTimeout != null && Integer.valueOf(inTimeout) != 0) {
				timeout = Integer.valueOf(inTimeout);
			}
		} catch (Exception e) {
			;
		}
		try {
			inUrl = inUrl.replace(" ", "+");
			if (null == inUrl || inUrl.trim().length() == 0) {
				errorCallback(0, EUExCallback.F_E_UEXXMLHTTP_OPEN,
						"hava illegal argument in url");
				return;
			}
		} catch (Exception e) {
			errorCallback(0, EUExCallback.F_E_UEXXMLHTTP_OPEN,
					"hava illegal argument in url");
			return;
		}
		if (mXmlHttpMap.containsKey(opCode)) {
			errorCallback(0, EUExCallback.F_E_UEXXMLHTTP_OPEN, "same opCode");
			return;
		}
		HttpTask xmlHttp = null;
		if (inMethod.equalsIgnoreCase("get")){
			xmlHttp = new EHttpGet(opCode, inUrl, timeout, this);
		}
		if (inMethod.equalsIgnoreCase("post")) {
			xmlHttp = new EHttpPost(opCode, inUrl, timeout, this);
		}
		if (inMethod.equalsIgnoreCase("put")) {
			xmlHttp = new EHttpPut(opCode, inUrl, timeout, this);
		}
		if (inMethod.equalsIgnoreCase("delete")) {
			xmlHttp = new EHttpDelete(opCode, inUrl, timeout, this);
		}
		mXmlHttpMap.put(opCode, xmlHttp);
	}

	/**
	 * 应用验证接口
	 * 
	 * @param parm
	 */
	public void setAppVerify(String[] parm) {
		if (parm.length < 2 || null == mXmlHttpMap) {
			return;
		}
		String opCode = parm[0];
		String isAppVerify = parm[1];
		boolean needAppVerify = false;
		if (isAppVerify.equals("1")) {
			needAppVerify = true;
		}
		mNeedVerifyMap.put(opCode, needAppVerify);
	}

	public void setPostData(String[] parm) {
		if (parm.length < 4 || null == mXmlHttpMap) {
			return;
		}
		String opCode = parm[0];
		String inDataType = parm[1];
		String inKey = parm[2];
		String inValue = parm[3];
		HttpTask xmlHttp = mXmlHttpMap.get(opCode);
		if (null != xmlHttp) {
			int type = 0;
			try {
				type = Integer.valueOf(inDataType);
			} catch (Exception e) {
			}
			xmlHttp.setData(type, inKey, inValue);
		}
	}

	public void setPutData(String[] parm) {
		if (parm.length < 4 || null == mXmlHttpMap) {
			return;
		}
		String opCode = parm[0];
		String inDataType = parm[1];
		String inKey = parm[2];
		String inValue = parm[3];
		HttpTask xmlHttp = mXmlHttpMap.get(opCode);
		if (null != xmlHttp) {
			int type = 0;
			try {
				type = Integer.valueOf(inDataType);
			} catch (Exception e) {
			}
			xmlHttp.setData(type, inKey, inValue);
		}
	}
	
	public void setInputStream(String[] parm) {
		if (parm.length < 2 || null == mXmlHttpMap) {
			return;
		}
		String opCode = parm[0];
		String inValue = parm[1];
		HttpTask xmlHttp = mXmlHttpMap.get(opCode);
		if (null != xmlHttp) {
			if (null != inValue) {
				String wp = mBrwView.getWidgetPath();
				int wtp = mBrwView.getWidgetType();
				inValue = BUtility.makeRealPath(inValue, wp, wtp);
				if (inValue.startsWith(BUtility.F_FILE_SCHEMA)) {
					inValue = inValue
							.substring(BUtility.F_FILE_SCHEMA.length());
				}
				File file = new File(inValue);
				if (file.exists()) {
					xmlHttp.setInputStream(file);
					return;
				}
				errorCallback(0, EUExCallback.F_E_UEXXMLHTTP_OPEN,
						"file not exists");
			}
		}
	}

	public void setCertificate(String[] parm) {
		if (parm.length < 3 || null == mXmlHttpMap) {
			return;
		}
		String opCode = parm[0];
		String cPassWord = parm[1];
		String cPath = parm[2];
		String cUrl = mBrwView.getCurrentUrl();
		if (null != cPath) {
			WWidgetData wd = mBrwView.getCurrentWidget();
			if ("default".equalsIgnoreCase(cPath)) {
				cPath = "file:///android_asset/widget/wgtRes/clientCertificate.p12";
				String appId = wd.m_appId;
				String psw = EUExUtil.getCertificatePsw(mContext, appId);
				cPassWord = psw;
			} else {
				if (cPath.contains("://")) {
					cPath = BUtility.makeRealPath(cPath, wd.m_widgetPath,
							wd.m_wgtType);
				} else {
					cPath = BUtility.makeUrl(cUrl, cPath);
					cPath = BUtility.makeRealPath(cPath, wd.m_widgetPath,
							wd.m_wgtType);
				}
			}
		}
		HttpTask xmlHttp = mXmlHttpMap.get(opCode);
		if (null != xmlHttp) {
			xmlHttp.setCertificate(cPassWord, cPath);
		}
	}

	public void send(String[] parm) {
		if (parm.length < 1 || null == mXmlHttpMap) {
			return;
		}
		String inXmlHttpOpCode = parm[0];
		int inPrintLog = 0;
		if (2 == parm.length) {
			String printLog = parm[1];
			try {
				inPrintLog = Integer.parseInt(printLog);
				mPrintLog = (1 != inPrintLog) ? false : true;
			} catch (Exception e) {
				;
			}
		}
		HttpTask xmlHttp = mXmlHttpMap.get(inXmlHttpOpCode);
		if (null != xmlHttp) {
			if(mNeedVerifyMap != null && mNeedVerifyMap.containsKey(inXmlHttpOpCode)) {
				Boolean isNeedAppVerify = mNeedVerifyMap.get(inXmlHttpOpCode);
				if (isNeedAppVerify) {
					xmlHttp.setAppVerifyHeader(mCurWData);
				}
			}
			xmlHttp.send();
		}
	}

	public void close(String[] parm) {
		if (parm.length < 1 || null == mXmlHttpMap) {
			return;
		}
		String inXmlHttpOpCode = parm[0];
		HttpTask xmlHttp = mXmlHttpMap.get(inXmlHttpOpCode);
		if (null != xmlHttp) {
			xmlHttp.cancel();
			mXmlHttpMap.remove(inXmlHttpOpCode);
			mNeedVerifyMap.remove(inXmlHttpOpCode);
		}
	}

	public void setHeaders(String[] parm) {
		if (parm.length < 2 || null == mXmlHttpMap) {
			return;
		}
		String opCode = parm[0];
		String inJson = parm[1];
		HttpTask xmlHttp = mXmlHttpMap.get(opCode);
		if (null != xmlHttp) {
			xmlHttp.setHeaders(inJson);
		}
	}

	public void setBody(String[] parm) {
		if (parm.length < 2 || null == mXmlHttpMap) {
			return;
		}
		String opCode = parm[0];
		String inContent = parm[1];
		HttpTask xmlHttp = mXmlHttpMap.get(opCode);
		if (null != xmlHttp) {
			xmlHttp.setBody(inContent);
		}
	}

	public void getCookie(String parm[]) {
		if (parm.length != 1) {
			return;
		}
		// String cookie = UexXMLHttpUtils.getCookie(mContext);
		// Log.i("getCookie", "getCookie=====" + cookie);
		getCookieCallBack(this.getCookie(parm[0]));
	}


	public void getCookieCallBack(String cookie) {
		String jsonS = "{" + "\"" + "cookie" + "\"" + ":" + "\"" + cookie
				+ "\"" + "}";
		jsCallback(getCookie_onFunction, 0, EUExCallback.F_C_JSON, jsonS);
	}


	public void onFinish(int opCode) {
		String code = String.valueOf(opCode);
		HttpTask xmlHttp = mXmlHttpMap.get(code);
		if (null != xmlHttp && null != mXmlHttpMap) {
			mXmlHttpMap.remove(code);
		}
	}

	public void printHeader(int code, int opCode, String url, boolean out,
			Header[] header) {
		if (mPrintLog && null != header) {
			String str = code + " , " + opCode + " , "
					+ (out ? "out: " : "back: ") + url + ", ";
			StringBuffer sb = new StringBuffer();
			for (Header h : header) {
				sb.append(h.getName());
				sb.append("=");
				sb.append(h.getValue());
				sb.append(";");
			}
			// System.out.println(sb.toString());
			XmlHttpUtil.printInfo2File(str + sb.toString());
		}
	}

	public void printResult(int opCode, String url, String result) {
		if (mPrintLog && null != result) {
			String str = opCode + " , " + url + " , " + result;
			XmlHttpUtil.printInfo2File(str);
		}
	}

	public void printHeader(int code, int opCode, String url, boolean out,
			Map<String, List<String>> headers) {
		if (mPrintLog && null != headers) {
			String str = code + " , " + opCode + " , "
					+ (out ? "out: " : "back: ") + url + ", ";
			StringBuffer sb = new StringBuffer();
			for (Map.Entry<String, List<String>> entry : headers.entrySet()) {
				String key = entry.getKey();
				List<String> value = entry.getValue();
				if (null != value) {
					sb.append(key);
					sb.append("=");
					String s = "";
					for (String v : value) {
						s += v;
					}
					sb.append(s);
					sb.append(";");
				}
			}
			XmlHttpUtil.printInfo2File(str + sb.toString());
		}
	}

	public String getWidgetPath() {

		return mBrwView.getWidgetPath();
	}

	public int getWidgetType() {

		return mBrwView.getWidgetType();
	}

	public void callBack(int inOpCode, String inResult, int responseCode, String response) {
		String js = SCRIPT_HEADER + "if(" + onFunction + "){" + onFunction
				+ "(" + inOpCode + "," + 1 + ",'" + inResult +"'," + responseCode + ",'" + response + "');}";
		onCallback(js);
	}

	public void errorCallBack(int inOpCode, String inResult, int responseCode, String response) {
		String js = SCRIPT_HEADER + "if(" + onFunction + "){" + onFunction
				+ "(" + inOpCode + "," + (-1) + ",'" + inResult + "'," + responseCode + ",'" + response +"');}";
		onCallback(js);
	}

	public void progressCallBack(int inOpCode, int progress) {
		String js = SCRIPT_HEADER + "if(" + post_onFunction + "){"
				+ post_onFunction + "(" + inOpCode + "," + progress + ")}";
		onCallback(js);
	}

	public Context getContext() {
		return mContext;
	}

	@Override
	protected boolean clean() {
		if (null != mXmlHttpMap) {
			synchronized (mXmlHttpMap) {
				Set<Entry<Object, HttpTask>> entrys = mXmlHttpMap.entrySet();
				for (Map.Entry<Object, HttpTask> entry : entrys) {
					HttpTask temp = entry.getValue();
					temp.cancel();
				}
				mXmlHttpMap.clear();
			}
		}
		return true;
	}

	public java.net.Proxy checkJavaProxy() {
		java.net.Proxy proxy = null;
		if (!checkWifi()) {// 获取当前正在使用的APN接入点
			Uri uri = Uri.parse("content://telephony/carriers/preferapn");
			Cursor mCursor = mContext.getContentResolver().query(uri, null,
					null, null, null);
			if (mCursor != null && mCursor.moveToFirst()) {
				String proxyStr = mCursor.getString(mCursor
						.getColumnIndex("proxy"));
				int proxyPort = mCursor.getInt(mCursor.getColumnIndex("port"));
				if (proxyStr != null && proxyStr.trim().length() > 0) {
					if (0 == proxyPort) {
						proxyPort = 80;
					}
					proxy = new java.net.Proxy(Proxy.Type.HTTP,
							new InetSocketAddress(proxyStr, proxyPort));
				}
				mCursor.close();
			}
		}
		return proxy;
	}

	public HttpHost checkAndroidProxy() {
		HttpHost proxy = null;
		if (!checkWifi()) {// 获取当前正在使用的APN接入点
			Uri uri = Uri.parse("content://telephony/carriers/preferapn");
			Cursor mCursor = mContext.getContentResolver().query(uri, null,
					null, null, null);
			if (mCursor != null && mCursor.moveToFirst()) {
				String proxyStr = mCursor.getString(mCursor
						.getColumnIndex("proxy"));
				int proxyPort = mCursor.getInt(mCursor.getColumnIndex("port"));
				if (proxyStr != null && proxyStr.trim().length() > 0) {
					if (0 == proxyPort) {
						proxyPort = 80;
					}
					proxy = new HttpHost(proxyStr, proxyPort);
				}
				mCursor.close();
			}
		}
		return proxy;
	}

	private boolean checkWifi() {

		return NETWORK_CLASS_WIFI == getConnectedType();
	}

	/** Unknown network class */
	public static final int NETWORK_CLASS_UNKNOWN = 0;
	/** Class of broadly defined "2G" networks */
	public static final int NETWORK_CLASS_2_G = 1;
	/** Class of broadly defined "3G" networks */
	public static final int NETWORK_CLASS_3_G = 2;
	/** Class of broadly defined "4G" networks */
	public static final int NETWORK_CLASS_4_G = 3;
	/** Class of broadly defined "WiFi" networks */
	public static final int NETWORK_CLASS_WIFI = 4;

	public int getConnectedType() {
		ConnectivityManager cManager = (ConnectivityManager) mContext
				.getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo nInfo = cManager.getActiveNetworkInfo();
		if (nInfo != null && nInfo.isAvailable()) {
			int type = nInfo.getType();
			int subType = nInfo.getSubtype();
			switch (type) {
			case ConnectivityManager.TYPE_MOBILE:
				switch (subType) {
				case 1:// TelephonyManager.NETWORK_TYPE_GPRS:
				case 2:// TelephonyManager.NETWORK_TYPE_EDGE:
				case 4:// TelephonyManager.NETWORK_TYPE_CDMA:
				case 7:// TelephonyManager.NETWORK_TYPE_1xRTT:
				case 11:// TelephonyManager.NETWORK_TYPE_IDEN:
					return NETWORK_CLASS_2_G;
				case 3:// TelephonyManager.NETWORK_TYPE_UMTS:
				case 5:// TelephonyManager.NETWORK_TYPE_EVDO_0:
				case 6:// TelephonyManager.NETWORK_TYPE_EVDO_A:
				case 8:// TelephonyManager.NETWORK_TYPE_HSDPA:
				case 9:// TelephonyManager.NETWORK_TYPE_HSUPA:
				case 10:// TelephonyManager.NETWORK_TYPE_HSPA:
				case 12:// TelephonyManager.NETWORK_TYPE_EVDO_B:
				case 14:// TelephonyManager.NETWORK_TYPE_EHRPD:
				case 15:// TelephonyManager.NETWORK_TYPE_HSPAP:
					return NETWORK_CLASS_3_G;
				case 13:// TelephonyManager.NETWORK_TYPE_LTE:
					return NETWORK_CLASS_4_G;
				default:
					return NETWORK_CLASS_UNKNOWN;
				}
			case ConnectivityManager.TYPE_WIFI:

				return NETWORK_CLASS_WIFI;
			}
		}
		return NETWORK_CLASS_UNKNOWN;
	}
}
