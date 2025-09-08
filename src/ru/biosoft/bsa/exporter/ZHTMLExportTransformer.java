package ru.biosoft.bsa.exporter;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.TreeMap;

import javax.imageio.ImageIO;

import org.apache.commons.lang.StringEscapeUtils;
import com.developmentontheedge.application.ApplicationUtils;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.bsa.Sequence;
import ru.biosoft.bsa.Site;
import ru.biosoft.bsa.Track;
import ru.biosoft.bsa.project.Project;
import ru.biosoft.bsa.project.Region;
import ru.biosoft.bsa.project.TrackInfo;
import ru.biosoft.bsa.view.MapViewOptions;
import ru.biosoft.bsa.view.SequenceView;
import ru.biosoft.bsa.view.SiteViewOptions;
import ru.biosoft.bsa.view.TaggedCompositeView;
import ru.biosoft.bsa.view.TrackBackgroundView;
import ru.biosoft.bsa.view.TrackViewBuilder;
import ru.biosoft.bsa.view.ViewFactory;
import ru.biosoft.bsa.view.ViewOptions;
import ru.biosoft.bsa.view.colorscheme.AbstractSiteColorScheme;
import ru.biosoft.bsa.view.sitelayout.SiteLayoutAlgorithm;
import ru.biosoft.graphics.CompositeView;
import ru.biosoft.graphics.ImageGenerator;
import ru.biosoft.graphics.View;
import ru.biosoft.table.export.HTMLExportTransformer;

public class ZHTMLExportTransformer extends HTMLExportTransformer
{
    private File imagesPath;
    public void setImagesPath(File path)
    {
        this.imagesPath = path;
    }

    @Override
    public void writeData(Object data)
    {
        String dataStr;
        if( data instanceof Project )
        {
            try
            {
                dataStr = createProjectString( (Project)data, imagesPath, colorSchemeMap, false, true );
            }
            catch( Throwable t )
            {
                dataStr = "";
            }
        }
        else if( data instanceof View )
        {
            try
            {
                dataStr = createViewString( (View)data, imagesPath );
            }
            catch( IOException e )
            {
                dataStr = "";
            }
        }
        else
        {
            dataStr = data == null ? "" : data.toString().replaceAll( "[\t\n]", " " );
        }
        super.writeData( dataStr );
    }

    @Override
    public void writeFooter()
    {
        pw.print( "</tr>\n</tbody>\n</table>\n</body>\n</html>\n" );
        try
        {
            if( !colorSchemeMap.isEmpty() )
            {
                pw.print( "\n<b>Site color legends:</b>\n" );
                int count = 1;
                for( Map.Entry<String, AbstractSiteColorScheme> entry : colorSchemeMap.entrySet() )
                {
                    String title = entry.getKey();
                    pw.print( "<br>\n" + ( count++ ) + ") <i>" + title + " legend</i>:\n<br>\n" );
                    String fileName = "colorLegend.png";
                    CompositeView view = entry.getValue().getLegend( ApplicationUtils.getGraphics() );
                    fileName = saveImageFile( fileName, imagesPath, ImageGenerator.generateImage( view, 1, true ) );
                    StringBuilder sb = new StringBuilder();
                    sb.append( "<img src=\"" ).append( fileName ).append( "\" alt=\"" ).append( StringEscapeUtils.escapeHtml( title ) )
                            .append( "\" title=\"" ).append( StringEscapeUtils.escapeHtml( title ) )
                            .append( "\" style=\"max-width:250px;\" width=\"auto\" height=\"auto\"/>" );
                    pw.print( sb.toString() );
                    pw.print( "\n" );
                }
            }
        }
        catch( IOException e )
        {
        }
        pw.flush();
    }

    private final Map<String, AbstractSiteColorScheme> colorSchemeMap = new TreeMap<>();

    public static String createProjectString(Project project, File imagesPath) throws IOException
    {
        return createProjectString( project, imagesPath, null, false, false );
    }

    public static String createProjectString(Project project, File imagesPath, Map<String, AbstractSiteColorScheme> siteColorSchemeMap,
            boolean fullTrackMode, boolean addRuler) throws IOException
    {
        String title = project.getName();
        String fileName = title + ".png";
        StringBuilder sb = new StringBuilder();
        View view = createProjectView( project, siteColorSchemeMap, fullTrackMode, addRuler );
        BufferedImage image = createProjectImage( view, 1 );
        fileName = saveImageFile( fileName, imagesPath, image );
        sb.append( "<img src=\"" ).append( fileName ).append( "\" alt=\"" ).append( StringEscapeUtils.escapeHtml( title ) )
                .append( "\" title=\"" ).append( StringEscapeUtils.escapeHtml( title ) )
                .append( "\" style=\"max-width:1200px;\" width=\"auto\" height=\"auto\"/>" );
        String description = project.getDescription();
        if( description != null && !description.isEmpty() )
            sb.append( "<div class=\"image-description\">" ).append( description ).append( "</div>" );
        return sb.toString();
    }

    private static BufferedImage createProjectImage(View view, double scale)
    {
        Rectangle r = view.getBounds();
        int xAdd = 2 * Math.abs( r.x ) + ( r.x != 0 ? 4 : 8 );
        int yAdd = 2 * Math.abs( r.y ) + ( r.y != 0 ? 4 : 8 );
        int width = (int)Math.ceil( ( r.width + xAdd ) * scale );
        int height = (int)Math.ceil( ( r.height + yAdd ) * scale );

        BufferedImage image = new BufferedImage( width, height, BufferedImage.TYPE_INT_RGB );
        Graphics2D graphics = image.createGraphics();
        graphics.setRenderingHint( RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON );
        graphics.setColor( Color.white );

        Rectangle rectangle = new Rectangle( 0, 0, width, height );
        graphics.fill( rectangle );
        graphics.setClip( rectangle );

        AffineTransform at = new AffineTransform();
        at.scale( scale, scale );
        graphics.setTransform( at );

        view.paint( graphics );
        return image;
    }

    private static View createProjectView(Project project, Map<String, AbstractSiteColorScheme> siteColorSchemeMap, boolean fullTrackMode,
            boolean addRuler)
    {
        Region region = project.getRegions()[0];
        Sequence seq = region.getSequence();
        int from = region.getFrom();
        int to = region.getTo();
        int sequenceLength = to - from + 1;

        ViewOptions viewOptions = project.getViewOptions();
        viewOptions.semanticZoomSet( 600. / sequenceLength );
        MapViewOptions mapViewOptions = viewOptions.getRegionViewOptions();

        SequenceView sequenceView = ViewFactory.createSequenceView( seq, mapViewOptions.getSequenceViewOptions(), from, to,
                ApplicationUtils.getGraphics() );

        CompositeView result = new CompositeView();
        if( addRuler )
            result.add( sequenceView, CompositeView.Y_BT );
        for( TrackInfo trackInfo : project.getTracks() )
        {
            Track track = trackInfo.getTrack();
            DataCollection<Site> sites = track.getSites( region.getSequenceName(), from, to );
            TrackViewBuilder viewBuilder = track.getViewBuilder();
            SiteViewOptions siteViewOptions = viewBuilder.createViewOptions();
            setSiteColorScheme( siteColorSchemeMap, trackInfo.getTitle(), siteViewOptions );
            siteViewOptions.setTrackDisplayMode( fullTrackMode ? SiteViewOptions.TRACK_MODE_FULL : SiteViewOptions.TRACK_MODE_COMPACT );
            CompositeView trackView = viewBuilder.createTrackView( sequenceView, sites, siteViewOptions, from, to,
                    SiteLayoutAlgorithm.BOTTOM, ApplicationUtils.getGraphics(), null );
            int width = sequenceView.getBounds().width;
            trackView.insert( new TrackBackgroundView( trackView.getBounds(), width ), 0 );
            TaggedCompositeView view = new TaggedCompositeView();
            view.add( trackView );
            view.setTags( siteViewOptions.getViewTagger() );

            result.add( view, CompositeView.Y_BT );
        }

        return result;
    }

    private static void setSiteColorScheme(Map<String, AbstractSiteColorScheme> siteColorSchemeMap, String title,
            SiteViewOptions siteViewOptions)
    {
        if( siteColorSchemeMap == null )
            return;
        AbstractSiteColorScheme siteColorScheme = siteColorSchemeMap.computeIfAbsent( title, s -> siteViewOptions.getColorScheme() );
        siteViewOptions.setColorScheme( siteColorScheme );
    }

    public static String createViewString(View view, File imagesPath) throws IOException
    {
        String title = "innerImage";
        String fileName = "innerImage.png";
        fileName = saveImageFile( fileName, imagesPath, ImageGenerator.generateImage( view, 1, true ) );
        StringBuilder sb = new StringBuilder();
        sb.append( "<img src=\"" ).append( fileName ).append( "\" alt=\"" ).append( StringEscapeUtils.escapeHtml( title ) )
                .append( "\" title=\"" ).append( StringEscapeUtils.escapeHtml( title ) )
                .append( "\" style=\"max-width:200px;max-height:200px;\" width=\"auto\" height=\"auto\"/>" );
        return sb.toString();
    }

    private static String saveImageFile(String fileName, File imagesPath, BufferedImage image) throws IOException
    {
        File file = new File( imagesPath, fileName );
        int i = 1;
        while( file.exists() )
        {
            String[] fields = fileName.split( "[.]" );
            fields[0] += i++;
            file = new File( imagesPath, String.join( ".", fields ) );
        }
        file.getParentFile().mkdirs();
        ImageIO.write( image, "PNG", file );
        return file.getName();
    }
}
