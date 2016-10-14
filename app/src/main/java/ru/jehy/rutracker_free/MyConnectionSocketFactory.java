package ru.jehy.rutracker_free;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.Socket;
import java.net.SocketTimeoutException;

import cz.msebera.android.httpclient.HttpHost;
import cz.msebera.android.httpclient.conn.ConnectTimeoutException;
import cz.msebera.android.httpclient.conn.socket.ConnectionSocketFactory;
import cz.msebera.android.httpclient.protocol.HttpContext;

/**
 * Created by Jehy on 14.10.2016.
 */


public class MyConnectionSocketFactory implements ConnectionSocketFactory {

    @Override
    public Socket createSocket(final HttpContext context) throws IOException {
        InetSocketAddress socksaddr = (InetSocketAddress) context.getAttribute("socks.address");
        Proxy proxy = new Proxy(Proxy.Type.SOCKS, socksaddr);
        return new Socket(proxy);
    }

    @Override
    public Socket connectSocket(
            final int connectTimeout,
            final Socket socket,
            final HttpHost host,
            final InetSocketAddress remoteAddress,
            final InetSocketAddress localAddress,
            final HttpContext context) throws IOException, ConnectTimeoutException {
        Socket sock;
        if (socket != null) {
            sock = socket;
        } else {
            sock = createSocket(context);
        }
        if (localAddress != null) {
            sock.bind(localAddress);
        }
        try {
            sock.connect(remoteAddress, connectTimeout);
        } catch (SocketTimeoutException ex) {
            throw new ConnectTimeoutException(ex, host, remoteAddress.getAddress());
        }
        return sock;
    }

}
