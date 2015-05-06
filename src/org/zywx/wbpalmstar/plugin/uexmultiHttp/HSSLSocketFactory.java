package org.zywx.wbpalmstar.plugin.uexmultiHttp;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.security.KeyStore;
import java.security.SecureRandom;
import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.params.HttpParams;

public class HSSLSocketFactory extends SSLSocketFactory {
	
	private SSLContext mSSLContext;
	
	public HSSLSocketFactory(KeyStore ksP12, String keyPass) throws Exception {
		super(ksP12);
		mSSLContext = SSLContext.getInstance(SSLSocketFactory.TLS);
		KeyManagerFactory kMgrFact = null; 
		TrustManager[] tMgrs = null;
		KeyManager[] kMgrs = null;
		TrustManager tMgr = null;
		tMgr = new HX509TrustManager(ksP12);
		kMgrFact = KeyManagerFactory.getInstance(Http.algorithm);
		if(null != keyPass){
			kMgrFact.init(ksP12, keyPass.toCharArray());
		}else{
			kMgrFact.init(ksP12, null);
		}
		kMgrs = kMgrFact.getKeyManagers();
		tMgrs = new TrustManager[]{tMgr};
		SecureRandom secureRandom = new java.security.SecureRandom();
		mSSLContext.init(kMgrs, tMgrs, secureRandom);
	}

	@Override
	public Socket createSocket(Socket socket, String host, int port, boolean autoClose) 
			throws IOException, UnknownHostException {
		javax.net.ssl.SSLSocketFactory socketfact = mSSLContext.getSocketFactory();
		Socket result = socketfact.createSocket(socket, host, port, autoClose);
		return result;
	}

	@Override
	public Socket connectSocket(Socket sock, String host, int port,
			InetAddress localAddress, int localPort, HttpParams params)
			throws IOException {

		return super.connectSocket(sock, host, port, localAddress, localPort, params);
	}

	@Override
	public Socket createSocket() throws IOException {
		javax.net.ssl.SSLSocketFactory socketfact = mSSLContext.getSocketFactory();
		Socket result = socketfact.createSocket();
		return result;
	}
}
