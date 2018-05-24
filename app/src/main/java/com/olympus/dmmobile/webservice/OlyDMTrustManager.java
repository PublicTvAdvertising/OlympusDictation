package com.olympus.dmmobile.webservice;

import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;

import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

import com.olympus.dmmobile.Build;

import android.util.Log;

/**
 * Custom TrustManager class that implements X509TrustManager interface to validate the 
 * server certificate with the system default Certificate Authorities. 
 * @version 1.2.1
 */
public class OlyDMTrustManager implements X509TrustManager {
	
	String TAG = OlyDMTrustManager.class.getSimpleName();
	private X509TrustManager defaultTrustManager;				// default trust manager instance
	private X509Certificate[] acceptedIssuers;					// an array of X509Certificate of system default certificate authorities
	
	public OlyDMTrustManager() {
		try {
			// initialize the default TrustManagerFactory
			TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
			tmf.init((KeyStore)null);
			
			// create the default trust manager
			defaultTrustManager = getX509TrustManager(tmf);
			
			if(defaultTrustManager == null){
				throw new IllegalStateException("X509TrustManager not found!");
			}
			
			List<X509Certificate> allIssuers = new ArrayList<X509Certificate>();
			for(X509Certificate x509certificate : defaultTrustManager.getAcceptedIssuers()){
				allIssuers.add(x509certificate);
			}
			
			acceptedIssuers = allIssuers.toArray(new X509Certificate[allIssuers.size()]);
			
		} catch(GeneralSecurityException gse){
			if(Build.DEBUG){
				Log.e(TAG, "Error creating TrustManager : "+gse.getLocalizedMessage());
			}
			//throws exception if any error occurs while initializing
			throw new RuntimeException();
		}
		
	}

	@Override
	public void checkClientTrusted(X509Certificate[] chain, String authType)
			throws CertificateException {
		try {
			// checks whether the client is trusted or not
			defaultTrustManager.checkClientTrusted(chain, authType);
			if(Build.DEBUG){
				System.out.println(TAG + " : client trusted ");
			}
		} catch (Exception e) {
			if(Build.DEBUG){
				System.out.println(TAG + " : client not trusted ");
				e.printStackTrace();
			}
			throw new CertificateException();
		}
		
	}

	@Override
	public void checkServerTrusted(X509Certificate[] chain, String authType)
			throws CertificateException {
		try {
			// checks whether the server is trusted or not
			defaultTrustManager.checkServerTrusted(chain, authType);
			if(Build.DEBUG){
				System.out.println(TAG + " : server trusted ");
			}
		} catch (Exception e) {
			if(Build.DEBUG){
				System.out.println(TAG + " : server not trusted ");
				e.printStackTrace();
			}
			throw new CertificateException();
		}
	}

	@Override
	public X509Certificate[] getAcceptedIssuers() {
		return acceptedIssuers;
	}
	
	/**
	 * method to create an X.509 certificate-based TrustManager.
	 * @param tmf TrustManagerFactory instance
	 * @return X509TrustManager instance
	 */
	private static X509TrustManager getX509TrustManager(TrustManagerFactory tmf){
		
		TrustManager[] trustManagers = tmf.getTrustManagers();
		for(int i = 0 ; i < trustManagers.length ; i++){
			if(trustManagers[i] instanceof X509TrustManager){
				return (X509TrustManager) trustManagers[i];
			}
		}
		
		return null;
	}

}
