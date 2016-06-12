package org.zywx.wbpalmstar.plugin.uexmultiHttp;

import android.os.Environment;

import org.json.JSONException;
import org.json.JSONObject;
import org.zywx.wbpalmstar.widgetone.dataservice.WWidgetData;

import java.io.File;
import java.io.FileOutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;

public class XmlHttpUtil {

    public final static String KEY_APPVERIFY = "appverify";
    public final static String XMAS_APPID = "x-mas-app-id";
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
     * @param curWData  当前widgetData
     * @param timeStamp 当前时间戳
     * @return
     * @createAt 2014.04
     * @author yipeng.zhang
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

    public static JSONObject getJSONHeaders(Map<String, List<String>> headers)
            throws JSONException {
        JSONObject jsonHeaders = new JSONObject();
        for (Map.Entry<String, List<String>> entry : headers.entrySet()) {
            String key = entry.getKey();
            String s = "";
            List<String> value = entry.getValue();
            if (null != value) {
                for (String v : value) {
                    s += v;
                }
            }
            if (key != null) {
                jsonHeaders.put(key, s);
            }
        }
        return jsonHeaders;
    }

}
