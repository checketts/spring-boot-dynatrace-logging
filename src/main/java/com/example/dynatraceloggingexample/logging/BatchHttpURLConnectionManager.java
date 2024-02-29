package com.example.dynatraceloggingexample.logging;

import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.HttpManager;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.ConfigurationException;
import org.apache.logging.log4j.core.config.Property;
import org.apache.logging.log4j.core.net.ssl.LaxHostnameVerifier;
import org.apache.logging.log4j.core.net.ssl.SslConfiguration;
import org.apache.logging.log4j.core.util.IOUtils;

import javax.net.ssl.HttpsURLConnection;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.Objects;
import java.util.concurrent.ArrayBlockingQueue;


public class BatchHttpURLConnectionManager extends HttpManager {

    private static final Charset CHARSET = Charset.forName("US-ASCII");

    private final URL url;
    private final boolean isHttps;
    private final String method;
    private final int connectTimeoutMillis;
    private final int readTimeoutMillis;
    private final Property[] headers;
    private final SslConfiguration sslConfiguration;
    private final boolean verifyHostname;
    private final ArrayBlockingQueue<byte[]> batch;
    private final int batchSize = 10;


    public BatchHttpURLConnectionManager(final Configuration configuration, final LoggerContext loggerContext, final String name,
                                    final URL url, final String method, final int connectTimeoutMillis,
                                    final int readTimeoutMillis,
                                    final Property[] headers,
                                    final SslConfiguration sslConfiguration,
                                    final boolean verifyHostname) {
        super(configuration, loggerContext, name);
        this.url = url;
        if (!(url.getProtocol().equalsIgnoreCase("http") || url.getProtocol().equalsIgnoreCase("https"))) {
            throw new ConfigurationException("URL must have scheme http or https");
        }
        this.isHttps = this.url.getProtocol().equalsIgnoreCase("https");
        this.method = Objects.requireNonNull(method, "method");
        this.connectTimeoutMillis = connectTimeoutMillis;
        this.readTimeoutMillis = readTimeoutMillis;
        this.headers = headers != null ? headers : Property.EMPTY_ARRAY;
        this.sslConfiguration = sslConfiguration;
        if (this.sslConfiguration != null && !isHttps) {
            throw new ConfigurationException("SSL configuration can only be specified with URL scheme https");
        }
        this.verifyHostname = verifyHostname;
        this.batch = new ArrayBlockingQueue<>(100000);

    }



    @Override
    public void send(final Layout<?> layout, final LogEvent event) throws IOException {
        final HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
        urlConnection.setAllowUserInteraction(false);
        urlConnection.setDoOutput(true);
        urlConnection.setDoInput(true);
        urlConnection.setRequestMethod(method);
        if (connectTimeoutMillis > 0) {
            urlConnection.setConnectTimeout(connectTimeoutMillis);
        }
        if (readTimeoutMillis > 0) {
            urlConnection.setReadTimeout(readTimeoutMillis);
        }
        if (layout.getContentType() != null) {
            urlConnection.setRequestProperty("Content-Type", layout.getContentType());
        }
        for (final Property header : headers) {
            urlConnection.setRequestProperty(header.getName(), header.evaluate(getConfiguration().getStrSubstitutor()));
        }
        if (sslConfiguration != null) {
            ((HttpsURLConnection) urlConnection).setSSLSocketFactory(sslConfiguration.getSslSocketFactory());
        }
        if (isHttps && !verifyHostname) {
            ((HttpsURLConnection) urlConnection).setHostnameVerifier(LaxHostnameVerifier.INSTANCE);
        }

        final byte[] msg = layout.toByteArray(event);
        batch.add(msg);
        if(batch.size() > batchSize) {
            send(urlConnection);
        }

    }

    private void send(HttpURLConnection urlConnection) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        bos.write("[".getBytes());
        while(!batch.isEmpty()) {
            try {
                bos.write(batch.take());
                if(!batch.isEmpty()) {
                    bos.write(",".getBytes());
                } else {
                    bos.write("]".getBytes());
                }
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
        byte[] content = bos.toByteArray();

        urlConnection.setFixedLengthStreamingMode(content.length);
        urlConnection.connect();
        try (final OutputStream os = urlConnection.getOutputStream()) {
            os.write(content);
        }

        final byte[] buffer = new byte[1024];
        try (final InputStream is = urlConnection.getInputStream()) {
            while (IOUtils.EOF != is.read(buffer)) {
                // empty
            }
        } catch (final IOException e) {
            final StringBuilder errorMessage = new StringBuilder();
            try (final InputStream es = urlConnection.getErrorStream()) {
                errorMessage.append(urlConnection.getResponseCode());
                if (urlConnection.getResponseMessage() != null) {
                    errorMessage.append(' ').append(urlConnection.getResponseMessage());
                }
                if (es != null) {
                    errorMessage.append(" - ");
                    int n;
                    while (IOUtils.EOF != (n = es.read(buffer))) {
                        errorMessage.append(new String(buffer, 0, n, CHARSET));
                    }
                }
            }
            if (urlConnection.getResponseCode() > -1) {
                throw new IOException(errorMessage.toString());
            } else {
                throw e;
            }
        }
    }

}
