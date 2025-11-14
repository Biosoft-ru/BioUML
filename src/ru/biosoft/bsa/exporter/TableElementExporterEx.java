package ru.biosoft.bsa.exporter;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.Properties;
import java.util.zip.ZipOutputStream;

import javax.annotation.Nonnull;

import com.developmentontheedge.application.ApplicationUtils;

import ru.biosoft.access.DataElementExporterRegistry;
import ru.biosoft.access.html.HtmlToPDFExporter;
import ru.biosoft.jobcontrol.FunctionJobControl;
import ru.biosoft.jobcontrol.JobControl;
import ru.biosoft.table.ColumnModel;
import ru.biosoft.table.RowDataElement;
import ru.biosoft.table.TableDataCollection;
import ru.biosoft.table.columnbeans.Column;
import ru.biosoft.table.export.TableElementExporter;
import ru.biosoft.util.TempFiles;
import ru.biosoft.util.archive.ArchiveUtil;

public class TableElementExporterEx extends TableElementExporter
{
    @Override
    public boolean init(Properties properties)
    {
        if( properties.getProperty( DataElementExporterRegistry.SUFFIX ).equals( "zhtml" ) )
        {
            transformer = new ZHTMLExportTransformer();
            return true;
        }
        else if( properties.getProperty( DataElementExporterRegistry.SUFFIX ).equals( "pdf" ) )
        {
            if( HtmlToPDFExporter.isPDFExportAvailable() )
            {
                transformer = new PDFExportTransformer();
                return true;
            }
            else
                return false;
        }
        return super.init( properties );
    }

    @Override
    public void doExport(@Nonnull ru.biosoft.access.core.DataElement de, @Nonnull File file, FunctionJobControl jobControl) throws Exception
    {
        if( transformer instanceof ZHTMLExportTransformer )
        {
            TableDataCollection tdc = (TableDataCollection)de;
            prepareProperties( tdc );

            ColumnModel model = tdc.getColumnModel();
            if( !properties.getSortOrder().equals( SortOrderSelector.NOT_SORTED ) )
            {
                String columnName = properties.getSortColumn();
                if( columnName != null && !columnName.isEmpty() )
                {
                    int columnNumber = model.getColumnIndex( columnName );
                    tdc.sortTable( columnNumber, properties.getSortOrder().equals( SortOrderSelector.ASCENDING ) );
                }
            }

            File tempDir = TempFiles.dir( "export" );
            try( OutputStream os = new FileOutputStream( new File( tempDir, "index.html" ) ) )
            {
                ( (ZHTMLExportTransformer)transformer ).setImagesPath( tempDir );
                transformer.setOutputStream( os );
                transformer.setDataElement( de );
                transformer.writeHeader( de.getName() );
                Column[] columns = properties.getColumns();
                if( properties.isIncludeHeaders() )
                {
                    transformer.writeColumnSectionStart();
                    if( properties.isIncludeIds() )
                    {
                        transformer.writeColumnTitle( "ID" );
                        transformer.writeColumnTitleSeparator();
                    }
                    boolean first = true;
                    for( Column column : columns )
                    {
                        if( first )
                            first = false;
                        else
                            transformer.writeColumnTitleSeparator();
                        transformer.writeColumnTitle( column.getNewName() );
                    }
                    transformer.writeColumnSectionEnd();
                }
                transformer.writeDataSectionStart();
                if( properties.getLastRow() > tdc.getSize() )
                    properties.setLastRow( tdc.getSize() );
                int rowNumber = 0;
                int size = properties.getLastRow();
                for( RowDataElement rde : tdc )
                {
                    rowNumber++;
                    if( rowNumber < properties.getFirstRow() )
                        continue;
                    if( rowNumber > properties.getLastRow() )
                        break;
                    if( properties.isIncludeIds() )
                    {
                        transformer.writeData( rde.getName() );
                        transformer.writeDataSeparator();
                    }
                    boolean first = true;
                    for( Column column : columns )
                    {
                        Object value = rde.getValues()[model.getColumnIndex( column.getName() )];
                        if( first )
                            first = false;
                        else
                            transformer.writeDataSeparator();

                        transformer.writeData( value );
                    }
                    transformer.writeLineSeparator();
                    if( jobControl != null && rowNumber % 1000 == 0 )
                    {
                        jobControl.setPreparedness( (int)Math.floor( rowNumber * 100. / size ) );
                        if( jobControl.getStatus() == JobControl.TERMINATED_BY_REQUEST )
                        {
                            ApplicationUtils.removeDir( tempDir );
                            file.delete();
                            return;
                        }
                    }
                }
                transformer.writeFooter();
            }

            if( transformer instanceof PDFExportTransformer )
            {
                new HtmlToPDFExporter().exportDirToPDF( tempDir, file );
                //exportToPDF( file, tempDir );
            }
            else
            {
                try (ZipOutputStream out = new ZipOutputStream( new FileOutputStream( file ) ))
                {
                    ArchiveUtil.addDirectoryToZip( out, tempDir, "" );
                }
            }

        }
        else
            super.doExport( de, file, jobControl );
    }

    //    protected void exportToPDF(File file, File tempDir) throws IOException, InterruptedException, Exception
    //    {
    //        File indexHtml = new File(tempDir, "index.html");
    //        
    //        Document doc = Jsoup.parse( indexHtml, StandardCharsets.US_ASCII.name() );
    //        doc.outputSettings().escapeMode( EscapeMode.extended );
    //        doc.outputSettings().charset( StandardCharsets.US_ASCII );
    //
    //        String serverName = SecurityManager.getSecurityProvider().getServerName();
    //        if(!serverName.startsWith( "http://" ) && !serverName.startsWith( "https://" ))
    //            serverName = "http://" + serverName;
    //        final String prefix = serverName;
    //        doc.select( "a" ).forEach( a -> {
    //            String href = a.attr( "href" );
    //            if(href.startsWith( "/" ))
    //                a.attr( "href", prefix + href );
    //        });
    //        
    //        ApplicationUtils.writeString( indexHtml, doc.toString() );
    //        
    //        ProcessBuilder pb = new ProcessBuilder( "chrome-headless-render-pdf",
    //                "--include-background",
    //                "--paper-width=8.27",
    //                "--paper-height=11.69", 
    //                "--url=" + indexHtml.getAbsoluteFile().toURI().toString(),
    //                "--pdf=" + file.getAbsolutePath());
    //
    //        pb.redirectError( new File("/dev/null") );
    //        pb.redirectOutput( new File("/dev/null") );
    //        Process proc = pb.start();
    //        int exitCode = proc.waitFor();
    //        if(exitCode != 0)
    //            throw new Exception("Exit code="+exitCode);
    //    }

}
