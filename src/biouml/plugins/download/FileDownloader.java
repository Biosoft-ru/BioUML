package biouml.plugins.download;

import it.sauronsoftware.ftp4j.FTPClient;
import it.sauronsoftware.ftp4j.FTPConnector;
import it.sauronsoftware.ftp4j.FTPException;
import it.sauronsoftware.ftp4j.FTPFile;
import it.sauronsoftware.ftp4j.FTPIllegalReplyException;
import it.sauronsoftware.ftp4j.connectors.DirectConnector;
import it.sauronsoftware.ftp4j.connectors.HTTPTunnelConnector;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import one.util.streamex.StreamEx;

import com.developmentontheedge.application.ApplicationUtils;
import ru.biosoft.jobcontrol.JobControl;
import ru.biosoft.access.exception.BiosoftNetworkException;
import ru.biosoft.access.security.SecurityManager;
import ru.biosoft.util.NetworkConfigurator;

/**
 * static method for file download from external FTP server
 * tries to use direct connect first, in case of exception or timeout tries to connect via HTTP proxy if proxy settings are available in the system
 * tries to resume aborted download 10 times
 */
public class FileDownloader
{
    public static String downloadFileFromFTP(URL url, File destinationFile, FTPUploadListener transferListener, boolean continueDownload) throws Exception
    {
        FTPClient ftpClient = null;
        try
        {
            File file = new File(url.getFile());
            String fileName = file.getName();

            String[] userInfo = new String[] {"anonymous", ""};
            if( url.getUserInfo() != null )
            {
                userInfo = url.getUserInfo().split(":", -1);
            }
            String hostname = url.getHost();
            int port = url.getPort();
            ftpClient = new FTPClient();
            FTPConnector connector = new DirectConnector();
            ftpClient.setConnector(connector);
            try
            {
                if( port != -1 )
                    ftpClient.connect(hostname, port);
                else
                    ftpClient.connect(hostname);
                ftpClient.login(userInfo[0], userInfo[1]);
            }
            catch( IllegalStateException | IOException | FTPIllegalReplyException | FTPException e1 )
            {
                //can not connect or login, try to use proxy settings
                try
                {
                    ftpClient.disconnect(false);
                }
                catch( IllegalStateException | IOException | FTPIllegalReplyException | FTPException e )
                {
                }
                if( NetworkConfigurator.getHost() != null )
                {
                    connector = new HTTPTunnelConnector(NetworkConfigurator.getHost(), NetworkConfigurator.getPort());
                }
                ftpClient.setConnector(connector);
                try
                {
                    if( port != -1 )
                        ftpClient.connect(hostname, port);
                    else
                        ftpClient.connect(hostname);
                    ftpClient.login(userInfo[0], userInfo[1]);
                }
                catch( IllegalStateException | IOException | FTPIllegalReplyException | FTPException e2 )
                {
                    ftpClient.disconnect(false);
                    throw new Exception("FTP server refused connection", e2);
                }
            }
            ftpClient.setType(FTPClient.TYPE_TEXTUAL);
            String parentDir = new File(url.getFile()).getParent();
            ftpClient.changeDirectory(parentDir);
            FTPFile[] files = ftpClient.list();
            ftpClient.setType(FTPClient.TYPE_BINARY);
            long originalFileSize = StreamEx.of( files ).filter( f -> f.getName().equals( fileName ) ).mapToLong( FTPFile::getSize )
                    .findAny().orElseThrow( () -> new Exception( "File " + fileName + " not found on the server " + hostname ) );
            transferListener.setTotalBytesToTransfer(originalFileSize);
            boolean transferred = false;
            int tries = 0;
            long offset = 0;
            if(continueDownload && destinationFile.exists())
                offset = destinationFile.length();
            while( !transferred && tries < 10 )
            {
                try
                {
                    transferListener.setBytesTransferred( offset );
                    ftpClient.download(url.getFile(), destinationFile, offset, transferListener);
                    ftpClient.currentDirectory(); //workaround, since download on user kick is not interrupted
                    transferred = true;
                }
                catch( Exception e )
                {
                    ftpClient.disconnect(false);
                    if( port != -1 )
                        ftpClient.connect(hostname, port);
                    else
                        ftpClient.connect(hostname);
                    ftpClient.login(userInfo[0], userInfo[1]);
                    ftpClient.setType(FTPClient.TYPE_BINARY);
                    //TODO: correct transfer resuming
                    try (FileOutputStream fos = new FileOutputStream( destinationFile, true ))
                    {
                        long downloadedFileSize = fos.getChannel().size();
                        if( downloadedFileSize > 0 && downloadedFileSize < originalFileSize )
                        {
                            offset = downloadedFileSize;
                        }
                    }
                    tries++;
                }
            }
            return fileName;
        }
        catch( Exception e )
        {
            throw new BiosoftNetworkException( e, url.toString() );
        }
        finally
        {
            try
            {
                if(ftpClient != null)
                    ftpClient.disconnect(true);
            }
            catch( IllegalStateException | IOException | FTPIllegalReplyException | FTPException e )
            {
            }
        }
    }
    
    public static String downloadFileFromHTTP(URL url, File destinationFile, JobControl jobControl, boolean continueDownload) throws Exception
    {
        if(continueDownload && (!destinationFile.exists() || destinationFile.length() == 0))
            continueDownload = false;
        
        HttpURLConnection connection = (HttpURLConnection)url.openConnection(NetworkConfigurator.getProxyObject());
        connection.setRequestProperty("Cache-Control", "no-cache");
        connection.setConnectTimeout( 10000 );
        connection.setReadTimeout( 10000 );
        if(continueDownload)
            connection.setRequestProperty( "Range", "bytes=" + destinationFile.length() + "-" );
        // TODO: support retrying upon broken connection
        //TODO: support If-Range header
        if(connection.getResponseCode() != 206)//206 Partial download
            continueDownload = false;
        long downloadedLength = continueDownload ? destinationFile.length() : 0;
        long length = downloadedLength + connection.getContentLengthLong();
        String disposition = connection.getHeaderField("Content-Disposition");
        String fileName = new File(url.getFile()).getName();
        if(disposition != null)
        {
            Matcher matcher = Pattern.compile("filename\\s*=\\s*(\"[^\"]+\"|[^\"]\\S+)").matcher(disposition);
            if(matcher.find())
            {
                fileName = matcher.group(1).replaceFirst("^\"(.+)\"$", "$1");
            }
        }
        
        final int BUFFER_SIZE = 64 * 1024;
        
        
        try (InputStream src = connection.getInputStream();
                OutputStream dst = new FileOutputStream( destinationFile, continueDownload);
                BufferedInputStream bis = new BufferedInputStream( src );
                BufferedOutputStream bos = new BufferedOutputStream(dst);)
        {
            byte[] buffer = new byte [BUFFER_SIZE];
            int len;

            while( (len = bis.read(buffer)) != -1 )
            {
                downloadedLength+=len;
                bos.write(buffer, 0, len);
                if( jobControl != null && length >= downloadedLength )
                    jobControl.setPreparedness((int)(downloadedLength*100.0/length));
            }
        }
        return fileName;
    }

    public static String downloadFile(URL url, File destinationFile, JobControl jobControl, boolean continueDownload) throws Exception
    {
        if(url.getProtocol().equalsIgnoreCase("file"))
        {
            File file = new File(url.toURI());
            if(!SecurityManager.isAdmin() &&
                    !SecurityManager.getSecurityProvider().isOnAllowedPath(file))
                    throw new SecurityException("File URL's are disabled");
            
            if(continueDownload && destinationFile.exists() && destinationFile.length() == file.length())
                return file.getName();
            ApplicationUtils.linkOrCopyFile(destinationFile, file, jobControl);
            return file.getName();
        } else if(url.getProtocol().equalsIgnoreCase("ftp"))
            return downloadFileFromFTP(url, destinationFile, new FTPUploadListener(jobControl, 0, 100), continueDownload);
        else if(url.getProtocol().equalsIgnoreCase("http") || url.getProtocol().equalsIgnoreCase("https"))
            return downloadFileFromHTTP(url, destinationFile, jobControl, continueDownload);
        else
            throw new Exception("Invalid protocol: "+url.getProtocol());
    }
    
    public static String downloadFile(URL url, File destinationFile, JobControl jobControl) throws Exception
    {
        return downloadFile( url, destinationFile, jobControl, false );
    }

    private static class URLConversion
    {
        private final String name;
        private final Pattern original;
        private final String replacement;

        public URLConversion(String name, String pattern, String replacement)
        {
            this.name = name;
            this.original = Pattern.compile( pattern, Pattern.CASE_INSENSITIVE );
            this.replacement = replacement;
        }

        public URL convert(URL input) throws MalformedURLException
        {
            String urlString = input.toExternalForm();
            Matcher matcher = original.matcher( urlString );
            if( matcher.matches() )
            {
                return new URL( matcher.replaceFirst( replacement ) );
            }
            return null;
        }

        @Override
        public String toString()
        {
            return name;
        }
    }

    private static List<URLConversion> urlConversions = Arrays.asList( new URLConversion( "GEO page",
            "http://www.ncbi.nlm.nih.gov/geo/query/acc.cgi\\?acc=(.+)", "http://www.ncbi.nlm.nih.gov/geosuppl/?acc=$1" ) );

    public static URL convertURL(URL url, Appendable messages) throws Exception
    {
        for( URLConversion urlConversion : urlConversions )
        {
            URL converted = urlConversion.convert( url );
            if( converted != null )
            {
                if( messages != null )
                    messages.append( urlConversion + " URL detected; converted to:\n" + converted );
                return converted;
            }
        }
        return url;
    }

    /**
     * Get info about file from HTTP header.
     * If URL is HTTP, returns file name and size.
     * For other remote files returns only name detected using URL string.
     * @param url
     * @return
     */
    public static RemoteFileInfo getFileInfoFromHeader(URL url)
    {
        String path = url.getPath();
        String fileName = "";
        int size = -1;

        HttpURLConnection connection = null;
        try
        {
            /*
             * Content-Disposition usually has following format:
             * 'attachment; filename="GSE100383_RAW.tar"; size=61470720'
             */
            connection = (HttpURLConnection)url.openConnection( NetworkConfigurator.getProxyObject() );
            String header = connection.getHeaderField( "Content-Disposition" );
            String[] headerParts = header.split( ";" );
            for( String part : headerParts )
            {
                part = part.trim();
                if( part.startsWith( "filename=" ) )
                    fileName = part.substring( 9 ).replace( "\"", "" ).replace( "'", "" );
                else if( part.startsWith( "size=" ) )
                    size = Integer.parseInt( part.substring( 5 ) );
            }
            if( fileName.isEmpty() )
                fileName = path.substring( path.lastIndexOf( '/' ) + 1 );
        }
        catch( Exception e )
        {
            fileName = path.substring( path.lastIndexOf( '/' ) + 1 );
        }
        finally
        {
            if( connection != null )
                connection.disconnect();
        }
        return new RemoteFileInfo( fileName, size );
    }

    public static class RemoteFileInfo
    {
        private final String fileName;
        private final int size;

        public RemoteFileInfo(String fileName, int size)
        {
            this.fileName = fileName;
            this.size = size;
        }
        public String getFileName()
        {
            return fileName;
        }
        public int getFileSize()
        {
            return size;
        }
    }
}
