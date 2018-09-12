package com.powsybl.afs.ws.server.sb.utils;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.zip.GZIPOutputStream;

import javax.servlet.ServletOutputStream;
import javax.servlet.WriteListener;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;

import org.springframework.http.HttpHeaders;

public class GZIPResponseWrapper  extends HttpServletResponseWrapper {

    private GZIPResponseStream gzipResponse;
    private ServletOutputStream servletOuput;
    private PrintWriter printerWriter;

    public GZIPResponseWrapper(HttpServletResponse response) {
        super(response);
    }

    public void finish() throws IOException {
        if(printerWriter != null) {
            printerWriter.close();
        }
        if(servletOuput != null) {
            servletOuput.close();
        }
        if(gzipResponse != null) {
            gzipResponse.close();
        }
    }

    @Override
    public void flushBuffer() throws IOException {
        if(printerWriter != null) {
            printerWriter.flush();
        }
        if(servletOuput != null) {
            servletOuput.flush();
        }
        super.flushBuffer();
    }

    @Override
    public ServletOutputStream getOutputStream() throws IOException {
        if(servletOuput == null) {
        	if ( super.getHeader(HttpHeaders.CONTENT_ENCODING) != null && super.getHeader(HttpHeaders.CONTENT_ENCODING).equals("gzip")) {
        		gzipResponse = new GZIPResponseStream(new GZIPOutputStream(getResponse().getOutputStream()));
        		servletOuput = gzipResponse;
        	}
        	else { 
        		servletOuput = getResponse().getOutputStream();
        	}
        }
        return servletOuput;
    }
    @Override
    public PrintWriter getWriter() throws IOException {
        if(printerWriter == null) {
        	if ( super.getHeader(HttpHeaders.CONTENT_ENCODING) != null && super.getHeader(HttpHeaders.CONTENT_ENCODING).equals("gzip")) {
        		gzipResponse = new GZIPResponseStream(new GZIPOutputStream(getResponse().getOutputStream()));
        		printerWriter = new PrintWriter(new OutputStreamWriter(gzipResponse, getResponse().getCharacterEncoding()));
        	}
        	else {
        		printerWriter = new PrintWriter(new OutputStreamWriter(getResponse().getOutputStream(), getResponse().getCharacterEncoding()));
        	}
        }
        return printerWriter;
    }

	class GZIPResponseStream extends ServletOutputStream {
	
		private GZIPOutputStream gzipOutput;
	    private final AtomicBoolean open = new AtomicBoolean(true);
	
	    public GZIPResponseStream(GZIPOutputStream zoutput) throws IOException {
	    	this.gzipOutput = zoutput;
	    }
	    @Override
	    public boolean isReady() {
	        return true;
	    }
	    @Override
	    public void setWriteListener(WriteListener arg0) {
	        
	    }
	    @Override
	    public void flush() throws IOException {
	        gzipOutput.flush();
	    }
	    @Override
	    public void close() throws IOException {
	        if(open.compareAndSet(true, false)) {
	            gzipOutput.close();
	        }
	    }
	    @Override
	    public void write(int b) throws IOException {
	        
	    }
	    @Override
	    public void write(byte[] b, int off, int len) throws IOException {
	        if(!open.get()) {
	            throw new IOException("closed");
	        }
	        gzipOutput.write(b, off, len);
	    }
	    @Override
	    public void write(byte[] b) throws IOException {
	        write(b, 0, b.length);
	    }
	}
}
