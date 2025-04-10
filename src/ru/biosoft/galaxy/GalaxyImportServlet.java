package ru.biosoft.galaxy;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.InvalidParameterException;
import java.util.Map;
import java.util.logging.Level;

import org.apache.commons.lang.StringEscapeUtils;

import com.developmentontheedge.application.Application;

import one.util.streamex.EntryStream;
import ru.biosoft.access.core.CollectionFactory;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.DataElementImporter;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.server.AbstractServlet;
import ru.biosoft.server.servlets.webservices.WebSession;
import ru.biosoft.util.NetworkConfigurator;
import ru.biosoft.util.TempFiles;
import ru.biosoft.util.TextUtil2;

/**
 * @author lan
 *
 */
public class GalaxyImportServlet extends AbstractServlet
{
    @Override
    public String service(String localAddress, Object session, Map params, OutputStream out, Map<String, String> header)
    {
        try (PrintWriter pw = new PrintWriter( out ))
        {
            WebSession.getSession(session);
            try
            {
                Map<String, String> parameters = convertParams(params);
                if(!parameters.containsKey("dc"))
                    throw new InvalidParameterException("dc is missing");
                if(!parameters.containsKey("tool"))
                    throw new InvalidParameterException("tool is missing");
                String dcName = parameters.get("dc");
                DataCollection<?> dc = CollectionFactory.getDataCollection( dcName );
                if(dc == null) throw new InvalidParameterException("Unable to open DC "+dcName);
                String toolName = parameters.get("tool");
                // hack to deal with bases which do not add parameters to query string in a nice way
                if(toolName.contains("?"))
                {
                    String[] fields = toolName.split("\\?");
                    toolName = fields[0];
                    fields = TextUtil2.split( fields[1], '=' );
                    parameters.put(fields[0], fields[1]);
                }
                DataElement toolElement = CollectionFactory.getDataElement(toolName);
                if(toolElement == null) throw new InvalidParameterException("Unable to open tool "+toolName);
                if(!(toolElement instanceof DataSourceMethodInfo))
                    throw new InvalidParameterException("Tool "+toolName+" is not valid Galaxy tool");
                DataSourceMethodInfo tool = (DataSourceMethodInfo)toolElement;
                if(parameters.containsKey("URL"))
                {
                    String url = parameters.get("URL");
                    String queryStr = new URL(url).getQuery();
                    if( queryStr != null )
                    {
                        String[] query = queryStr.split("[\\&\\;]");
                        for( String queryParameter : query )
                        {
                            String[] fields = TextUtil2.split( queryParameter, '=' );
                            parameters.put( TextUtil2.decodeURL( fields[0] ), TextUtil2.decodeURL( fields[1] ) );
                        }
                    }
                }
                Map<String, String> galaxyParameters = tool.translateParameters(parameters);
                String format = galaxyParameters.get("data_type");
                if(format == null) format = "auto";

                String urlStr = galaxyParameters.get("URL");
                if(urlStr == null)
                    throw new InvalidParameterException("URL is missing");
                URL url = new URL(urlStr);
                String urlMethod = galaxyParameters.get("URL_method");
                if(urlMethod == null) urlMethod = "get";
                HttpURLConnection connection = (HttpURLConnection)url.openConnection(NetworkConfigurator.getProxyObject());
                if(urlMethod.equals("post"))
                {
                    connection.setRequestMethod("POST");
                    String postData = EntryStream.of( galaxyParameters ).mapKeys( TextUtil2::encodeURL ).mapValues( TextUtil2::encodeURL )
                            .join( "=" ).joining( "&" );
                    connection.setDoOutput(true);
                    try (OutputStreamWriter wr = new OutputStreamWriter( connection.getOutputStream() ))
                    {
                        wr.write( postData );
                        wr.flush();
                    }
                }
                InputStream urlStream = connection.getInputStream();
                pw.print("Importing data to "+Application.getGlobalValue("ApplicationName")+"...<br>");
                pw.flush();
                File file = TempFiles.file("galaxydata."+format, urlStream);
                if(format.equals("auto"))
                {
                    format = GalaxyFactory.detectExtension(file);
                    File newFile = TempFiles.file("galaxydata."+format);
                    newFile.delete();
                    file.renameTo(newFile);
                    file = newFile;
                }
                String name = galaxyParameters.get("name");
                if(name == null || name.equals("")) name = "Imported "+tool.getDisplayName();
                DataElementPath path = DataElementPath.create(dc, name).uniq();
                DataElementImporter importer = FormatRegistry.getImporter(format, path, file).importer;
                importer.doImport(dc, file, path.getName(), null, log);
                file.delete();
                pw.print("File is saved as "+path);
                pw.print("<div id='info'/>\n"+
                        "<a href='javascript:window.close()'>Close window</a>");
                pw.flush();
                pw.print(
                        "<div id='debug' style='display:none'/>\n"+
                        "<script>\n"+
                        "var wnd = opener;\n"+
                        "var opened = false;\n"+
                        "while(true) {\n"+
                        "  try {\n"+
                        "    wnd.top.importFinished('"+StringEscapeUtils.escapeJavaScript(path.toString())+"','"+StringEscapeUtils.escapeJavaScript(tool.getDisplayName())+"');\n"+
                        "    opened = true;\n"+
                        "    break;\n"+
                        "  }\n"+
                        "  catch(e) {document.getElementById('debug').innerHTML+=e+'<br>'}\n"+
                        "  wnd = wnd.opener;\n"+
                        "  if(!wnd) break;\n"+
                        "}\n"+
                        "if(opened) window.close();\n"+
                        "else document.getElementById('info').innerHTML += \"<a href='/bioumlweb'>Return to "+Application.getGlobalValue("ApplicationName")+"</a><br>\"\n"+
                        "\n"+
                        "</script>");
            }
            catch( Exception e )
            {
                pw.print("<b>Error</b>: "+e);
                pw.print("<br>");
                pw.print("<a href='/bioumlweb'>Return to "+Application.getGlobalValue("ApplicationName")+"</a>");
                log.log(Level.SEVERE, "Error processing galaxy query", e);
            }
        }
        catch( Exception e )
        {
            log.log(Level.SEVERE, "Unable to process query", e);
        }
        return "text/html";
    }

}
