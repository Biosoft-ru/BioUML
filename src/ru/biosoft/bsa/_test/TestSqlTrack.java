package ru.biosoft.bsa._test;

import java.util.Arrays;
import java.util.Comparator;

import com.developmentontheedge.beans.DynamicProperty;
import com.developmentontheedge.beans.DynamicPropertySet;
import com.developmentontheedge.beans.DynamicPropertySetAsMap;

import ru.biosoft.access._test.AbstractBioUMLTest;
import ru.biosoft.access.core.CollectionFactory;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.bsa.AnnotatedSequence;
import ru.biosoft.bsa.Basis;
import ru.biosoft.bsa.Precision;
import ru.biosoft.bsa.Sequence;
import ru.biosoft.bsa.Site;
import ru.biosoft.bsa.SiteImpl;
import ru.biosoft.bsa.SiteType;
import ru.biosoft.bsa.SqlTrack;
import ru.biosoft.bsa.StrandType;

public class TestSqlTrack extends AbstractBioUMLTest
{
    @Override
    protected void setUp() throws Exception
    {
        super.setUp();
        CollectionFactory.createRepository( "../data/test/ru/biosoft/bsa" );
    }
    
    public void testInsertSites() throws Exception
    {
        DataElementPath trackPath = DataElementPath.create( "databases/generic2/sqltrack_new" );
        SqlTrack track = SqlTrack.createTrack( trackPath, null);
        double[] profile1 = {1,2,3,4,5,6,7,8,9,10};
        
        for( int i = 0; i < 100000; i++ )
        {
            DynamicPropertySet properties = new DynamicPropertySetAsMap();
            properties.add( new DynamicProperty( "profile", double[].class, profile1 ) );
            Site site1 = new SiteImpl( null, "site1", SiteType.TYPE_MISC_SIGNAL, Basis.BASIS_PREDICTED, 1, 10, Precision.PRECISION_EXACTLY,
                    StrandType.STRAND_PLUS, null, properties );
            track.addSite( site1 );
        }
        
        track.finalizeAddition();
    }
    
    public void testOverlappingProfile() throws Exception
    {
        DataElementPath trackPath = DataElementPath.create( "databases/generic/sqltrack" );
        DataElementPath seqCollectionPath = DataElementPath.create( "databases/Ensembl/Sequences/chromosomes NCBI36" );
        
        SqlTrack track = SqlTrack.createTrack( trackPath, null, seqCollectionPath );
        Sequence sequence = seqCollectionPath.getChildPath( "1" ).getDataElement(AnnotatedSequence.class).getSequence();
        
        DynamicPropertySet properties = new DynamicPropertySetAsMap();
        double[] profile1 = {1,2,3,4,5,6,7,8,9,10};
        properties.add( new DynamicProperty( "profile", double[].class, profile1 ) );
        Site site1 = new SiteImpl( null, "site1", SiteType.TYPE_MISC_SIGNAL, Basis.BASIS_PREDICTED, 1, 10, Precision.PRECISION_EXACTLY, StrandType.STRAND_PLUS, sequence , properties );
        track.addSite( site1 );
        
        properties = new DynamicPropertySetAsMap();
        double[] profile2 = {11,12,13,14,15,16,17,18,19,20};
        properties.add( new DynamicProperty( "profile", double[].class, profile2 ) );
        Site site2 = new SiteImpl( null, "site2", SiteType.TYPE_MISC_SIGNAL, Basis.BASIS_PREDICTED, 2, 10, Precision.PRECISION_EXACTLY, StrandType.STRAND_PLUS, sequence , properties );
        track.addSite( site2 );
        
        track.finalizeAddition();
        
        trackPath.save( track );
        
        trackPath.getParentCollection().release( trackPath.getName() );
        track = trackPath.getDataElement( SqlTrack.class );
        
        DataCollection<Site> sites = track.getAllSites();
        checkSites( profile1, profile2, sites );
        
        sites = track.getSites( seqCollectionPath.getChildPath( "1" ).toString(), 0, 100 );
        checkSites( profile1, profile2, sites );

        trackPath.remove();
    }
    private void checkSites(double[] profile1, double[] profile2, DataCollection<Site> sites)
    {
        Site site1;
        Site site2;
        assertEquals( 2, sites.getSize() );
        site1 = site2 = null;
        for(Site s : sites)
        {
            if(s.getStart() == 1)
                site1 = s;
            else if(s.getStart() == 2)
                site2 = s;
        }
        assertNotNull( site1 );
        assertNotNull( site2 );
        
        Object profile1Obj = site1.getProperties().getValue( "profile" );
        assertTrue( profile1Obj instanceof double[] );
        double[] savedProfile1 = (double[])profile1Obj;
        assertEquals( profile1.length, savedProfile1.length );
        for(int i = 0; i < profile1.length; i++)
            assertEquals( profile1[i], savedProfile1[i] );
        
        
        Object profile2Obj = site2.getProperties().getValue( "profile" );
        assertTrue( profile2Obj instanceof double[] );
        double[] savedProfile2 = (double[])profile2Obj;
        assertEquals( profile2.length, savedProfile2.length );
        for(int i = 0; i < profile2.length; i++)
            assertEquals( profile2[i], savedProfile2[i] );
    }
    
    public void testGetOverlappingSites()
    {
        DataElementPath trackPath = DataElementPath.create( "databases/generic/sqltrack2" );
        DataElementPath seqCollectionPath = DataElementPath.create( "databases/Ensembl/Sequences/chromosomes NCBI36" );
        
        SqlTrack track = SqlTrack.createTrack( trackPath, null, seqCollectionPath );
        
        Sequence sequence = seqCollectionPath.getChildPath( "1" ).getDataElement(AnnotatedSequence.class).getSequence();
        
        DynamicPropertySet properties = new DynamicPropertySetAsMap();
        properties.add( new DynamicProperty( "prop1", Double.class, 0.1 ) );
        Site site1 = new SiteImpl( null, "site1", SiteType.TYPE_MISC_SIGNAL, Basis.BASIS_PREDICTED, 1, 10, Precision.PRECISION_EXACTLY, StrandType.STRAND_PLUS, sequence , properties );
        track.addSite( site1 );
        
        properties = new DynamicPropertySetAsMap();
        properties.add( new DynamicProperty( "prop1", Double.class, 0.2 ) );
        Site site2 = new SiteImpl( null, "site2", SiteType.TYPE_MISC_SIGNAL, Basis.BASIS_PREDICTED, 1, 10, Precision.PRECISION_EXACTLY, StrandType.STRAND_PLUS, sequence , properties );
        track.addSite( site2 );


        properties = new DynamicPropertySetAsMap();
        properties.add( new DynamicProperty( "prop1", Double.class, 0.3 ) );
        Site site3 = new SiteImpl( null, "site3", SiteType.TYPE_MISC_SIGNAL, Basis.BASIS_PREDICTED, 10, 3, Precision.PRECISION_EXACTLY, StrandType.STRAND_PLUS, sequence , properties );
        track.addSite( site3 );

        properties = new DynamicPropertySetAsMap();
        properties.add( new DynamicProperty( "prop1", Double.class, 0.4 ) );
        Site site4 = new SiteImpl( null, "site4", SiteType.TYPE_MISC_SIGNAL, Basis.BASIS_PREDICTED, 13, 3, Precision.PRECISION_EXACTLY, StrandType.STRAND_PLUS, sequence , properties );
        track.addSite( site4 );
        
        sequence = seqCollectionPath.getChildPath( "2" ).getDataElement(AnnotatedSequence.class).getSequence();
        properties = new DynamicPropertySetAsMap();
        properties.add( new DynamicProperty( "prop1", Double.class, 0.5 ) );
        Site site5 = new SiteImpl( null, "site5", SiteType.TYPE_MISC_SIGNAL, Basis.BASIS_PREDICTED, 1, 10, Precision.PRECISION_EXACTLY, StrandType.STRAND_PLUS, sequence , properties );
        track.addSite( site5 );

        track.finalizeAddition();

        DataCollection<Site> sites = track.getSites( "databases/Ensembl/Sequences/chromosomes NCBI36/1", 3, 10 );
        assertEquals( 3, sites.getSize() );
        Site[] sitesArr = new Site[sites.getSize()];
        int i = 0;
        for(Site s : sites)
            sitesArr[i++] = s;
        Arrays.sort( sitesArr, Comparator.comparingInt( Site::getFrom ));

        assertEquals( "1", sitesArr[0].getOriginalSequence().getName() );
        assertEquals( 1, sitesArr[0].getFrom());
        assertEquals( 10, sitesArr[0].getTo());
        
        assertEquals( "1", sitesArr[1].getOriginalSequence().getName() );
        assertEquals( 1, sitesArr[1].getFrom());
        assertEquals( 10, sitesArr[1].getTo());

        assertEquals( "1", sitesArr[2].getOriginalSequence().getName() );
        assertEquals( 10, sitesArr[2].getFrom());
        assertEquals( 12, sitesArr[2].getTo());

        trackPath.remove();
    }
}
