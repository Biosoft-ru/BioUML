package ru.biosoft.server.servlets.webservices.providers;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;

import org.jsoup.Connection.Response;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import com.developmentontheedge.application.ApplicationUtils;

import one.util.streamex.EntryStream;
import ru.biosoft.access.security.SecurityManager;
import ru.biosoft.server.servlets.webservices.BiosoftWebRequest;
import ru.biosoft.server.servlets.webservices.BiosoftWebResponse;

public class WikiHelpProvider extends WebProviderSupport
{
    private static final String WIKI_URL = "http://wiki.biouml.org";
    private static final String WIKI_PAGE_PREFIX = WIKI_URL + "/index.php/";

    @Override
    public void process(BiosoftWebRequest args, BiosoftWebResponse resp) throws Exception
    {
        String page = args.get( "page" );
        String search = args.get( "search" );
        
        if( page != null )
            processURL( WIKI_PAGE_PREFIX + page, resp );
        else if(search != null)
        {
            Map<String, String> params = new HashMap<>(args.getArguments());
            params.remove( BiosoftWebRequest.ACTION );
            params.remove( SecurityManager.SESSION_ID );
            processSearch(params, resp);
        }
        else
            throw new IllegalArgumentException("No 'page' or 'search' parameter");
    }

    private void processSearch(Map<String, String> params, BiosoftWebResponse resp) throws Exception
    {
        String url = "http://wiki.biouml.org/index.php?" + EntryStream.of( params )
            .mapKeyValue( (k,v)->k + "=" + encodeURLParam(v) ).joining( "&" );
        
        Response wikiResp = Jsoup.connect( url ).execute();
        if(wikiResp.statusCode() == 200)
        {
            Document doc = wikiResp.parse();
            preprocessDocument( doc );
            sendDocument( resp, doc );
        }
        else if(wikiResp.statusCode() == 302)
        {
            String redirectURL = wikiResp.header( "Location" );
            if(redirectURL != null && redirectURL.startsWith( WIKI_PAGE_PREFIX ))
            {
                processURL( redirectURL, resp );
            }
            else
                throw new Exception("Unexpected response from http://wiki.biouml.org");
        }
        else
            throw new Exception("Unexpected response from http://wiki.biouml.org");
    }
    
    private String encodeURLParam(String s)
    {
        try
        {
            return URLEncoder.encode( s, "UTF-8" );
        }
        catch( UnsupportedEncodingException e )
        {
            throw new RuntimeException(e);
        }
    }
    
    private void processURL(String url, BiosoftWebResponse resp) throws IOException
    {
        Document doc = Jsoup.parse( new URL(url), 3000 );
        preprocessDocument( doc );
        sendDocument( resp, doc );
    }

    private void sendDocument(BiosoftWebResponse resp, Document doc) throws IOException
    {
        resp.getOutputStream().write( doc.toString().getBytes() );
    }

    private void preprocessDocument(Document doc) throws IOException
    {
        doc.select( "body > div:not(#content)" ).remove();
        doc.select( "body > div#content" ).attr( "style", "margin-left: 0" );
        doc.select( "[href]" ).forEach( e -> {
            String oldHRef = e.attr( "href" );
            if( oldHRef.startsWith( "/index.php/" ) )
            {
                String href = "/biouml/web/wikihelp?page=" + oldHRef.substring( "/index.php/".length() );
                e.attr( "href", href );
            }
            else if( oldHRef.startsWith("/index.php?") && oldHRef.contains( "search=" ))
            {
                e.attr( "href", "/biouml/web/wikihelp?" + oldHRef.substring( "index.php?".length() ) );
            }
            else
            {
                String href = oldHRef;
                if( !isExternalHRef( href ) && !href.startsWith( "#" ) )
                {
                    String prefix = href.startsWith( "/" ) ? WIKI_URL : WIKI_PAGE_PREFIX;
                    href = prefix + href;
                    e.attr( "href", href );
                }
                if( e.tagName().equalsIgnoreCase( "a" ) && isExternalHRef( href ) )
                    e.attr( "target", "_blank" );
            }
        } );
        doc.select( "[src]" ).forEach( e -> {
            String oldSrc = e.attr( "src" );
            if( !isExternalHRef( oldSrc ) )
            {
                String prefix = oldSrc.startsWith( "/" ) ? WIKI_URL : WIKI_PAGE_PREFIX;
                String src = prefix + oldSrc;
                e.attr( "src", src );
            }
        } );
        doc.select( "form[action]" ).forEach( e->{
            String action = e.attr( "action" );
            if(action.equals( "/index.php" ))
                e.attr("action", "/biouml/web/wikihelp");
        } );
        
        String header = ApplicationUtils.readAsString( WikiHelpProvider.class.getResourceAsStream( "resources/wiki_help_header.html" ) );
        doc.body().prepend( header );
    }

    private boolean isExternalHRef(String href)
    {
        return href.startsWith( "http://" ) || href.startsWith( "https://" ) || href.startsWith( "//" );
    }

}