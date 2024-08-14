package ru.biosoft.access.html;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Entities.EscapeMode;

import com.developmentontheedge.application.ApplicationUtils;

import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.DataElementExporter;
import ru.biosoft.jobcontrol.FunctionJobControl;
import ru.biosoft.util.TempFiles;
import ru.biosoft.access.security.SecurityManager;;

//install chrome-headless-render-pdf:
//#apt install npm nodejs-legacy
//#npm install -g chrome-headless-render-pdf
//install chrome:
//#wget -q -O - https://dl-ssl.google.com/linux/linux_signing_key.pub | sudo apt-key add -
//#sudo sh -c 'echo "deb [arch=amd64] http://dl.google.com/linux/chrome/deb/ stable main" >> /etc/apt/sources.list.d/google-chrome.list'
//#sudo apt-get update
//#sudo apt-get install google-chrome-stable
//probably will require newer nodejs (download binaries from official cite and add link /usr/bin/node->/path/where/new/nodejs/installed)
public class HtmlToPDFExporter implements DataElementExporter
{
    @Override
    public int accept(DataElement de)
    {
        return de instanceof ZipHtmlDataCollection ? ACCEPT_HIGH_PRIORITY + 1 : ACCEPT_UNSUPPORTED;
    }
    
    @Override
    public List<Class<? extends DataElement>> getSupportedTypes()
    {
        return Arrays.asList( ZipHtmlDataCollection.class );
    }

    @Override
    public void doExport(DataElement de, File file) throws Exception
    {
       doExport( de, file, null );
    }

    @Override
    public void doExport(DataElement de, File file, FunctionJobControl jobControl) throws Exception
    {
        File outDir = extractZip( de );
        File indexHtml = new File(outDir, "index.html");
        
        preprocessHtml(indexHtml);
        
        ProcessBuilder pb = new ProcessBuilder( "chrome-headless-render-pdf",
                "--include-background",
                "--paper-width=8.27",
                "--paper-height=11.69", 
                "--url=" + indexHtml.getAbsoluteFile().toURI().toString(),
                "--pdf=" + file.getAbsolutePath());

        pb.redirectError( new File("/dev/null") );
        pb.redirectOutput( new File("/dev/null") );
        Process proc = pb.start();
        int exitCode = proc.waitFor();
        if(exitCode != 0)
            throw new Exception("Exit code="+exitCode);
    }

    protected void preprocessHtml(File indexHtml) throws IOException
    {
        Document doc = Jsoup.parse( indexHtml, StandardCharsets.US_ASCII.name() );
        doc.outputSettings().escapeMode( EscapeMode.extended );
        doc.outputSettings().charset( StandardCharsets.US_ASCII );

        preprocessHtmlDoc( doc );
        
        ApplicationUtils.writeString( indexHtml, doc.toString() );
    }
    
    protected void preprocessHtmlDoc(Document doc)
    {
        String serverName = SecurityManager.getSecurityProvider().getServerName();
        if(!serverName.startsWith( "http://" ) && !serverName.startsWith( "https://" ))
            serverName = "http://" + serverName;
        final String prefix = serverName;
        doc.select( "a" ).forEach( a -> {
            String href = a.attr( "href" );
            if(href.startsWith( "/" ))
                a.attr( "href", prefix + href );
        });
        
        if(isGenomeEnhancerReport(doc) && isOldGenomeEnhancerReport(doc))
            fixOldGenomeEnhancer(doc);
    }

    private boolean isGenomeEnhancerReport(Document doc)
    {
        return !doc.select( "body.narrow_body" ).isEmpty();
    }
    
    private boolean isOldGenomeEnhancerReport(Document doc)
    {
        return !doc.select( "head style" ).get( 0 ).text().contains( "@media print" );
    }

    private void fixOldGenomeEnhancer(Document doc)
    {
        String media =
           "@media print {\n" + 
           "  @page { size: A4; }\n" + 
           "  img {max-width: 100%;}\n" + 
           "  .narrow_body{max-width:100%;}\n" + 
           "}\n" + 
           "\n" + 
           "@media screen {\n" + 
           "  .narrow_body{max-width:562pt;padding:72pt 72pt 72pt 72pt}\n" + 
           "}\n";
        
        Element style = doc.select( "head style" ).get( 0 );
        String newStyleStr = style.html();
        newStyleStr = newStyleStr.replace( ".narrow_body{background-color:#ffffff;max-width:562pt;padding:72pt 72pt 72pt 72pt}", ".narrow_body{background-color:#ffffff;}" );
        newStyleStr = media + newStyleStr;
        style.html( newStyleStr );
    }

    public File extractZip(DataElement de) throws IOException, FileNotFoundException
    {
        File fileZip = ((ZipHtmlDataCollection)de).getFile();
        File destDir = TempFiles.dir( "html" );
        byte[] buffer = new byte[1024];
        ZipInputStream zis = new ZipInputStream(new FileInputStream(fileZip));
        ZipEntry zipEntry = zis.getNextEntry();
        while (zipEntry != null) {
            File newFile = new File(destDir, zipEntry.getName());
            newFile.getParentFile().mkdirs();
            FileOutputStream fos = new FileOutputStream(newFile);
            int len;
            while ((len = zis.read(buffer)) > 0) {
                fos.write(buffer, 0, len);
            }
            fos.close();
            zipEntry = zis.getNextEntry();
        }
        zis.closeEntry();
        zis.close();
        return destDir;
    }

    @Override
    public boolean init(Properties properties)
    {
        return isProgExists("chrome-headless-render-pdf");
    }

    private boolean isProgExists(String progName)
    {
        for(String path : System.getenv( "PATH" ).split( ":" ))
        {
            File file = new File(path, progName);
            if(file.exists() && file.canExecute())
                return true;
        }
        return false;
    }

}
