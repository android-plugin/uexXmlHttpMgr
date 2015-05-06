package org.zywx.wbpalmstar.plugin.uexmultiHttp;


import java.security.KeyStore;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

public class HX509TrustManager implements X509TrustManager {

	private X509TrustManager mTrustManager; 
	
	public HX509TrustManager(KeyStore ksP12) throws Exception{

		TrustManagerFactory tfactory = TrustManagerFactory.getInstance(Http.algorithm);  
		tfactory.init(ksP12);  
        TrustManager[] trustMgr = tfactory.getTrustManagers();  
        if (trustMgr.length == 0) {  
            throw new NoSuchAlgorithmException("no trust manager found");  
        }  
        mTrustManager = (X509TrustManager)trustMgr[0]; 
        
	}

	@Override
	public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
		
		mTrustManager.checkClientTrusted(chain, authType);
	}

	@Override
	public void checkServerTrusted(X509Certificate[] chain, String authType){
		try{
			if ((chain != null) && (chain.length == 1)) {  
				chain[0].checkValidity();  
	        } else {  
	        	mTrustManager.checkServerTrusted(chain, authType);  
	        } 
		}catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public X509Certificate[] getAcceptedIssuers() {
		
		return null;
	}
	
}
