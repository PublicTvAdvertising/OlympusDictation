package com.olympus.dmmobile.webservice;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

import org.apache.http.conn.ConnectTimeoutException;
import org.apache.http.conn.scheme.LayeredSocketFactory;
import org.apache.http.conn.ssl.X509HostnameVerifier;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;

import com.olympus.dmmobile.Build;


/**
 * A custom SocketFactory class to use custom TrustManager, this class is used with HttpClient API,
 * because HttpClient's SSLSocketFactory doesn't let us specify a custom TrustManager.
 * @version 1.2.1
 *
 */
public class OlyDMSSLSocketFactory implements LayeredSocketFactory {

    private SSLContext sslContext;
    private SSLSocketFactory socketFactory;
    private X509HostnameVerifier hostnameVerifier;

    /**
     * constructor which accepts a SSLContext initialized with custom trust manager(OlyDMTrustManager) and a host name verifier
     * @param sslCtx SSLContext instance
     * @param hostnameVerifier host name verifier instance
     */
    public OlyDMSSLSocketFactory(SSLContext sslCtx,
            X509HostnameVerifier hostnameVerifier) {
        this.sslContext = sslCtx;
        this.socketFactory = sslContext.getSocketFactory();
        this.hostnameVerifier = hostnameVerifier;
    }

    @Override
    public Socket connectSocket(Socket sock, String host, int port,
            InetAddress localAddress, int localPort, HttpParams params)
            throws IOException, UnknownHostException, ConnectTimeoutException {
        if (host == null) {
            throw new IllegalArgumentException("Target host may not be null.");
        }
        if (params == null) {
            throw new IllegalArgumentException("Parameters may not be null.");
        }

        SSLSocket sslsock = (SSLSocket) ((sock != null) ? sock : createSocket());

        if ((localAddress != null) || (localPort > 0)) {
            if (localPort < 0)
                localPort = 0;

            InetSocketAddress isa = new InetSocketAddress(localAddress,
                    localPort);
            sslsock.bind(isa);
        }

        int connTimeout = HttpConnectionParams.getConnectionTimeout(params);
        int soTimeout = HttpConnectionParams.getSoTimeout(params);

        InetSocketAddress remoteAddress = new InetSocketAddress(host, port);
        
        
        sslsock.connect(remoteAddress, connTimeout);

        sslsock.setSoTimeout(soTimeout);
        try {
            hostnameVerifier.verify(host, sslsock);
        } catch (IOException iox) {
        	if(Build.DEBUG)
        		System.out.println(OlyDMSSLSocketFactory.class.getSimpleName() + ": hostname verification error. ");
            try {
                sslsock.close();
            } catch (Exception x) {
            }
            // throws exception if the host name verification fails
            throw iox;
        }

        return sslsock;
    }

    @Override
    public Socket createSocket() throws IOException {
        return socketFactory.createSocket();
    }

    @Override
    public boolean isSecure(Socket sock) throws IllegalArgumentException {
        if (sock == null) {
            throw new IllegalArgumentException("Socket may not be null.");
        }

        if (!(sock instanceof SSLSocket)) {
            throw new IllegalArgumentException(
                    "Socket not created by this factory.");
        }

        if (sock.isClosed()) {
            throw new IllegalArgumentException("Socket is closed.");
        }

        return true;

    }

    @Override
    public Socket createSocket(Socket socket, String host, int port,
            boolean autoClose) throws IOException, UnknownHostException {
    	
        SSLSocket sslSocket = (SSLSocket) socketFactory.createSocket(socket,
                host, port, autoClose);
        hostnameVerifier.verify(host, sslSocket);

        return sslSocket;
    }

}
