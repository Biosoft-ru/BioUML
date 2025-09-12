package ru.biosoft.plugins.jsreport;

import java.awt.Rectangle;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.text.StringEscapeUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.mozilla.javascript.NativeArray;
import org.mozilla.javascript.NativeJavaObject;

import ru.biosoft.bsa.exporter.ZHTMLExportTransformer;
import ru.biosoft.bsa.project.Project;
import ru.biosoft.bsa.view.colorscheme.AbstractSiteColorScheme;
import ru.biosoft.graphics.View;
import ru.biosoft.table.StringSet;

public class TableReport
{
    private static final Logger log = Logger.getLogger( TableReport.class.getName() );

    protected static final DecimalFormat decFormat, expFormat;
    static
    {
        DecimalFormatSymbols decimalFormatSymbols = new DecimalFormatSymbols(Locale.US);
        decimalFormatSymbols.setNaN( "" );
        decFormat = new DecimalFormat("#.##");
        decFormat.setDecimalFormatSymbols(decimalFormatSymbols);
        expFormat = new DecimalFormat("#.##E0");
        expFormat.setDecimalFormatSymbols(decimalFormatSymbols);
    }

    private File imagesPath;
    private PrintWriter pw;

    public TableReport(File imagesPath, PrintWriter pw)
    {
        this.imagesPath = imagesPath;
        this.pw = pw;
    }

    /*className: 
        "data" - regular biouml table, 
        "parameters" - show full text in lines, add ":" symbol after value in first column
        "data extendable" - add clickable "more" link to expand column value to full text
     */
    public void addTable(NativeArray colHeaders, NativeArray array, String className, boolean escape) throws Exception
    {
        pw.println("<table class=\""+className+"\">");
        if( colHeaders != null )
        {
            pw.println("\t<tr>");
            for( int col = 0; col < colHeaders.getLength(); col++ )
            {
                String value = stringValue( colHeaders.get( col, null ), false );
                pw.println("\t\t<th>" + (escape?StringEscapeUtils.escapeHtml4(value):value));
            }
        }
        boolean cutLongString = !className.startsWith( "parameters" );
        boolean isExtendable = className.contains( "extendable" );
        for( int id = 0; id < array.getLength(); id++ )
        {
            Object object = array.get(id, null);
            if( ! ( object instanceof NativeArray ) )
                continue;
            NativeArray row = (NativeArray)object;
            if( row.getLength() == 0 )
                continue;
            pw.println("\t<tr class=\""+( id % 2 == 0 ? "even-row" : "odd-row" )+"\">");
            String value = stringValue( row.get( 0, null ), cutLongString );
            String reportValue = escape?StringEscapeUtils.escapeHtml4(value):value;
            if(isExtendable && cutLongString)
            {
                String fullValue = stringValue( row.get( 0, null ), false );
                if( !fullValue.equals( value ) )
                    reportValue = "<span>" + value + "<span class='clickable_cell' onclick='toggleTableCell(this)'>(more)</span></span>" +
                        "<span style='display:none;'>"+fullValue+ "<span class='clickable_cell' onclick='toggleTableCell(this)'>(less)</span></span>";
            }
                        
            pw.println("\t\t<td class=\"header\">"
                    + reportValue + (className.startsWith("data")?"":":"));
            for( int col = 1; col < row.getLength(); col++ )
            {
                value = stringValue( row.get( col, null ), cutLongString );
                reportValue = escape ? StringEscapeUtils.escapeHtml4( value ) : value;
                if( isExtendable && cutLongString )
                {
                    String fullValue = stringValue( row.get( col, null ), false );
                    if( !fullValue.equals( value ) )
                        reportValue = "<span>" + value + "<span class='clickable_cell' onclick='toggleTableCell(this)'>(more)</span></span>"
                            + "<span style='display:none;'>" + fullValue
                                + "<span class='clickable_cell' onclick='toggleTableCell(this)'> (less)</span></span>";
                }
                pw.println( "\t\t<td class=\"cell\">" + reportValue );
            }
        }
        pw.println("</table>");
        if( isExtendable )
        {
            pw.println( "<script type='text/javascript'>" );
            pw.println( "function toggleTableCell(this_el) {" );
            pw.println( "var parentcell = this_el.parentElement.parentElement;"
                    + "for (let i = 0; i < parentcell.children.length; i++) {"
                    + "        let el = parentcell.children[i];" 
                    + "    if ( el.style.display == '' || el.style.display == 'block' ) { \n"
                    + "        el.style.display = 'none';\n" 
                    + "    }else{\n" 
                    + "        el.style.display = 'block';\n" 
                    + "    }" 
                    + "}" );
            pw.println( "}" );
            pw.println( "</script>" );
            pw.println( "<style>" );
            pw.println( "span.clickable_cell {cursor: pointer;color: blue;}" );
            pw.println( "span.clickable_cell:hover {cursor: pointer;color: blue;text-decoration: underline;}" );
            pw.println( "</style>" );
        }
    }

    private String stringValue(Object object)
    {
        return stringValue( object, true );
    }
    private String stringValue(Object object, boolean cutLongString)
    {
        if( object instanceof NativeJavaObject )
            object = ( (NativeJavaObject)object ).unwrap();
        if( object instanceof Float || object instanceof Double )
        {
            double num = ( (Number)object ).doubleValue();
            object = ( num == 0 ? "0" : ( Math.abs(num) >= 1e6 || Math.abs(num) < 0.1 ) ? expFormat.format(num) : decFormat
                    .format(num) );
        }
        else if( object instanceof StringSet )
        {
            StringSet stringSet = (StringSet)object;
            String suffix = ( cutLongString && stringSet.size() > 7 ) ? "..." : "";
            object = cutLongString ? stringSet.stream().limit( 7 ).joining( ", " ) + suffix : stringSet.stream().joining( ", " );
        }
        else if( object instanceof String )
        {
            String str = (String)object;
            Document taggedStr = Jsoup.parse( str );
            String innerStr = taggedStr.text();
            if( cutLongString && innerStr.length() > 100 )
            {
                innerStr = innerStr.substring( 0, 100 ) + "...";
                Elements children = taggedStr.body().children();
                if( children.size() > 0 ) //is element with tags
                {
                    Element el = children.get( 0 );
                    el.text( innerStr );
                    object = el.toString();
                }
                else
                    object = innerStr;
            }
        }
        else if( object instanceof View )
        {
            try
            {
                View view = (View)object;
                Rectangle r = view.getBounds();
                if( r.height == 0 || r.width == 0 )
                    object = "";
                else
                    object = ZHTMLExportTransformer.createViewString( view, imagesPath );
            }
            catch( IOException e )
            {
                log.log(Level.WARNING, "Error during creating image for table", e );
            }
        }
        else if( object instanceof Project )
        {
            try
            {
                Project project = (Project)object;
                object = ZHTMLExportTransformer.createProjectString( project, imagesPath, cachedSiteColorMap, true, true );
            }
            catch( IOException e )
            {
                log.log(Level.WARNING, "Error during creating image for table", e );
            }
        }
        return String.valueOf(object);
    }

    private final Map<String, AbstractSiteColorScheme> cachedSiteColorMap = new HashMap<>();

    public static class Hyperlink
    {
        private String text;
        private String url;
        private String target = "_blank";
        private String onclick = null;

        public Hyperlink(String text, String url)
        {
            this.text = text;
            this.url = url;
        }

        public Hyperlink(String text, String url, String target, String onclick)
        {
            this( text, url );
            this.target = target;
            this.onclick = onclick;
        }

        @Override
        public String toString()
        {
            StringBuilder sb = new StringBuilder();
            sb.append( "<a href=\"" ).append( StringEscapeUtils.escapeHtml4( url ) ).append( "\" target=\"" ).append( target )
                    .append( "\"" );
            if( onclick != null )
                sb.append( " onclick=\"" ).append( onclick ).append( "\"" );
            sb.append( ">" ).append( StringEscapeUtils.escapeHtml4( text ) ).append( "</a>" );
            return sb.toString();
        }

    }
}
