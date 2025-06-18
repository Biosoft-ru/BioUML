package ru.biosoft.server.servlets.webservices;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileItemFactory;
import org.apache.commons.fileupload.FileUpload;
import org.apache.commons.fileupload.RequestContext;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.http.ConnectionClosedException;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.HttpServerConnection;
import org.apache.http.HttpStatus;
import org.apache.http.MethodNotSupportedException;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.DefaultConnectionReuseStrategy;
import org.apache.http.impl.DefaultHttpResponseFactory;
import org.apache.http.impl.DefaultHttpServerConnection;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.CoreConnectionPNames;
import org.apache.http.params.CoreProtocolPNames;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.BasicHttpProcessor;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpRequestHandler;
import org.apache.http.protocol.HttpRequestHandlerRegistry;
import org.apache.http.protocol.HttpService;
import org.apache.http.protocol.ResponseConnControl;
import org.apache.http.protocol.ResponseContent;
import org.apache.http.protocol.ResponseDate;
import org.apache.http.protocol.ResponseServer;
import org.apache.http.util.EntityUtils;

import java.util.logging.Level;
import java.util.logging.Logger;

import ru.biosoft.access.security.SecurityManager;
import ru.biosoft.util.TextUtil2;

/**
 * @author lan
 *
 */
public class HttpServer
{
    private static final Logger log = Logger.getLogger(HttpServer.class.getName());
    private Thread listenerThread;
    private final int port;
    private final String sessionId;

    public HttpServer(int port, String sessionId)
    {
        this.port = port;
        this.sessionId = sessionId;
        SecurityManager.addThreadToSessionRecord(Thread.currentThread(), SecurityManager.SYSTEM_SESSION);
        SecurityManager.anonymousLogin();
    }

    public void startServer() throws IOException
    {
        if(listenerThread != null)
        {
            throw new IllegalStateException("Already started");
        }
        listenerThread = new RequestListenerThread(port, "/biouml/web/", sessionId);
        listenerThread.setDaemon(true);
        listenerThread.start();
    }

    public void stopServer()
    {
        if(listenerThread == null)
        {
            throw new IllegalStateException("Not started");
        }
        listenerThread.interrupt();
    }

    public static class ServerHttpResponseWrapper
    {
        private final HttpResponse response;

        public ServerHttpResponseWrapper(HttpResponse response)
        {
            this.response = response;
        }

        public void setContentType(String contentType)
        {
            setHeader("Content-type", contentType);
        }

        public void setHeader(String key, String value)
        {
            response.setHeader(key, value);
        }
    }

    private static class MultipartFormRequestContext implements RequestContext
    {
        private final HttpEntity entity;

        public MultipartFormRequestContext(HttpEntity entity)
        {
            this.entity = entity;
        }

        @Override
        public String getCharacterEncoding()
        {
            return entity.getContentEncoding() == null ? "UTF-8" : entity.getContentEncoding().getValue();
        }

        @Override
        public int getContentLength()
        {
            return (int)entity.getContentLength();
        }

        @Override
        public String getContentType()
        {
            return entity.getContentType() == null ? "text/plain" : entity.getContentType().getValue();
        }

        @Override
        public InputStream getInputStream() throws IOException
        {
            return entity.getContent();
        }
    }

    private static class WebProviderHandler implements HttpRequestHandler
    {
        private final String docRoot;
        private final SystemSession session = new SystemSession();
        private final WebServicesServlet servlet = new WebServicesServlet();
        private final String sessionId;

        public WebProviderHandler(final String docRoot, String sessionId)
        {
            super();
            this.docRoot = docRoot;
            this.sessionId = sessionId;
        }

        @Override
        public void handle(final HttpRequest request, final HttpResponse response, final HttpContext context) throws HttpException,
                IOException
        {
            String method = request.getRequestLine().getMethod().toUpperCase(Locale.ENGLISH);
            if( !method.equals("GET") && !method.equals("HEAD") && !method.equals("POST") )
            {
                throw new MethodNotSupportedException(method + " method not supported");
            }
            String sessionId = "";
            for(Header header: request.getHeaders("Cookie"))
            {
                String[] fields = TextUtil2.split(header.getValue(), '=');
                if(fields.length >= 2 && fields[0].equals("JSESSIONID"))
                    sessionId = fields[1];
            }
            if(!sessionId.equals(this.sessionId))
            {
                sendForbidden(response);
                return;
            }
            String target = request.getRequestLine().getUri();
            if(!target.startsWith(docRoot))
            {
                sendNotFound(response, target);
                return;
            }
            int pos = target.indexOf('?');
            List<String> uriParameters = new ArrayList<>();
            if( pos >= 0 )
            {
                uriParameters.addAll(Arrays.asList(TextUtil2.split(target.substring(pos+1), '&')));
                target = target.substring(0, pos);
            }
            final String subTarget = target.substring(1);
            final Map<String, Object> arguments = new HashMap<>();
            response.setStatusCode(HttpStatus.SC_OK);
            final ByteArrayOutputStream out = new ByteArrayOutputStream();
            try
            {
                if( request instanceof HttpEntityEnclosingRequest )
                {
                    HttpEntity entity = ( (HttpEntityEnclosingRequest)request ).getEntity();
                    MultipartFormRequestContext requestContext = new MultipartFormRequestContext(entity);
                    if(!FileUpload.isMultipartContent(requestContext))
                    {
                        uriParameters.addAll(Arrays.asList(TextUtil2.split(new String(EntityUtils.toByteArray(entity)), '&')));
                    }
                    else
                    {
                        // Create a factory for disk-based file items
                        FileItemFactory factory = new DiskFileItemFactory();

                        // Create a new file upload handler
                        FileUpload upload = new FileUpload(factory);

                        // Parse the request
                        List /* FileItem */items = upload.parseRequest(requestContext);
                        // Process the uploaded items
                        Iterator iter = items.iterator();
                        while( iter.hasNext() )
                        {
                            FileItem item = (FileItem)iter.next();

                            if( item.isFormField() )
                            {
                                String name = item.getFieldName();
                                String value = item.getString();
                                arguments.put(name, new String[] {value});
                            }
                            else
                            {
                                String name = item.getFieldName();
                                arguments.put(name, new FileItem[] {item});
                            }
                        }
                    }
                }
                for(String uriParameter: uriParameters)
                {
                    String[] fields = TextUtil2.split(uriParameter, '=');
                    if(fields.length > 1)
                        arguments.put( TextUtil2.decodeURL( fields[0] ), new String[] {TextUtil2.decodeURL( fields[1] )} );
                }

                servlet.service(subTarget, session, arguments, out, new ServerHttpResponseWrapper(response));
            }
            catch( Throwable t )
            {
                new JSONResponse(out).error(t);
            }
            response.setEntity(new ByteArrayEntity(out.toByteArray()));
        }

        private void sendForbidden(HttpResponse response)
        {
            response.setStatusCode(HttpStatus.SC_FORBIDDEN);
            String forbiddenMessage = "<html><body><h1>Access to this server is forbidden</h1></body></html>";
            ByteArrayEntity body = new ByteArrayEntity(forbiddenMessage.getBytes());
            body.setContentType("text/html; charset=UTF-8");
            response.setEntity(body);
        }

        private void sendNotFound(final HttpResponse response, final String target)
        {
            response.setStatusCode(HttpStatus.SC_NOT_FOUND);
            String forbiddenMessage = "<html><body><h1>The requested URL "+target+" is not found</h1></body></html>";
            ByteArrayEntity body = new ByteArrayEntity(forbiddenMessage.getBytes());
            body.setContentType("text/html; charset=UTF-8");
            response.setEntity(body);
        }
    }

    private static class RequestListenerThread extends Thread
    {
        private final ServerSocket serversocket;
        private final HttpParams params;
        private final HttpService httpService;

        public RequestListenerThread(int port, final String docroot, String sessionId) throws IOException
        {
            super("HTTP server at port "+port);
            this.serversocket = new ServerSocket(port, 50, InetAddress.getByName("127.0.0.1"));
            this.params = new BasicHttpParams();
            this.params.setIntParameter(CoreConnectionPNames.SO_TIMEOUT, 5000)
                    .setIntParameter(CoreConnectionPNames.SOCKET_BUFFER_SIZE, 8 * 1024)
                    .setBooleanParameter(CoreConnectionPNames.STALE_CONNECTION_CHECK, false)
                    .setBooleanParameter(CoreConnectionPNames.TCP_NODELAY, true)
                    .setParameter(CoreProtocolPNames.ORIGIN_SERVER, "HttpComponents/1.1");

            // Set up the HTTP protocol processor
            BasicHttpProcessor httpproc = new BasicHttpProcessor();
            httpproc.addInterceptor(new ResponseDate());
            httpproc.addInterceptor(new ResponseServer());
            httpproc.addInterceptor(new ResponseContent());
            httpproc.addInterceptor(new ResponseConnControl());

            // Set up request handlers
            HttpRequestHandlerRegistry reqistry = new HttpRequestHandlerRegistry();
            reqistry.register("*", new WebProviderHandler(docroot, sessionId));

            // Set up the HTTP service
            this.httpService = new HttpService(httpproc, new DefaultConnectionReuseStrategy(), new DefaultHttpResponseFactory());
            this.httpService.setParams(this.params);
            this.httpService.setHandlerResolver(reqistry);
        }

        @Override
        public void run()
        {
            log.info("HTTP server: Listening on port " + this.serversocket.getLocalPort());
            while( !Thread.interrupted() )
            {
                try
                {
                    // Set up HTTP connection
                    Socket socket = this.serversocket.accept();
                    DefaultHttpServerConnection conn = new DefaultHttpServerConnection();
                    log.log(Level.FINE, "HTTP server: Incoming connection from " + socket.getInetAddress());
                    conn.bind(socket, this.params);

                    // Start worker thread
                    Thread t = new WorkerThread(this.httpService, conn);
                    t.setDaemon(true);
                    t.start();
                }
                catch( InterruptedIOException ex )
                {
                    break;
                }
                catch( IOException e )
                {
                    log.log(Level.SEVERE, "HTTP server: I/O error initialising connection thread: " + e.getMessage());
                    break;
                }
            }
        }
    }

    private static class WorkerThread extends Thread
    {
        private final HttpService httpservice;
        private final HttpServerConnection conn;

        public WorkerThread(final HttpService httpservice, final HttpServerConnection conn)
        {
            super("HTTP worker");
            this.httpservice = httpservice;
            this.conn = conn;
        }

        @Override
        public void run()
        {
            log.log(Level.FINE, "HTTP server: New connection thread");
            HttpContext context = new BasicHttpContext(null);
            try
            {
                while( !Thread.interrupted() && this.conn.isOpen() )
                {
                    this.httpservice.handleRequest(this.conn, context);
                }
            }
            catch( ConnectionClosedException ex )
            {
                log.log(Level.FINE, "HTTP server: Client closed connection");
            }
            catch( IOException ex )
            {
                log.log(Level.FINE, "HTTP server: I/O error: " + ex.getMessage());
            }
            catch( HttpException ex )
            {
                log.log(Level.FINE, "HTTP server: Unrecoverable HTTP protocol violation: " + ex.getMessage());
            }
            finally
            {
                try
                {
                    this.conn.shutdown();
                }
                catch( IOException ignore )
                {
                }
            }
        }
    }
}
