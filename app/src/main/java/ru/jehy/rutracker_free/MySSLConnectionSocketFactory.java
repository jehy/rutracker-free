package ru.jehy.rutracker_free;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.Socket;

import javax.net.ssl.SSLContext;

import cz.msebera.android.httpclient.conn.ssl.SSLConnectionSocketFactory;
import cz.msebera.android.httpclient.protocol.HttpContext;

/**
 * Created by Jehy on 14.10.2016.
 */

public class MySSLConnectionSocketFactory extends SSLConnectionSocketFactory {

    public MySSLConnectionSocketFactory(final SSLContext sslContext) {
        super(sslContext);
    }

    @Override
    public Socket createSocket(final HttpContext context) throws IOException {
        InetSocketAddress socksaddr = (InetSocketAddress) context.getAttribute("socks.address");
        Proxy proxy = new Proxy(Proxy.Type.SOCKS, socksaddr);
        return new Socket(proxy);
    }

}