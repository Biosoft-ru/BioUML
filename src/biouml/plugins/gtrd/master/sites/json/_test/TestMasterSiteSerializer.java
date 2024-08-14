package biouml.plugins.gtrd.master.sites.json._test;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.file.Files;
import java.nio.file.Paths;

import biouml.plugins.gtrd.master.meta.Metadata;
import biouml.plugins.gtrd.master.meta.json.MetadataSerializer;
import biouml.plugins.gtrd.master.sites.MasterSite;
import biouml.plugins.gtrd.master.sites.chipseq.ChIPSeqPeak;
import biouml.plugins.gtrd.master.sites.chipseq.MACS2ChIPSeqPeak;
import biouml.plugins.gtrd.master.sites.json.MasterSiteSerializer;
import junit.framework.TestCase;

public class TestMasterSiteSerializer extends TestCase
{
    public void testParse() throws IOException
    {
        String siteJson = new String(Files.readAllBytes( Paths.get( "biouml/plugins/gtrd/master/sites/json/_test/resources/master_site.json" ) ), "utf8");
        Metadata metadata = getMetadata();
        _testMasterSiteParse( siteJson, metadata );
    }

    private Metadata getMetadata() throws UnsupportedEncodingException, IOException
    {
        String metadataJson = new String(Files.readAllBytes( Paths.get( "biouml/plugins/gtrd/master/sites/json/_test/resources/metadata.json" ) ), "utf8");
        MetadataSerializer serializer = new MetadataSerializer();
        Metadata metadata = serializer.fromJSON( metadataJson );
        return metadata;
    }
    
    private MasterSite _testMasterSiteParse(String json, Metadata metadata) throws IOException
    {
        MasterSiteSerializer serializer = new MasterSiteSerializer( null, metadata );
        MasterSite ms = serializer.fromJSON( json );
        assertEquals( 31, ms.getId() );
        assertEquals( 15, ms.getSummit() );
        assertEquals( 2, ms.getVersion() );
        assertEquals( "stable", ms.getReliabilityLevel() );
        assertEquals( 1.0f, ms.getReliabilityScore() );
        
        //assertEquals(5, ms.chipSeqPeaks.size());
        ChIPSeqPeak peak0 = ms.getChipSeqPeaks().get( 0 );
        assertEquals(1, peak0.getId());
        assertEquals("macs2", peak0.getPeakCaller());
        //assertEquals("EXP048613", peak0.exp.getName());
        assertEquals("1", peak0.getChr());
        assertEquals(778449, peak0.getFrom());
        assertEquals(778908, peak0.getTo());
        assertTrue( peak0 instanceof MACS2ChIPSeqPeak );
        assertEquals(0.0f, ((MACS2ChIPSeqPeak)peak0).getFoldEnrichment());
        assertEquals(24.5802f, ((MACS2ChIPSeqPeak)peak0).getMLog10PValue());
        assertEquals(20.6714f, ((MACS2ChIPSeqPeak)peak0).getMLog10QValue());
        assertEquals(239, ((MACS2ChIPSeqPeak)peak0).getSummit());
        assertEquals(35, ((MACS2ChIPSeqPeak)peak0).getPileup());
        return ms;
    }
    
    public void testWrite() throws IOException
    {
        Metadata metadata = getMetadata();
        
        MasterSite ms = new MasterSite();
        ms.setId( 31 );
        ms.setChr( "1" );
        ms.setFrom( 778676 );
        ms.setTo( 778706 );
        ms.setSummit( 15 );
        ms.setVersion( 2 );
        ms.setReliabilityLevel("stable");
        ms.setReliabilityScore(1.0f);
        MACS2ChIPSeqPeak peak0 = new MACS2ChIPSeqPeak();
        peak0.setId( 1 );
        peak0.setExp(metadata.chipSeqExperiments.get( "EXP048613" ));
        peak0.setChr( "1" );
        peak0.setFrom(778449);
        peak0.setTo(778908);
        peak0.setFoldEnrichment( 0.0f );
        peak0.setMLog10PValue( 24.5802f );
        peak0.setMLog10QValue( 20.6714f );
        peak0.setSummit( 239 );
        peak0.setPileup(35);
        ms.getChipSeqPeaks().add( peak0 );
        
        
        MasterSiteSerializer serializer = new MasterSiteSerializer( null, metadata );
        String json = serializer.toPrettyJSON( ms );
        
        System.out.println(json);
        
        _testMasterSiteParse( json, metadata );
    }
}
