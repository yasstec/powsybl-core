package com.powsybl.afs.ws.server.utils.sb;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.GZIPInputStream;

import javax.servlet.ReadListener;
import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;

public class GzippedInputStreamWrapper extends HttpServletRequestWrapper {

    private InputStream in = null;
    public GzippedInputStreamWrapper(final HttpServletRequest request) throws IOException {
        super(request);
        try {
            in = new GZIPInputStream(request.getInputStream());
        } catch (EOFException e) {
            e.printStackTrace();
        }
    }

    @Override
    public ServletInputStream getInputStream() throws IOException {
        return new ServletInputStream() {
            @Override
            public int read() throws IOException {
                return in.read();
            }

            @Override
            public void close() throws IOException {
                super.close();
                in.close();
            }
            @Override
            public boolean isFinished() {
                return false;
            }
            @Override
            public boolean isReady() {
                return false;
            }
            @Override
            public void setReadListener(ReadListener listener) {
            }
        };
    }
}
