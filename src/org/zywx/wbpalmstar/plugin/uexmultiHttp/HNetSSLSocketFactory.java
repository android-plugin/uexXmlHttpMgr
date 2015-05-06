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
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;

public class HNetSSLSocketFactory extends SSLSocketFactory {

	private SSLContext mSSLContext;
	
	public HNetSSLSocketFactory(KeyStore ksP12, String keyPass) throws Exception{
		super();
		mSSLContext = SSLContext.getInstance("TLS");
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
	public String[] getDefaultCipherSuites() {
		javax.net.ssl.SSLSocketFactory socketfact = mSSLContext.getSocketFactory();
		return socketfact.getDefaultCipherSuites();
	}

	@Override
	public String[] getSupportedCipherSuites() {
		javax.net.ssl.SSLSocketFactory socketfact = mSSLContext.getSocketFactory();
		return socketfact.getSupportedCipherSuites();
	}

	@Override
	public Socket createSocket(Socket socket, String host, int port, boolean autoClose) throws IOException {
		javax.net.ssl.SSLSocketFactory socketfact = mSSLContext.getSocketFactory();
		Socket result = socketfact.createSocket(socket, host, port, autoClose);
		return result;
	}

	@Override
	public Socket createSocket(String host, int port) throws IOException,
			UnknownHostException {
		javax.net.ssl.SSLSocketFactory socketfact = mSSLContext.getSocketFactory();
		return socketfact.createSocket(host, port);
	}

	@Override
	public Socket createSocket(String host, int port, InetAddress localHost,
			int localPort) throws IOException, UnknownHostException {
		javax.net.ssl.SSLSocketFactory socketfact = mSSLContext.getSocketFactory();
		return socketfact.createSocket(host, port, localHost, localPort);
	}

	@Override
	public Socket createSocket(InetAddress host, int port) throws IOException {
		javax.net.ssl.SSLSocketFactory socketfact = mSSLContext.getSocketFactory();
		return socketfact.createSocket(host, port);
	}

	@Override
	public Socket createSocket(InetAddress address, int port,
			InetAddress localAddress, int localPort) throws IOException {
		javax.net.ssl.SSLSocketFactory socketfact = mSSLContext.getSocketFactory();
		return socketfact.createSocket(address, port, localAddress, localPort);
	}

}
