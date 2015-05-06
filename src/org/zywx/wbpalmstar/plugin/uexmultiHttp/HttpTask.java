package org.zywx.wbpalmstar.plugin.uexmultiHttp;

import java.io.File;

import org.zywx.wbpalmstar.widgetone.dataservice.WWidgetData;

public interface HttpTask {
	void setData(int inDataType, String inKey, String inValue);

	void send();

	void cancel();

	void setCertificate(String cPassWord, String cPath);

	void setHeaders(String headJson);

	void setBody(String body);

	void setInputStream(File file);

	/**
	 * 添加app验证Header（如果需要）
	 * 
	 * @param curWData
	 */
	void setAppVerifyHeader(WWidgetData curWData);
}
