package biouml.plugins.gtrd.master.index._test;

import java.util.ArrayList;
import java.util.List;

import biouml.plugins.gtrd.master.index.OverlapUtils;
import biouml.plugins.gtrd.master.sites.GenomeLocation;
import junit.framework.TestCase;

public class OverlapUtilsTest extends TestCase
{
    public void testMapOverlapping1()
    {
        List<GenomeLocation> xs = new ArrayList<>();
        List<GenomeLocation> ys = new ArrayList<>();
        OverlapUtils.mapOverlapping( xs, ys, 100, (x,y)->assertTrue( false ) );
    }
    
    public void testMapOverlapping2()
    {
        List<GenomeLocation> xs = new ArrayList<>();
        xs.add( site(0,100) );
        List<GenomeLocation> ys = new ArrayList<>();
        ys.add( site(0,100) );
        ys.add( site(10,20) );
        ys.add( site(15,50) );
        ys.add( site(30,40) );
        
        List<GenomeLocation> resX = new ArrayList<>();
        List<GenomeLocation> resY = new ArrayList<>();
        OverlapUtils.mapOverlapping( xs, ys, 0, (x,y)->{ resX.add( x ); resY.add( y ); } );
        
        assertEquals( 4, resX.size() );
        assertEquals( 4, resY.size() );
        assertSiteEq( site(0,100), resX.get(0) );
        assertSiteEq( site(0,100), resY.get(0) );
        assertSiteEq( site(0,100), resX.get(1) );
        assertSiteEq( site(10,20), resY.get(1) );
        assertSiteEq( site(0,100), resX.get(2) );
        assertSiteEq( site(15,50), resY.get(2) );
        assertSiteEq( site(0,100), resX.get(3) );
        assertSiteEq( site(30,40), resY.get(3) );
    }
    
    public void testMapOverlapping3()
    {
        List<GenomeLocation> xs = new ArrayList<>();
        xs.add( site(0,100) );
        xs.add( site(-5,-1) );
        xs.add( site(0,10) );
        xs.add( site(16,18) );
        List<GenomeLocation> ys = new ArrayList<>();
        ys.add( site(0,100) );
        ys.add( site(10,20) );
        ys.add( site(15,50) );
        ys.add( site(30,40) );
        
        List<GenomeLocation> resX = new ArrayList<>();
        List<GenomeLocation> resY = new ArrayList<>();
        OverlapUtils.mapOverlapping( xs, ys, 0, (x,y)->{ resX.add( x ); resY.add( y ); } );
        
        assertSiteEq( site(0,100), resX.get(0) );
        assertSiteEq( site(0,100), resY.get(0) );
        
        assertSiteEq( site(0,100), resX.get(1) );
        assertSiteEq( site(10,20), resY.get(1) );
        
        assertSiteEq( site(0,100), resX.get(2) );
        assertSiteEq( site(15,50), resY.get(2) );
        
        assertSiteEq( site(0,100), resX.get(3) );
        assertSiteEq( site(30,40), resY.get(3) );
        
        assertSiteEq( site(0,10), resX.get(4) );
        assertSiteEq( site(0,100), resY.get(4) );
        
        assertSiteEq( site(0,10), resX.get(5) );
        assertSiteEq( site(10,20), resY.get(5) );

        assertSiteEq( site(16,18), resX.get(6) );
        assertSiteEq( site(0,100), resY.get(6) );

        assertSiteEq( site(16,18), resX.get(7) );
        assertSiteEq( site(10,20), resY.get(7) );
        
        assertSiteEq( site(16,18), resX.get(8) );
        assertSiteEq( site(15,50), resY.get(8) );
        
        assertEquals( 9, resX.size() );
        assertEquals( 9, resY.size() );
    }
    
    private GenomeLocation site(int from, int to)
    {
        GenomeLocation res = new GenomeLocation();
        res.setFrom( from );
        res.setTo( to );
        return res;
    }
    
    private void assertSiteEq(GenomeLocation x, GenomeLocation y)
    {
        assertTrue( x.getFrom() == y.getFrom() && x.getTo() == y.getTo() );
    }
}
