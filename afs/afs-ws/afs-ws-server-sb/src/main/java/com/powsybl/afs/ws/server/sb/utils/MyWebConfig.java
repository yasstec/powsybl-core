package com.powsybl.afs.ws.server.sb.utils;



import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ReadListener;
import javax.servlet.ServletException;
import javax.servlet.ServletInputStream;
import javax.servlet.ServletOutputStream;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.WriteListener;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;

import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;




@Configuration
public class MyWebConfig implements Filter   {
	@Override
	public final void doFilter(final ServletRequest servletRequest,
	                           final ServletResponse servletResponse,
	                           final FilterChain chain) throws IOException, ServletException {
				
	    HttpServletRequest request = (HttpServletRequest) servletRequest;
	    HttpServletResponse response = (HttpServletResponse) servletResponse;

	    
	    
	    boolean isGzipped = request.getHeader(HttpHeaders.CONTENT_ENCODING) != null && request.getHeader(HttpHeaders.CONTENT_ENCODING).contains("gzip");
	    
	    boolean requestTypeSupported = "POST".equals(request.getMethod()) ||  "PUT".equals(request.getMethod());
	    if (isGzipped && !requestTypeSupported) {
	        throw new IllegalStateException(request.getMethod()
	                + " is not supports gzipped body of parameters."
	                + " Only POST requests are currently supported.");
	    }
	    if (isGzipped && requestTypeSupported) {
	        request = new GzippedInputStreamWrapper((HttpServletRequest) servletRequest);
	    }
	    //TODO: mettre plus propre
	    if (request.getRequestURL().toString().contains("timeSeries/name") || request.getRequestURL().toString().contains("timeSeries/metadata") 
	    		|| request.getRequestURL().toString().contains("timeSeries/double") || request.getRequestURL().toString().contains("timeSeries/string")) {
	    	GZIPResponseWrapper gzipResponse = new GZIPResponseWrapper(response);
	    	chain.doFilter(request, gzipResponse);
	    	gzipResponse.finish();
	    } else {
	    	chain.doFilter(request, response);
	    }
	}

	@Override
	public void init(FilterConfig filterConfig) throws ServletException {
	}

	@Override
	public void destroy() {
		// TODO Auto-generated method stub
		
	}
}

class GZIPResponseWrapper extends HttpServletResponseWrapper {

    private GZIPResponseStream gzipResponse;
    private ServletOutputStream servletOuput;
    private PrintWriter printerWriter;

    public GZIPResponseWrapper(HttpServletResponse response) {
        super(response);
        response.addHeader("Content-Encoding", "gzip");
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
            gzipResponse = new GZIPResponseStream(getResponse().getOutputStream());
            servletOuput = gzipResponse;
        }
        return servletOuput;
    }

    @Override
    public PrintWriter getWriter() throws IOException {
        if(printerWriter == null) {
            gzipResponse = new GZIPResponseStream(getResponse().getOutputStream());
            printerWriter = new PrintWriter(new OutputStreamWriter(gzipResponse, getResponse().getCharacterEncoding()));
        }
        return printerWriter;
    }

}
class GZIPResponseStream extends ServletOutputStream {

    private GZIPOutputStream gzipOutput;
    private final AtomicBoolean open = new AtomicBoolean(true);

    public GZIPResponseStream(OutputStream output) throws IOException {
        this.gzipOutput = new GZIPOutputStream(output);
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













final class GzippedInputStreamWrapper extends HttpServletRequestWrapper {

    private InputStream in = null;
    public GzippedInputStreamWrapper(final HttpServletRequest request) throws IOException {
        super(request);
        try {
            in = new GZIPInputStream(request.getInputStream());
        } catch (EOFException e) {
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
				// TODO Auto-generated method stub
				return false;
			}
			@Override
			public boolean isReady() {
				// TODO Auto-generated method stub
				return false;
			}
			@Override
			public void setReadListener(ReadListener listener) {
				// TODO Auto-generated method stub
				
			}
        };
    }
}
