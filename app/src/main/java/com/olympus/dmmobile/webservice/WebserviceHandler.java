package com.olympus.dmmobile.webservice;

import android.net.TrafficStats;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URI;
import java.security.GeneralSecurityException;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.HttpResponseException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.scheme.SocketFactory;
import org.apache.http.conn.ssl.BrowserCompatHostnameVerifier;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;

import com.olympus.dmmobile.DMApplication;

/**
 * Class used for handling the web service communication
 * 
 * @version 1.0.1
 * 
 *
 */
public class WebserviceHandler {

	private final int mTimeLimitForRequest = 20 * 1000;
	private String mBackGroundResponse = null;
	private HttpResponse mHttpResponse = null;
	private HttpClient mHttpClient = null;
	private HttpParams mHttpParameters = null;
	private HttpGet mHttpGet = null;
	
	/**
	 * method to create SSLContext initialized with custom trust manager
	 * @return SSLContext instance
	 * @throws GeneralSecurityException
	 */
	public SSLContext createSSLContext() throws GeneralSecurityException{
		
		OlyDMTrustManager olyDMTrustManager = new OlyDMTrustManager();
        TrustManager[] tm = new TrustManager[]{olyDMTrustManager};
        
        SSLContext context = null;
		context = SSLContext.getInstance("TLS");
		//if you don't need client authentication, you can just pass null as the first parameter of SSLContext.init()
		context.init(null, tm, null);
		
		return context;
	}
	
	/**
	 * method to create HttpClient instance and initialize with custom socket factory
	 * @param socketFactory custom socket factory instance
	 * @param httpParameters HttpProtocolParams instance
	 * @return HttpClient instance
	 */
	public HttpClient createHttpClient(SocketFactory socketFactory,HttpParams httpParameters){
		
		/*HttpConnectionParams.setConnectionTimeout(httpParameters, mTimeLimitForRequest);
	    HttpConnectionParams.setSoTimeout(httpParameters, mTimeLimitForRequest);*/
	    HttpProtocolParams.setVersion(httpParameters, HttpVersion.HTTP_1_1);
	    HttpProtocolParams.setContentCharset(httpParameters, HTTP.DEFAULT_CONTENT_CHARSET);
	    HttpProtocolParams.setUseExpectContinue(httpParameters, true);
	    
	    SchemeRegistry schemeRegistry = new SchemeRegistry();
        schemeRegistry.register(new Scheme("http", PlainSocketFactory
                .getSocketFactory(), 80));
	    
        SocketFactory sslSocketFactory = SSLSocketFactory.getSocketFactory();
        if (socketFactory != null) {
            sslSocketFactory = socketFactory;
        }
        schemeRegistry.register (new Scheme ("https",sslSocketFactory, 443));
        
        ClientConnectionManager cm = new ThreadSafeClientConnManager(httpParameters,
                schemeRegistry);

        return new DefaultHttpClient(cm, httpParameters);
		
	}

	/**
	 * Method used to activate the server
	 * 
	 * @param url
	 *            for activation
	 * 
	 * @param xmlrequest
	 *            request for activation
	 * 
	 * @return response from server
	 * 
	 * 
	 */
	public String service_Activation(String url, String xmlrequest) {
		String strResponse = null;
		//System.out.println("activation " + url);
		try {
			HttpParams httpParameters = new BasicHttpParams();
			HttpConnectionParams.setConnectionTimeout(httpParameters,
					mTimeLimitForRequest);
			HttpConnectionParams.setSoTimeout(httpParameters,
					mTimeLimitForRequest);
			
			
			// create SSLContext
			SSLContext sslContext = createSSLContext();
			// create custom socket factory
			OlyDMSSLSocketFactory socketFactory = new OlyDMSSLSocketFactory(sslContext, new BrowserCompatHostnameVerifier());
			//DefaultHttpClient httpClient = new DefaultHttpClient(/*mSafeClientConnManager,*/httpParameters);
		    DefaultHttpClient httpClient = (DefaultHttpClient) createHttpClient(socketFactory,httpParameters);
		    
			HttpPost httppost = new HttpPost(url);
			URI mURi = new URI(url);
			httppost.setURI(mURi);
			httppost.setParams(httpParameters);
			httppost.setHeader("Host", mURi.getHost());
			httppost.addHeader("Content-Type", "application/xml");
			StringEntity entity = new StringEntity(xmlrequest, "UTF-8");
			httppost.setEntity(entity);
			//System.out.println("activation request: " + xmlrequest);
			if (DMApplication.isONLINE()) {
				HttpResponse response = httpClient.execute(httppost);
				BasicResponseHandler responseHandler = new BasicResponseHandler();
				if (response != null) {
					strResponse = responseHandler.handleResponse(response);
				}
			} else {
				strResponse = null;
			}

		} catch (IOException e) {
			//System.out.println(" ++ IOException " + e);
			if (e.getMessage() != null)
				if (e.getMessage().toString().contains("timed out"))
					strResponse = "TimeOut";// For Time Out
		} catch (Exception e) {
			//System.out.println(" ++ Exception ");
			e.printStackTrace();
		}
		return strResponse;
	}
	

	/**
	 * Method to get settings from server
	 * 
	 * 
	 * @param uri
	 *            url
	 * 
	 * @param base64value
	 *            base64encoded value
	 * 
	 * @param host
	 * 
	 * @return response from the server
	 * 
	 * 
	 */
	public String service_Settings(String uri, String base64value) {
		String strResponse = null;
		HttpResponse response = null;
		try {
			HttpParams httpParameters = new BasicHttpParams();
			HttpConnectionParams.setConnectionTimeout(httpParameters,
					mTimeLimitForRequest);
			HttpConnectionParams.setSoTimeout(httpParameters,
					mTimeLimitForRequest);
			
			SSLContext sslContext = createSSLContext();
			OlyDMSSLSocketFactory socketFactory = new OlyDMSSLSocketFactory(sslContext, new BrowserCompatHostnameVerifier());
			//HttpClient client = new DefaultHttpClient(httpParameters);
			DefaultHttpClient httpClient = (DefaultHttpClient) createHttpClient(socketFactory,httpParameters);
			
			HttpGet request = new HttpGet();
			URI mURi = new URI(uri);
			request.setURI(mURi);
			request.setParams(httpParameters);
			request.setHeader("Host", mURi.getHost());
			request.addHeader("X-ILS-Authorization",
					"Basic " + base64value.trim());
			if (DMApplication.isONLINE()) {
				TrafficStats.setThreadStatsTag(10000);
				response = httpClient.execute(request);
				strResponse = EntityUtils.toString(response.getEntity());
			}
		} catch (IOException e) {
			if (e.getMessage() != null)
				if (e.getMessage().toString().contains("timed out"))
					strResponse = "TimeOut";// For Time Out
		} catch (Exception e) {
		}
		return strResponse;
	}

	/**
	 * Method to get settings from server
	 * 
	 * 
	 * @param uri
	 *            url
	 * 
	 * @param base64value
	 *            base64encoded value
	 * 
	 * @param host
	 * 
	 * @return response from the server
	 * 
	 * 
	 */
	public String onRequestBackgroundSettings(String uri, String base64value) {

		mBackGroundResponse = null;
		mHttpResponse = null;
		try {
			//mHttpClient = null;
			mHttpParameters = new BasicHttpParams();
			HttpConnectionParams.setConnectionTimeout(mHttpParameters,
					mTimeLimitForRequest);
			HttpConnectionParams.setSoTimeout(mHttpParameters,
					mTimeLimitForRequest);
			
			SSLContext sslContext = createSSLContext();
			OlyDMSSLSocketFactory socketFactory = new OlyDMSSLSocketFactory(sslContext, new BrowserCompatHostnameVerifier());
			//mHttpClient = new DefaultHttpClient(mHttpParameters);
			DefaultHttpClient mHttpClient = (DefaultHttpClient) createHttpClient(socketFactory,mHttpParameters);
			
			mHttpGet = new HttpGet();
			URI mURi = new URI(uri);
			mHttpGet.setURI(mURi);
			mHttpGet.setParams(mHttpParameters);
			mHttpGet.setHeader("Host", mURi.getHost());
			mHttpGet.addHeader("X-ILS-Authorization",
					"Basic " + base64value.trim());
			if (DMApplication.isONLINE()) {
				mHttpResponse = mHttpClient.execute(mHttpGet);
				mBackGroundResponse = EntityUtils.toString(mHttpResponse
						.getEntity());
			}
		} catch (IOException e) {
			if (e.getMessage() != null)
				if (e.getMessage().toString().contains("timed out"))
					mBackGroundResponse = "TimeOut";// For Time Out
		} catch (Exception e) {
		}
		return mBackGroundResponse;

	}

	/**
	 * Method to send error info to server
	 * 
	 * 
	 * @param base64value
	 *            is the base encoded value
	 * 
	 * @param errorRequest
	 *            is the xml request for error-info
	 * 
	 * 
	 * @param filename
	 *            is the name of error file
	 * 
	 * @param errorUrl
	 * 
	 * @return response from server
	 * 
	 * 
	 */
	public String service_Errorinfo(String base64value, String errorRequest,
			String filename, String errorUrl) {
		String strResponse = null;
		try {
			HttpParams httpParameters = new BasicHttpParams();
			HttpConnectionParams.setConnectionTimeout(httpParameters,
					mTimeLimitForRequest);
			HttpConnectionParams.setSoTimeout(httpParameters,
					mTimeLimitForRequest);
			
			SSLContext sslContext = createSSLContext();
			OlyDMSSLSocketFactory socketFactory = new OlyDMSSLSocketFactory(sslContext, new BrowserCompatHostnameVerifier());
			//DefaultHttpClient httpClient = new DefaultHttpClient();
			DefaultHttpClient httpClient = (DefaultHttpClient) createHttpClient(socketFactory,mHttpParameters);
			
			URI mURi = new URI(errorUrl);
			HttpPost httppost = new HttpPost(mURi);
			httppost.addHeader("Host", mURi.getHost());
			httppost.addHeader("Content-Type",
					"multipart/form-data; boundary=----------ILSBoundary8FD83EF0C2254A9B");
			httppost.addHeader("X-ILS-Authorization",
					"Basic " + base64value.trim());
			String boundary = "------------ILSBoundary8FD83EF0C2254A9B";
			String contentDisposition = "Content-Disposition: form-data; name=\"ils-request\"";
			String contentType_application = "Content-Type: application/xml; charset=\"UTF-8\"";
			String contentDisposition_attachment = "Content-Disposition: attachment; filename=\"Log.xml\"";
			String contentType_xilsdebug = "Content-Type: application/x-ils-debug";
			String ContentTransfer = "Content-Transfer-Encoding:binary";
			StringBuffer requestBody = new StringBuffer();
			requestBody.append(boundary);
			requestBody.append("\n");
			requestBody.append(contentDisposition);
			requestBody.append("\n");
			requestBody.append(contentType_application);
			requestBody.append("\n");
			requestBody.append("\n");
			requestBody.append(errorRequest);
			requestBody.append("\n");
			requestBody.append(boundary);
			requestBody.append("\n");
			requestBody.append(contentDisposition_attachment);
			requestBody.append("\n");
			requestBody.append(contentType_xilsdebug);
			requestBody.append("\n");
			requestBody.append(ContentTransfer);
			requestBody.append("\n");
			requestBody.append("\n");
			requestBody.append(bytetoBinary(filename));
			requestBody.append("\n");
			requestBody.append("\n");
			requestBody.append(boundary);
			requestBody.append("\n");
			String request = requestBody.toString();
			StringEntity entity = new StringEntity(request, "UTF-8");
			// httppost.setEntity(new
			// ByteArrayEntity(request.toString().getBytes("UTF-8")));
			httppost.setEntity(entity);
			for (int i = 0; i < httppost.getAllHeaders().length; i++)
				if (DMApplication.isONLINE()) {
					HttpResponse response = httpClient.execute(httppost);
					if (response != null) {
						try {
							BasicResponseHandler responseHandler = new BasicResponseHandler();

							strResponse = responseHandler
									.handleResponse(response);
						} catch (HttpResponseException e) {
							// e.printStackTrace();
						} catch (IOException e) {
							// e.printStackTrace();
						}
					}
				} else {
					strResponse = null;
				}
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		return strResponse;
	}

	/**
	 * Method used to convert byte to binary
	 * 
	 * @param filename
	 *            name of the file to convert into binary
	 * 
	 * @return processed binary value
	 * 
	 * 
	 */
	public String bytetoBinary(String filename) {
		StringBuilder sb = new StringBuilder();

		try {
			File file = new File(filename);
			DataInputStream input = new DataInputStream(new FileInputStream(
					file));
			String tmp;
			while ((tmp = input.readLine()) != null) {
				sb.append(tmp);

			}
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

		/*
		 * try { while( true ) { sb.append(Integer.toBinaryString(
		 * input.readByte())); } } catch( EOFException eof ) { } catch(
		 * IOException e ) { e.printStackTrace(); }
		 */

		return sb.toString();
	}

	/**
	 * Method used to update os or model of device
	 * 
	 * @param url
	 * 
	 * @param xmlrequest
	 *            is the update request
	 * 
	 * @param base64value
	 *            is the base encoded value
	 * 
	 * @return response from the server
	 * 
	 * 
	 */
	public String service_Update(String url, String xmlrequest,
			String base64value) {
		String responseBody = "";
		try {
			HttpParams httpParameters = new BasicHttpParams();
			HttpConnectionParams.setConnectionTimeout(httpParameters,
					mTimeLimitForRequest);
			HttpConnectionParams.setSoTimeout(httpParameters,
					mTimeLimitForRequest);
			
			SSLContext sslContext = createSSLContext();
			OlyDMSSLSocketFactory socketFactory = new OlyDMSSLSocketFactory(sslContext, new BrowserCompatHostnameVerifier());
			//DefaultHttpClient httpClient = new DefaultHttpClient(httpParameters);
			DefaultHttpClient httpClient = (DefaultHttpClient) createHttpClient(socketFactory,mHttpParameters);
			
			URI mURi = new URI(url);
			HttpPut httpPut = new HttpPut(mURi);
			httpPut.addHeader("Host", mURi.getHost());
			httpPut.addHeader("Content-Type", "application/xml");
			httpPut.addHeader("X-ILS-Authorization",
					"Basic " + base64value.trim());
			StringEntity entity = new StringEntity(xmlrequest, "UTF-8");
			httpPut.setEntity(entity);
			if (DMApplication.isONLINE()) {
				HttpResponse response = httpClient.execute(httpPut);
				int responseCode = response.getStatusLine().getStatusCode();
				if (response != null) {
					HttpEntity e = response.getEntity();
					if (entity != null) {
						responseBody = EntityUtils.toString(e);
					}
				}
			} else {
				responseBody = null;
			}
		} catch (ClientProtocolException e) {
		} catch (IOException e) {
			if (e.getMessage() != null)
				if (e.getMessage().toString().contains("timed out"))
					responseBody = "TimeOut";// For Time Out
		} catch (Exception e) {
			// e.printStackTrace();
		}
		return responseBody;
	}
	
	/**
	 * method to update recipients to ODP
	 * @param url url of ODP
	 * @param xmlrequest request string
	 * @param base64value authentication header 
	 * @return returns the response from the ODP
	 */
	public String updateRecipientsToODP(String url, String xmlrequest,
			String base64value) {
		String responseBody = "";
		//System.out.println("Url ** " + url);
		try {
			HttpParams httpParameters = new BasicHttpParams();
			HttpConnectionParams.setConnectionTimeout(httpParameters,
					mTimeLimitForRequest);
			HttpConnectionParams.setSoTimeout(httpParameters,
					mTimeLimitForRequest);
			
			SSLContext sslContext = createSSLContext();
			OlyDMSSLSocketFactory socketFactory = new OlyDMSSLSocketFactory(sslContext, new BrowserCompatHostnameVerifier());
			//DefaultHttpClient httpClient = new DefaultHttpClient(httpParameters);
			DefaultHttpClient httpClient = (DefaultHttpClient) createHttpClient(socketFactory,httpParameters);
			
			URI mURi = new URI(url);
			HttpPut httpPut = new HttpPut(mURi);
			httpPut.addHeader("Host", mURi.getHost());
			httpPut.addHeader("Content-Type", "application/xml");
			httpPut.addHeader("X-ILS-Authorization",
					"Basic " + base64value.trim());
			StringEntity entity = new StringEntity(xmlrequest, "UTF-8");
			httpPut.setEntity(entity);
			if (DMApplication.isONLINE()) {
				HttpResponse response = httpClient.execute(httpPut);
				int responseCode = response.getStatusLine().getStatusCode();
				if (response != null) {
					HttpEntity e = response.getEntity();
					if (entity != null) {
						responseBody = EntityUtils.toString(e);
					}
				}
			} else {
				responseBody = null;
			}
		} catch (ClientProtocolException e) {
		} catch (IOException e) {
			if (e.getMessage() != null)
				if (e.getMessage().toString().contains("timed out"))
					responseBody = "TimeOut";// For Time Out
		} catch (Exception e) {

		}
		if(responseBody !=null && responseBody.equalsIgnoreCase("")){
			return null;
		}
		return responseBody;
	}
}