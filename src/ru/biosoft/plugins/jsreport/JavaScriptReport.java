package ru.biosoft.plugins.jsreport;

import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.zip.ZipOutputStream;

import javax.imageio.ImageIO;

import org.apache.commons.lang.StringEscapeUtils;
import java.util.logging.Logger;
import org.json.JSONException;
import org.mozilla.javascript.NativeArray;

import com.developmentontheedge.application.ApplicationUtils;

import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.FileDataElement;
import ru.biosoft.access.ImageDataElement;
import ru.biosoft.access.core.TextDataElement;
import ru.biosoft.access.ZipHTMLImporter;
import ru.biosoft.access.html.ZipHtmlDataCollection;
import ru.biosoft.access.security.CodePrivilege;
import ru.biosoft.access.security.CodePrivilegeType;
import ru.biosoft.graphics.chart.Chart;
import ru.biosoft.plugins.javascript.JavaScriptHostObjectBase;
import ru.biosoft.util.RhinoUtils;
import ru.biosoft.util.TempFiles;
import ru.biosoft.util.archive.ArchiveUtil;
import ru.biosoft.util.j2html.tags.DomContent;

/**
 * API to create reports from JS
 * @author lan
 */
public class JavaScriptReport extends JavaScriptHostObjectBase
{
    private static final Logger log = Logger.getLogger(JavaScriptReport.class.getName());
    
    public Report create(String title) throws IOException
    {
        return new Report(title);
    }

    @CodePrivilege(CodePrivilegeType.REPOSITORY)
    public static class Report
    {
        private final File path;
        private final PrintWriter pw;
        private boolean valid;

        public Report(String title) throws IOException
        {
            path = TempFiles.dir("report");
            ApplicationUtils.copyStream(new FileOutputStream(new File(path, "style.css")),
                    Report.class.getResourceAsStream("resources/reportstyle.css"));
            pw = new PrintWriter(new File(path, "index.html"), "UTF-8");
            valid = true;
            pw.println("<!DOCTYPE html>");
            pw.println("<html>");
            pw.println("<head>");
            pw.println("<meta charset=\"UTF-8\">");
            pw.println("<link rel=\"stylesheet\" type=\"text/css\" href=\"style.css\"/>");
            wrapHTML("title", title);
            pw.println("</head>");
            pw.println("<body>");
        }

        public void addHeader(String header)
        {
            wrapHTML("h1", header);
        }

        public void addSubHeader(String subHeader)
        {
            wrapHTML("h2", subHeader);
        }

        public void addSubSubHeader(String subHeader)
        {
            wrapHTML("h3", subHeader);
        }

        public void addParagraph(String paragraph)
        {
            wrapHTML("p", paragraph);
        }
        
        public void addHTML(DomContent content)
        {
            addHTML(content.render());
        }

        public void addHTML(String html)
        {
            if( !valid )
                return;
            pw.println(html);
        }

        public void addTable(NativeArray array) throws Exception
        {
            addTable(null, array);
        }
        
        public void addTable(NativeArray colHeaders, NativeArray array) throws Exception
        {
            addTable(colHeaders, array, "parameters");
        }
        
        public void addTable(NativeArray colHeaders, NativeArray array, String className) throws Exception
        {
            addTable(colHeaders, array, className, true);
        }

        public void addTable(NativeArray colHeaders, NativeArray array, String className, boolean escape) throws Exception
        {
            if( !valid )
                return;
            TableReport tableReport = new TableReport( path, pw );
            tableReport.addTable( colHeaders, array, className, escape );
        }
        
        public void addImage(BufferedImage image, String fileName, String title) throws IOException
        {
            addImage(image, fileName, title, "", null);
        }

        public void addImage(BufferedImage image, String fileName, String title, String description) throws IOException
        {
            addImage(image, fileName, title, description, null);
        }

        public void addImage(BufferedImage image, String fileName, String title, String description, String floatPos) throws IOException
        {
            if( !valid )
                return;
            while( fileName.startsWith("/") )
                fileName = fileName.substring(1);
            if( !fileName.matches("[\\w/\\ \\-]+") )
                throw new IllegalArgumentException("Invalid fileName");
            fileName = fileName + ".png";
            saveImageFile( fileName, image );
            pw.println( "<img src=\"" + fileName + "\" alt=\"" + StringEscapeUtils.escapeHtml( title ) + "\" title=\""
                    + StringEscapeUtils.escapeHtml( title ) + "\"" + ( floatPos != null ? " align=\"" + floatPos + "\"" : "" ) + ">" );
            if( description != null && !description.isEmpty() )
                pw.println("<div class=\"image-description\">" + description + "</div>");
        }

        public void addChart(NativeArray chart, int width, int height, String fileName, String title, String description)
                throws JSONException, IOException
        {
            addImage(new Chart(RhinoUtils.toJSONArray(chart)).getImage(width, height), fileName, title, description);
        }

        public void addReport(String reportPath, String dirName) throws Exception
        {
            if( !valid )
                return;
            ZipHtmlDataCollection zhtml = DataElementPath.create(reportPath).getDataElement(ZipHtmlDataCollection.class);
            copyReportFiles(new File(path, dirName), zhtml);
            boolean copyMode = false;
            BufferedReader br = new BufferedReader(new StringReader(zhtml.get("index.html").cast( TextDataElement.class ).getContent()));
            while( br.ready() )
            {
                String row = br.readLine();
                if( row == null || row.startsWith("</body>") )
                    break;
                if( row.startsWith("<body>") )
                    copyMode = true;
                else if( copyMode )
                {
                    row = row.replace("<a href=\"", "<a href=\"" + dirName + "/");
                    row = row.replace("<img src=\"", "<img src=\"" + dirName + "/");
                    pw.println(row);
                }
            }
        }

        public void store(String pathStr) throws Exception
        {
            DataElementPath dePath = DataElementPath.create(pathStr);
            pw.println("</body>");
            pw.println("</html>");
            pw.close();
            File zip = TempFiles.file("report.zhtml");
            try(ZipOutputStream out = new ZipOutputStream(new FileOutputStream(zip)))
            {
                ArchiveUtil.addDirectoryToZip( out, this.path, "" );
            }
            ZipHTMLImporter fileImporter = new ZipHTMLImporter();
            fileImporter.getProperties( dePath.optParentCollection(), zip, dePath.getName() ).setPreserveExtension( false );
            fileImporter.doImport(dePath.getParentCollection(), zip, dePath.getName(), null, log);
            zip.delete();
            destroy();
        }

        public void destroy()
        {
            valid = false;
            pw.close();
            ApplicationUtils.removeDir(path);
        }

        protected void wrapHTML(String tag, String html)
        {
            if( !valid )
                return;
            pw.println("<" + tag + ">" + html + "</" + tag + ">");
        }

        /**
         * @param file
         * @param zhtml
         * @throws Exception
         */
        private void copyReportFiles(File file, ZipHtmlDataCollection zhtml) throws Exception
        {
            file.mkdirs();
            for( ru.biosoft.access.core.DataElement element : zhtml )
            {
                File outputFile = new File(file, element.getName());
                if( element instanceof ZipHtmlDataCollection )
                {
                    copyReportFiles(outputFile, (ZipHtmlDataCollection)element);
                }
                else if( element instanceof TextDataElement )
                {
                    ApplicationUtils.copyStream(new FileOutputStream(outputFile), new ByteArrayInputStream( ( (TextDataElement)element )
                            .getContent().getBytes(StandardCharsets.UTF_8)));
                }
                else if( element instanceof ImageDataElement )
                {
                    ImageIO.write( ( (ImageDataElement)element ).getImage(null), "PNG", outputFile);
                }
                else if( element instanceof FileDataElement )
                {
                    ApplicationUtils.copyFile(outputFile, ( (FileDataElement)element ).getFile());
                }
            }
        }

        private void saveImageFile(String fileName, BufferedImage image) throws IOException
        {
            File file = new File( path, fileName );
            file.getParentFile().mkdirs();
            ImageIO.write( image, "PNG", file );
        }

        @Override
        protected void finalize()
        {
            destroy();
        }
    }
}
