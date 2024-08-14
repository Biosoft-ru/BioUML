package biouml.plugins.gtrd.master.progs;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import biouml.plugins.gtrd.master.MasterTrack;
import biouml.plugins.gtrd.master.sites.MasterSite;
import biouml.plugins.gtrd.master.sites.PWMMotif;
import biouml.plugins.gtrd.master.sites.chipseq.ChIPSeqPeak;
import biouml.plugins.gtrd.master.sites.chipseq.GEMPeak;
import biouml.plugins.gtrd.master.sites.chipseq.MACS2ChIPSeqPeak;
import biouml.plugins.gtrd.master.sites.chipseq.PICSPeak;
import biouml.plugins.gtrd.master.sites.chipseq.SISSRSPeak;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataCollectionConfigConstants;
import ru.biosoft.bsa.track.big.BigBedTrack;

public class ExtractSitesFromMasterTrack {
	public static void main(String[] args) throws IOException {
		Properties props = new Properties();
		String name = "mt.ANDR_HUMAN.v1.bb";
        props.setProperty( DataCollectionConfigConstants.NAME_PROPERTY, name );
        props.setProperty( BigBedTrack.PROP_BIGBED_PATH, "data/gtrd_data/"+name );
		MasterTrack mt = new MasterTrack(null, props);
		int total=0;
		int supported=0;
        //String expId = "EXP036542";//R1881 for 30min
		String expId = "EXP036543";//10nM R1881 for 4h
        Set<String> untreatedExpIds = new HashSet<>();
        untreatedExpIds.add( "EXP060882" );
        untreatedExpIds.add( "EXP000732" );
        untreatedExpIds.add( "EXP000408" );
        List<Features> features = new ArrayList<>();
		for(String chr : mt.getChromosomes())
		{
		    System.out.println("Processing " +  chr);
			List<MasterSite> mss = mt.query(chr);
			for(MasterSite ms : mss)
			{
				total++;
                if(!isSupportedByExp(ms, expId))
					continue;
                Features f = computeFeatures( ms, expId, untreatedExpIds );
                features.add( f );
				supported++;
			}
		}
		System.out.println("Total sites: " + total);
		System.out.println("Supported by this exp: " + supported);
		//writeFeatures(features, new File("data/analysis/sites/gtrd_master_sites_EXP036542.txt"));
		writeFeatures(features, new File("data/analysis/sites/gtrd_master_sites_EXP036543.txt"));
	}
	
    static class Features
	{
	    String msId;
	    String chr;
	    int from;
	    int to;
	    
		float macs2FoldEnrichment = Float.NaN, macs2mLog10PValue = Float.NaN, macs2mLog10QValue = Float.NaN;
		float gemFold = Float.NaN, gemPMLog10 = Float.NaN, gemQMLog10 = Float.NaN;
		float sissrsFold = Float.NaN, sissrsPValue = Float.NaN;
		float picsScore = Float.NaN;
		
		float hocomocoScore = Float.NaN;
		int closestHocomocoDistance = Integer.MAX_VALUE;
		
		boolean supportedByUntreatedExps = false;
		float untreatedMACS2FoldEnrichment = Float.NaN, untreatedMACS2mLog10PValue = Float.NaN;
	}
    
    private static void writeFeatures(List<Features> features, File file) throws IOException
    {
        BufferedWriter writer = new BufferedWriter(new FileWriter( file ));
        writer.append( "ID\tchr\tfrom\tto\tMACS2FoldEnrichment\tMACS2MLog10PValue\tMACS2MLog10QValue\tGEMFold\tGEMPMLog10\tgemQMLog10\tSISSRSFold\tSISSRSPValue\tPICSScore\tHOCOMOCO_SCORE\tHOCOMOCO_DISTANCE\tUntreated\tUntreatedMACS2FoldEnrichment\tUntreatedMACS2MLog10PValue\n" );
        for(Features f : features)
        {
            writer
            .append( f.msId )
            .append( '\t' ).append( f.chr )
            .append( '\t' ).append( String.valueOf(f.from) )
            .append( '\t' ).append( String.valueOf(f.to) )
            .append( '\t' ).append( String.valueOf(f.macs2FoldEnrichment) )
            .append( '\t' ).append( String.valueOf(f.macs2mLog10PValue) )
            .append( '\t' ).append( String.valueOf(f.macs2mLog10QValue) )
            .append( '\t' ).append( String.valueOf(f.gemFold) )
            .append( '\t' ).append( String.valueOf(f.gemPMLog10) )
            .append( '\t' ).append( String.valueOf(f.gemQMLog10) )
            .append( '\t' ).append( String.valueOf(f.sissrsFold) )
            .append( '\t' ).append( String.valueOf(f.sissrsPValue) )
            .append( '\t' ).append( String.valueOf(f.picsScore) )
            .append( '\t' ).append( String.valueOf(f.hocomocoScore) )
            .append( '\t' ).append( f.closestHocomocoDistance==Integer.MAX_VALUE?"NaN":String.valueOf(f.closestHocomocoDistance) )
            .append( '\t' ).append( String.valueOf(f.supportedByUntreatedExps) )
            .append( '\t' ).append( String.valueOf(f.untreatedMACS2FoldEnrichment) )
            .append( '\t' ).append( String.valueOf(f.untreatedMACS2mLog10PValue) )
            .append( '\n' );
        }
        writer.close();
    }
    
	static Features computeFeatures(MasterSite ms, String expId, Set<String> untreatedExpIds)
	{
	    Features f = new Features();
	    f.msId = ms.getStableId();
	    f.chr = ms.getChr();
	    f.from = ms.getFrom();
	    f.to = ms.getTo();
	    
	    for(ChIPSeqPeak peak : ms.getChipSeqPeaks())
	    {
	        if(!peak.getExp().getName().equals( expId ))
	            continue;
	            
	        if(peak instanceof MACS2ChIPSeqPeak)
	        {
	            MACS2ChIPSeqPeak cPeak = (MACS2ChIPSeqPeak)peak;
	            f.macs2FoldEnrichment = maxNAN(f.macs2FoldEnrichment, cPeak.getFoldEnrichment());
	            f.macs2mLog10PValue = maxNAN(f.macs2mLog10PValue, cPeak.getMLog10PValue());
	            f.macs2mLog10QValue = maxNAN(f.macs2mLog10QValue, cPeak.getMLog10QValue());
	        } else if(peak instanceof GEMPeak)
	        {
	            GEMPeak cPeak = (GEMPeak)peak;
	            f.gemFold = maxNAN( f.gemFold, cPeak.getFold());
	            f.gemPMLog10 = maxNAN( f.gemPMLog10, cPeak.getPMLog10());
	            f.gemQMLog10 = maxNAN( f.gemQMLog10, cPeak.getQMLog10());
	        } else if(peak instanceof SISSRSPeak)
	        {
	            SISSRSPeak cPeak = (SISSRSPeak)peak;
	            f.sissrsFold = maxNAN(f.sissrsFold, cPeak.getFold());
	            f.sissrsPValue = maxNAN(f.sissrsPValue, cPeak.getPValue());
	        } else if(peak instanceof PICSPeak)
	        {
	            PICSPeak cPeak = (PICSPeak)peak;
	            f.picsScore = maxNAN(f.picsScore, cPeak.getPicsScore());
	        }
	    }
	    
	    for(ChIPSeqPeak peak : ms.getChipSeqPeaks())
        {
            if(!untreatedExpIds.contains(peak.getExp().getName()))
                continue;
            f.supportedByUntreatedExps = true;
            if(peak instanceof MACS2ChIPSeqPeak)
            {
                MACS2ChIPSeqPeak cPeak = (MACS2ChIPSeqPeak)peak;
                f.untreatedMACS2FoldEnrichment = maxNAN(f.untreatedMACS2FoldEnrichment, cPeak.getFoldEnrichment());
                f.untreatedMACS2mLog10PValue = maxNAN(f.untreatedMACS2mLog10PValue, cPeak.getMLog10PValue());
            }
        }
	    
	    for(PWMMotif pwmMotif : ms.getMotifs())
	    {
	        f.hocomocoScore = maxNAN(f.hocomocoScore, (float)pwmMotif.getScore());
	        int distance = Math.abs(ms.getSummit() - pwmMotif.getSummit());
	        f.closestHocomocoDistance = Math.min( f.closestHocomocoDistance, distance );
	    }
	    
	    return f;
	}
	
	static float maxNAN(float x, float y)
	{
	    if(Double.isNaN( x ))
	        return y;
	    if(Double.isNaN( y ))
	        return x;
	    return x > y ? x : y;
	}
	
	static boolean isSupportedByExp(MasterSite ms, String expId)
	{
		for(ChIPSeqPeak peak : ms.getChipSeqPeaks())
		{
			if(peak.getExp().getName().equals(expId))
				return true;
		}
		return false;
	}
	

}
