package ru.biosoft.bsa._test;

import java.awt.Color;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.imageio.ImageIO;

import com.developmentontheedge.application.ApplicationUtils;

import ru.biosoft.access._test.AbstractBioUMLTest;
import ru.biosoft.access.core.CollectionFactory;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.core.VectorDataCollection;
import ru.biosoft.bsa.AnnotatedSequence;
import ru.biosoft.bsa.Interval;
import ru.biosoft.bsa.LinearSequence;
import ru.biosoft.bsa.MapAsVector;
import ru.biosoft.bsa.Nucleotide5LetterAlphabet;
import ru.biosoft.bsa.Sequence;
import ru.biosoft.bsa.Site;
import ru.biosoft.bsa.SiteImpl;
import ru.biosoft.bsa.StrandType;
import ru.biosoft.bsa.Track;
import ru.biosoft.bsa.TrackImpl;
import ru.biosoft.bsa.track.combined.CombinedTrack;
import ru.biosoft.bsa.view.MapViewOptions;
import ru.biosoft.bsa.view.SequenceView;
import ru.biosoft.bsa.view.TrackViewBuilder;
import ru.biosoft.bsa.view.ViewFactory;
import ru.biosoft.bsa.view.sitelayout.SiteLayoutAlgorithm;
import ru.biosoft.graphics.BoxView;
import ru.biosoft.graphics.Brush;
import ru.biosoft.graphics.CompositeView;
import ru.biosoft.graphics.Pen;
import ru.biosoft.graphics.View;
import ru.biosoft.util.ImageGenerator;

public class CombinedTrackTest extends AbstractTrackTest
{
    private CombinedTrack combinedTrack;
    private Track innerTrack;

    private final String outDirectory = AbstractBioUMLTest.getTestDir() + "/results/";
    private final String imageDirectory = outDirectory + "images/";

    public void testCreateView() throws IOException
    {
        TrackViewBuilder viewBuilder = combinedTrack.getViewBuilder();

        AnnotatedSequence seq = (AnnotatedSequence)CollectionFactory.getDataElement( "test/1" );
        Interval interval = new Interval( 0, 100 );
        DataCollection<Site> sites = combinedTrack.getSites( DataElementPath.create( seq ).toString(), interval.getFrom(),
                interval.getTo() );
        SequenceView sequenceView = ViewFactory.createSequenceView( seq.getSequence(), new MapViewOptions().getSequenceViewOptions(),
                interval.getFrom(), interval.getTo(), ApplicationUtils.getGraphics() );
        assertNotNull( "Sequence view created", sequenceView );
        CompositeView cv = viewBuilder.createTrackView( sequenceView, sites, viewBuilder.createViewOptions(), interval.getFrom(),
                interval.getTo(), SiteLayoutAlgorithm.BOTTOM, ApplicationUtils.getGraphics(), null );

        assertNotNull( "View created", cv );
        Rectangle rect = cv.getBounds();
        rect.grow( 20, 10 );
        rect.setLocation( 0, 40 );
        Color col = new Color( 255, 255, 255, 80 );
        Brush brush = new Brush( col );
        View nv = new BoxView( new Pen( 1, col ), brush, rect );
        CompositeView ccv = new CompositeView();
        ccv.add( nv );
        sequenceView.setLocation( 20, 0 );
        ccv.add( sequenceView );
        cv.setLocation( 20, 50 );
        ccv.add( cv );
        saveViewToFile( ccv, new File( imageDirectory + combinedTrack.getName() ) );

    }

    public static void saveViewToFile(View view, File file) throws IOException
    {
        BufferedImage image = ImageGenerator.generateImage( view, 1, true );
        ImageIO.write( image, "PNG", file );
    }

    public void testReadTrack()
    {
        DataElementPath trackPath = DataElementPath.create( "databases/generic/testReadTrack" );
        CombinedTrack track = trackPath.optDataElement( CombinedTrack.class );
        assertNotNull( "Can not read track", track );
    }

    public void testSaveTrack() throws Exception
    {
        String trackName = combinedTrack.getName();
        DataElementPath trackPath = DataElementPath.create( "databases/generic", trackName );
        DataCollection dc = trackPath.getParentCollection();
        assertNull( "Collection already contains element", dc.get( trackName ) );
        trackPath.save( combinedTrack );
        assertNotNull( "Collection does not contain saved element", dc.get( trackName ) );
        trackPath.remove();
    }

    @Override
    protected void setUp() throws Exception
    {
        super.setUp();
        VectorDataCollection<AnnotatedSequence> vdc = new VectorDataCollection<>( "test" );
        CollectionFactory.registerRoot( vdc );
        CollectionFactory.createRepository( "../data/test/ru/biosoft/bsa" );
        String seqString = "acgtacgtacgtacgatcgatcgatcgatgctagctagcatcgatcgatcgatcgatcgatcaaagtctgccagagaaagggtttgggtttgggaaaggtg";
        Sequence seq = new LinearSequence( "1", seqString, Nucleotide5LetterAlphabet.getInstance() );
        vdc.put( new MapAsVector( "1", vdc, seq, null ) );
        TrackImpl track1 = new TrackImpl( "track1", null );
        track1.addSite( new SiteImpl( null, "1_1", 1, 8, StrandType.STRAND_PLUS, seq ) );
        track1.addSite( new SiteImpl( null, "1_2", 16, 10, StrandType.STRAND_PLUS, seq ) );
        track1.addSite( new SiteImpl( null, "1_3", 35, 10, StrandType.STRAND_MINUS, seq ) );
        track1.addSite( new SiteImpl( null, "1_4", 43, 12, StrandType.STRAND_PLUS, seq ) );
        track1.finalizeAddition();
        innerTrack = track1;

        TrackImpl track2 = new TrackImpl( "track2", null );
        track2.addSite( new SiteImpl( null, "2_1", 6, 7, StrandType.STRAND_PLUS, seq ) );
        track2.addSite( new SiteImpl( null, "2_2", 27, 5, StrandType.STRAND_PLUS, seq ) );
        track2.addSite( new SiteImpl( null, "2_3", 29, 12, StrandType.STRAND_MINUS, seq ) );
        track2.finalizeAddition();
        
        List<Track> tracks = new ArrayList<>();
        tracks.add(track1);
        tracks.add(track2);
        String name = "combinedtrack" + Math.random();
        combinedTrack = new CombinedTrack( name, vdc, tracks, null );
        combinedTrack.getCondition().setDistance( 2 );

        File dir = new File( outDirectory );
        if( !dir.exists() && !dir.mkdir() )
            throw new RuntimeException( "Failed to create output directory" );

        dir = new File( imageDirectory );
        if( !dir.exists() && !dir.mkdir() )
            throw new RuntimeException( "Failed to create image directory" );

    }


}
