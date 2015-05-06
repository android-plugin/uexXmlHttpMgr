package org.zywx.wbpalmstar.plugin.uexmultiHttp;

import java.io.EOFException;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.zip.GZIPInputStream;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.util.ByteArrayBuffer;
import org.zywx.wbpalmstar.widgetone.dataservice.WWidgetData;

import android.os.Environment;

public class XmlHttpUtil {

	public final static String KEY_APPVERIFY = "appverify";
	static DateFormat formatter = new SimpleDateFormat("yyyyMMdd");
	static DateFormat formatter1 = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss");

	public static void printInfo2File(String msg) {
		if (null == msg || 0 == msg.length()) {
			return;
		}
		try {
			String time = formatter.format(new Date());
			String time1 = formatter1.format(new Date());
			String fileName = time + ".log";
			if (Environment.getExternalStorageState().equals(
					Environment.MEDIA_MOUNTED)) {
				String path = "/sdcard/widgetone/log/xmlHttp/";
				File dir = new File(path);
				if (!dir.exists()) {
					dir.mkdirs();
				}
				FileOutputStream fos = new FileOutputStream(path + fileName,
						true);
				fos.write((time1 + ": " + msg + "\n").getBytes());
				fos.flush();
				fos.close();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * 添加验证头
	 * 
	 * @param curWData
	 *            当前widgetData
	 * @param timeStamp
	 *            当前时间戳
	 * @createAt 2014.04
	 * @author yipeng.zhang
	 * @return
	 */
	public static String getAppVerifyValue(WWidgetData curWData, long timeStamp) {
		String value = null;
		String md5 = getMD5Code(curWData.m_appId + ":" + curWData.m_appkey
				+ ":" + timeStamp);
		value = "md5=" + md5 + ";ts=" + timeStamp;
		return value;

	}

	public static String getMD5Code(String value) {
		if (value == null) {
			value = "";
		}
		try {
			MessageDigest md = MessageDigest.getInstance("MD5");
			md.reset();
			md.update(value.getBytes());
			byte[] md5Bytes = md.digest();
			StringBuffer hexValue = new StringBuffer();
			for (int i = 0; i < md5Bytes.length; i++) {
				int val = ((int) md5Bytes[i]) & 0xff;
				if (val < 16)
					hexValue.append("0");
				hexValue.append(Integer.toHexString(val));
			}
			return hexValue.toString();
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}

		return null;
	}
	
	
	public static byte[] toByteArray(HttpEntity entity) throws Exception {
		if (entity == null) {
			throw new Exception("HTTP entity may not be null");
		}
		InputStream mInStream = entity.getContent();
		if (mInStream == null) {
			return new byte[] {};
		}
		long len = entity.getContentLength();
		if (len > Integer.MAX_VALUE) {
			throw new Exception(
					"HTTP entity too large to be buffered in memory");
		}
		Header contentEncoding = entity.getContentEncoding();
		boolean gzip = false;
		if (null != contentEncoding) {
			if ("gzip".equalsIgnoreCase(contentEncoding.getValue())) {
				mInStream = new GZIPInputStream(mInStream, 2048);
				gzip = true;
			}
		}
		ByteArrayBuffer buffer = new ByteArrayBuffer(1024 * 8);
		// \&:38, \n:10, \r:13, \':39, \":34, \\:92
		try {
			if (gzip) {
				int lenth = 0;
				while (lenth != -1) {
					byte[] buf = new byte[2048];
					try {
						lenth = mInStream.read(buf, 0, buf.length);
						if (lenth != -1) {
							buffer.append(buf, 0, lenth);
						}
					} catch (EOFException e) {
						int tl = buf.length;
						int surpl;
						for (int k = 0; k < tl; ++k) {
							surpl = buf[k];
							if (surpl != 0) {
								buffer.append(surpl);
							}
						}
						lenth = -1;
					}
				}
				int bl = buffer.length();
				ByteArrayBuffer temBuffer = new ByteArrayBuffer(
						(int) (bl * 1.4));
				for (int j = 0; j < bl; ++j) {
					int cc = buffer.byteAt(j);
					if (cc == 34 || cc == 39 || cc == 92 || cc == 10
							|| cc == 13 || cc == 38) {
						temBuffer.append('\\');
					}
					temBuffer.append(cc);
				}
				buffer = temBuffer;
			} else {
				int c;
				while ((c = mInStream.read()) != -1) {
					if (c == 34 || c == 39 || c == 92 || c == 10 || c == 13
							|| c == 38) {
						buffer.append('\\');
					}
					buffer.append(c);
				}
			}
		} catch (Exception e) {
			mInStream.close();
		} finally {
			mInStream.close();
		}
		return buffer.toByteArray();
	}
}
