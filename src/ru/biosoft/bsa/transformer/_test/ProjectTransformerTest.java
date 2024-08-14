package ru.biosoft.bsa.transformer._test;

import java.awt.Color;

import junit.framework.TestCase;
import ru.biosoft.access.Entry;
import ru.biosoft.access.core.CollectionFactory;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.bsa.AnnotatedSequence;
import ru.biosoft.bsa.Track;
import ru.biosoft.bsa.project.Project;
import ru.biosoft.bsa.project.ProjectAsLists;
import ru.biosoft.bsa.project.Region;
import ru.biosoft.bsa.project.TrackInfo;
import ru.biosoft.bsa.transformer.ProjectTransformer;
import ru.biosoft.bsa.view.MapViewOptions;
import ru.biosoft.bsa.view.SiteViewOptions;
import ru.biosoft.bsa.view.ViewOptions;
import ru.biosoft.bsa.view.colorscheme.SiteColorScheme;
import ru.biosoft.bsa.view.colorscheme.SiteWeightColorScheme;
import ru.biosoft.graphics.Brush;

public class ProjectTransformerTest extends TestCase
{
    public void testSimple() throws Exception
    {
        CollectionFactory.createRepository( "../data/test/ru/biosoft/bsa" );
        ProjectTransformer transformer = new ProjectTransformer();
        assertNotNull("Can not create project transformer", transformer);
        Project project = getTestProject();
        Entry entry = transformer.transformOutput(project);
        assertNotNull("Can not transform Project to Entry", entry);
        Project project2 = transformer.transformInput(entry);
        assertNotNull("Can not transform Entry to Project", project2);
        compare(project, project2);
    }
    
    protected Project getTestProject()
    {
        Project project = new ProjectAsLists("test", null);
        project.setDescription("Test description\nin two lines");

        AnnotatedSequence seq1 = DataElementPath.create( "databases/Sequence analysis/sequences/embl/ADHBADA2" )
                .getDataElement( AnnotatedSequence.class );
        Region r1 = new Region( seq1 );
        r1.setTitle("ADHBADA2");
        r1.setFrom(5);
        r1.setTo(10);
        r1.setOrder(2);
        r1.setStrand(1);
        r1.setVisible(false);
        r1.setDescription("Region 1 description");
        project.addRegion(r1);

        AnnotatedSequence seq2 = DataElementPath.create( "databases/Sequence analysis/sequences/embl/AGHBD" )
                .getDataElement( AnnotatedSequence.class );
        Region r2 = new Region( seq2 );
        r2.setTitle("AGHBD");
        r2.setFrom(1);
        r2.setTo(100);
        r2.setOrder(1);
        r2.setStrand(2);
        r2.setVisible(true);
        r2.setDescription("Region 2 description");
        project.addRegion(r2);

        Track track1 = seq1.iterator().next();
        TrackInfo t1 = new TrackInfo(track1);
        t1.setTitle("ADHBADA2");
        t1.setGroup("group1");
        t1.setOrder(2);
        t1.setVisible(false);
        t1.setDescription("Track 1 descr\nsecond line test");
        project.addTrack(t1);

        Track track2 = seq2.iterator().next();
        TrackInfo t2 = new TrackInfo(track2);
        t2.setTitle("AGHBD");
        t2.setGroup("group2");
        t2.setOrder(1);
        t2.setVisible(true);
        t2.setDescription("Track 2 descr");
        project.addTrack(t2);
        
        {
            SiteViewOptions t1VO = project.getViewOptions().getTrackViewOptions( track1.getCompletePath() );
            t1VO.setShowTitle( false );
            SiteColorScheme cs = t1VO.getColorScheme();
            cs.setDefaultBrush( new Brush( Color.CYAN ) );
        }
        
        {
            SiteViewOptions t2VO = project.getViewOptions().getTrackViewOptions( track2.getCompletePath() );
            t2VO.setColorSchemeName( "By weight" );
            SiteWeightColorScheme cs = (SiteWeightColorScheme)t2VO.getColorScheme();
            cs.setMinValue( 10 );
            cs.setMaxValue( 100 );
            cs.setWeightProperty( "score" );
            cs.setFirstColor( Color.LIGHT_GRAY );
            cs.setSecondColor( Color.BLACK );
        }

        return project;
    }

    private void compare(Project p1, Project p2) throws Exception
    {
        assertEquals("Wrong name", p1.getName(), p2.getName());
        assertEquals("Wrong description", p1.getDescription(), p2.getDescription());

        Region[] regions1 = p1.getRegions();
        Region[] regions2 = p2.getRegions();
        assertEquals("Wrong region number", regions1.length, regions2.length);
        for( int i = 0; i < regions1.length; i++ )
        {
            Region r1 = regions1[i];
            Region r2 = regions2[i];
            assertEquals("Wrong sequence", r1.getSequencePath(), r2.getSequencePath());
            assertEquals("Wrong title", r1.getTitle(), r2.getTitle());
            assertEquals("Wrong interval", r1.getInterval(), r2.getInterval());
            assertEquals("Wrong order", r1.getOrder(), r2.getOrder());
            assertEquals("Wrong visible", r1.isVisible(), r2.isVisible());
            assertEquals("Wrong description", r1.getDescription(), r2.getDescription());
        }
        ViewOptions vo1 = p1.getViewOptions();
        ViewOptions vo2 = p2.getViewOptions();
        compareSequenceViewOptions( vo1.getRegionViewOptions(), vo2.getRegionViewOptions() );

        TrackInfo[] tracks1 = p1.getTracks();
        TrackInfo[] tracks2 = p2.getTracks();
        assertEquals("Wrong track number", tracks1.length, tracks2.length);
        for( int i = 0; i < tracks1.length; i++ )
        {
            TrackInfo t1 = tracks1[i];
            TrackInfo t2 = tracks2[i];
            assertEquals("Wrong DB", t1.getDbName(), t2.getDbName());
            assertEquals("Wrong title", t1.getTitle(), t2.getTitle());
            assertEquals("Wrong group", t1.getGroup(), t2.getGroup());
            assertEquals("Wrong order", t1.getOrder(), t2.getOrder());
            assertEquals("Wrong visible", t1.isVisible(), t2.isVisible());
            assertEquals("Wrong description", t1.getDescription(), t2.getDescription());
            compareTrackViewOptions(vo1.getTrackViewOptions( t1.getTrack().getCompletePath() ), vo2.getTrackViewOptions( t2.getTrack().getCompletePath() ));
        }
    }
    
    private void compareTrackViewOptions(SiteViewOptions a, SiteViewOptions b)
    {
        assertEquals(a.getBoxHeight(), b.getBoxHeight());
        assertEquals( a.getColorSchemeName(), b.getColorSchemeName() );
        assertEquals( a.getDisplayName(), b.getDisplayName() );
        assertEquals( a.getInterval(), b.getInterval() );
        assertEquals( a.getMaxProfileHeight(), b.getMaxProfileHeight() );
        assertEquals( a.getMinProfileHeight(), b.getMinProfileHeight() );
        assertEquals( a.getTrackDisplayMode(), b.getTrackDisplayMode() );
        assertEquals( a.getColorScheme(), b.getColorScheme() );
        assertEquals( a.getFont(), b.getFont() );
        assertEquals( a.getLayoutAlgorithm().getClass(), b.getLayoutAlgorithm().getClass() );
        assertEquals( a.getSequenceFont(), b.getSequenceFont() );
        assertEquals( a.getTrackTitleFont(), b.getTrackTitleFont() );
        //assertEquals( a.getViewTagger(), b.getViewTagger() );
        assertEquals( a.isShowBox(), b.isShowBox() );
        assertEquals( a.isShowPositions(), b.isShowPositions() );
        assertEquals( a.isShowSequence(), b.isShowSequence() );
        assertEquals( a.isShowStrand(), b.isShowStrand());
        assertEquals( a.isShowStructure(), b.isShowStructure() );
        assertEquals( a.isShowTitle(), b.isShowTitle() );
    }

    private void compareSequenceViewOptions(MapViewOptions regionViewOptions, MapViewOptions regionViewOptions2)
    {
        // TODO Auto-generated method stub
        
    }
}
