package org.zywx.wbpalmstar.plugin.uexmultiHttp;

public interface HttpMsg extends Runnable{
	
	public void forceClose();
	public boolean inProgress();
	public void setCertificate(String cPsw, String cPath);
	public void setHeaders(String json);
	public void setBody(String body);
	public int getIdentity();
	public void setIdentity(int id);
	public int getOperateId();
	public void setOperateId(int id);
	public void setPostData(int inDataType, String inKey, String inValue);
}
