package biouml.plugins.seek;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.logging.Level;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nonnull;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.http.Header;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicNameValuePair;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import com.developmentontheedge.application.ApplicationUtils;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.DataElementImporter;
import ru.biosoft.access.DataElementImporterRegistry;
import ru.biosoft.access.DataElementImporterRegistry.ImporterInfo;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.FileImporter;
import ru.biosoft.access.FileImporter.FileImportProperties;
import ru.biosoft.access.core.ClassIcon;
import ru.biosoft.analysiscore.AnalysisMethodSupport;
import ru.biosoft.util.TempFiles;
import ru.biosoft.util.XmlUtil;

@ClassIcon ( "resources/SeekSyncAnalysis.png" )
public class SeekSyncAnalysis extends AnalysisMethodSupport<SeekSyncParameters>
{

    public SeekSyncAnalysis(DataCollection<?> origin, String name)
    {
        super( origin, name, new SeekSyncParameters() );
        parameters.setLogger( log );
    }

    @Override
    public Object justAnalyzeAndPut() throws Exception
    {
        String login = parameters.getLogin();
        String password = parameters.getPassword();
        String seekUrl = parameters.getSeekUrl();
        String sessionId = SeekClient.obtainSessionId( seekUrl, login, password );
        if( sessionId == null )
        {
            log.log(Level.SEVERE,  "Wrong username or password" );
            return "Wrong username or password";
        }
        else
        {
            DataElementPath outputFolder = parameters.getOutputPath();
            //String sessionId = SeekClient.obtainSessionId( seekUrl, login, password );
            String[] selectedDataFiles = parameters.getAvailableDataFiles();
            obtainSelectedDataFiles( selectedDataFiles, login, password, seekUrl, outputFolder );
            return null;
        }
    }



    private void obtainSelectedDataFiles(String[] selectedDataFiles, String login, String password, String seekUrl,
            DataElementPath outputFolder) throws ClientProtocolException, IOException
    {
        //        String sessionId = SeekClient.obtainSessionId( seekUrl, login, password );
        //        if( sessionId == null )
        //        {
        //            return;
        //        }
        try (CloseableHttpClient httpClient = HttpClientBuilder.create().build())
        {
            // String baseUrlString = "http://test.genexplain.com/seek/";
            HttpPost httpPost = new HttpPost( seekUrl + "/session" );
            ArrayList<NameValuePair> params = new ArrayList<>();
            params.add( new BasicNameValuePair( "login", login ) );
            params.add( new BasicNameValuePair( "password", password ) );
            httpPost.setEntity( new UrlEncodedFormEntity( params ) );

            CloseableHttpResponse response = httpClient.execute( httpPost );
            String redirectUri = response.getFirstHeader( "location" ).getValue();
            if( !redirectUri.equals( seekUrl ) )
            {
                log.log(Level.SEVERE,  "Can not obtain: Wrong username or password" );
                log.warning( redirectUri + " not " + seekUrl );
                return;
            }
            Header cookie = response.getFirstHeader( "Set-Cookie" );

            String sessionId = cookie.getElements()[0].getValue();// _session_id

            for( String dfInfo : selectedDataFiles )
            {
                String id = dfInfo.split( "\\." )[0];
                String downloadUrl = seekUrl + "/data_files/" + id + "/download";
                syncFile( downloadUrl, outputFolder, sessionId, httpClient );
            }
        }
        // TODO Auto-generated method stub
    }

    private void syncFile(String downloadURL, DataElementPath outputFolder, String sessionId, CloseableHttpClient httpClient)
            throws ClientProtocolException, IOException
    {
        HttpGet httpGet = new HttpGet( downloadURL );
        httpGet.setHeader( "Cookie", "_session_id=" + sessionId );
        log.info( downloadURL + ": Request ready" );
        CloseableHttpResponse response = httpClient.execute( httpGet );
        log.info( downloadURL + ": Response received" );
        try
        {
            Header fileNameParam = response.getFirstHeader( "content-disposition" );
            String fileName = fileNameParam.getElements()[0].getParameterByName( "filename" ).getValue();
            File tempFile = TempFiles.file( "." + fileName );
            FileOutputStream out = new FileOutputStream( tempFile );
            InputStream inputStream = response.getEntity().getContent();
            ApplicationUtils.copyStream( out, inputStream );

            DataCollection<DataElement> deParent = outputFolder.getDataCollection();

            ImporterInfo[] importerInfos = DataElementImporterRegistry.getAutoDetectImporter( tempFile, deParent, true );

            for( ImporterInfo info : importerInfos )
            {
                DataElementImporter importer = info.cloneImporter();
                try
                {
                    DataElement element = importer.doImport( deParent, tempFile, fileName, null, log );
                    if( element == null )
                    {
                        continue;
                    }
                    log.info( "Imported with " + importer.getClass().getName() );
                    return;
                }
                catch( Exception e )
                {
                    //TODO: fix error handling
                    log.log( Level.WARNING, importer.getClass().getName(), e );
                    continue;
                }
            }

            log.warning( "Cannot find importer for '" + fileName + "'. It will be imported as raw file." );
            FileImporter fileImporter = new FileImporter();
            FileImportProperties fiProperties = fileImporter.getProperties( deParent, tempFile, fileName );
            fiProperties.setPreserveExtension( false );
            fileImporter.doImport( deParent, tempFile, fileName, null, log );
        }
        catch( Exception e )
        {
            log.warning( "No access to" + downloadURL );
            return;
        }
    }

    static @Nonnull String[] listAllDataFiles(String login, String password, String baseUrlString, DataElementPath outputFolder)
            throws Exception
    {
        try (CloseableHttpClient httpClient = HttpClientBuilder.create().build())
        {
            // String baseUrlString = "http://test.genexplain.com/seek/";

            HttpPost httpPost = new HttpPost( baseUrlString + "/session" );
            ArrayList<NameValuePair> params = new ArrayList<>();
            params.add( new BasicNameValuePair( "login", login ) );
            params.add( new BasicNameValuePair( "password", password ) );
            httpPost.setEntity( new UrlEncodedFormEntity( params ) );

            CloseableHttpResponse response = httpClient.execute( httpPost );
            String redirectUri = response.getFirstHeader( "location" ).getValue();
            if( !redirectUri.equals( baseUrlString ) )
            {
                //log.log(Level.SEVERE,  "Could not list data files: Wrong username or password" );
                //log.warning( redirectUri + " not " + baseUrlString );
                return new String[] {};
            }

            Header cookie = response.getFirstHeader( "Set-Cookie" );

            String sessionId = cookie.getElements()[0].getValue();// _session_id
            //log.info( "logged in:" + sessionId );

            String dataFilesUrlString = baseUrlString + "/data_files?page=all";
            URL dataFilesUrl = new URL( dataFilesUrlString );
            HttpGet httpGet = new HttpGet( dataFilesUrl.toURI() );
            httpGet.setHeader( "Cookie", "_session_id=" + sessionId );
            httpGet.setHeader( "Accept", "application/xml" );
            response = httpClient.execute( httpGet );
            InputStream inputStream = response.getEntity().getContent();
            Document xmlDoc = createDocument( inputStream );
            Element items = XmlUtil.findElementByTagName( xmlDoc.getDocumentElement(), "items" );
            Iterable<Element> elements = XmlUtil.elements( items, "data_file" );

            class FileInfo implements Comparable<FileInfo>
            {
                final Integer id;
                final String title;
                public FileInfo(String id, String title)
                {
                    this.id = Integer.valueOf( id );
                    this.title = title;
                }
                @Override
                public String toString()
                {
                    return id + ". " + title;
                }
                @Override
                public int compareTo(FileInfo o)
                {
                    return this.id.compareTo( o.id );
                }
            }

            List<FileInfo> dfAttributes = new ArrayList<>();
            for( Element e : elements )
            {
                dfAttributes.add( new FileInfo( e.getAttribute( "id" ), e.getAttribute( "xlink:title" ) ) );
            }
            return dfAttributes.stream().sorted().map( FileInfo::toString ).toArray( String[]::new );
        }
    }

    private static Document createDocument(InputStream stream) throws SAXException, IOException, ParserConfigurationException
    {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        return builder.parse( stream );
    }

}
