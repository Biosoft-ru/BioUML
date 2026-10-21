package biouml.plugins.wdl;

import java.io.File;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Path;

import com.developmentontheedge.application.ApplicationUtils;

import biouml.plugins.wdl.diagram.WDLImporter;
import biouml.plugins.wdl.model.ScriptInfo;
import biouml.plugins.wdl.nextflow.NextFlowImporter;

public class FileScriptLoader extends ScriptLoader
{
    public String rootDir;
    private boolean webBased = false;

    public FileScriptLoader(String type, File rootDir)
    {
        this( type, rootDir.getAbsolutePath(), false );
    }
    
    public FileScriptLoader(String type, String rootDir, boolean webBased)
    {
        super( type );
        this.rootDir = rootDir;
        this.webBased = webBased;
    }

    @Override
    public ScriptInfo loadScript(String path) throws Exception
    {
        if( path.contains( "https://" ) )
        {
            String script = downloadText( path );
            String newRoot = path.substring( 0, path.lastIndexOf( "/" ) );
            return readScript( path, script, newRoot, true );
        }
        else if( webBased )
        {
            String url = rootDir + "/" + path;
            String script = downloadText( url );
            return readScript( path, script, rootDir, true );
        }
        else
        {
            Path rootPath = Path.of( rootDir );
            Path resultPath = rootPath.resolve( Path.of( path ) );
            File resultFile = resultPath.toFile();
            String script = ApplicationUtils.readAsString( resultFile );
            String name = resultFile.getName();
            name = name.substring( 0, name.lastIndexOf( ".wdl" ) );
            return readScript( name, script, resultFile.getParentFile().getAbsolutePath(), false );
        }
    }

    protected ScriptInfo readScript(String name, String script, String newRoot, boolean webBased) throws Exception
    {
        switch( type )
        {
            case WDL_TYPE:
            {
                WDLImporter importer = new WDLImporter();
                importer.setScriptLoader( new FileScriptLoader( WDL_TYPE, newRoot, webBased ) );
                return importer.readScript( name, script );
            }
            case NEXTFLOW_TYPE:
            {
                NextFlowImporter importer = new NextFlowImporter();
                importer.setScriptLoader( new FileScriptLoader( NEXTFLOW_TYPE, newRoot, webBased ) );
                return importer.parseNextflow( name, script );
            }
        }
        return null;
    }

    public static String downloadText(String url) throws Exception
    {
        HttpClient client = HttpClient.newBuilder().followRedirects( HttpClient.Redirect.NORMAL ).build();
        HttpRequest request = HttpRequest.newBuilder().uri( URI.create( url ) ).GET().build();
        HttpResponse<String> response = client.send( request, HttpResponse.BodyHandlers.ofString() );
        if( response.statusCode() != 200 )
            throw new RuntimeException( "HTTP error: " + response.statusCode() );
        return response.body();
    }
}