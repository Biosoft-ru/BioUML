package biouml.plugins.gtrd.master;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import biouml.plugins.gtrd.ChIPexoExperiment;
import biouml.plugins.gtrd.ChIPseqExperiment;
import biouml.plugins.gtrd.DNaseExperiment;
import biouml.plugins.gtrd.master.sites.MasterSite;
import biouml.plugins.gtrd.master.sites.PWMMotif;
import biouml.plugins.gtrd.master.sites.Peak;
import biouml.plugins.gtrd.master.sites.chipexo.ChIPexoPeak;
import biouml.plugins.gtrd.master.sites.chipseq.ChIPSeqPeak;
import biouml.plugins.gtrd.master.sites.dnase.DNaseFootprint;
import biouml.plugins.gtrd.master.sites.dnase.DNasePeak;
import biouml.plugins.gtrd.master.sites.dnase.FootprintCluster;

public class VelocityHelper
{

    public List<String> getCellTreatmentList(MasterSite ms)
    {
        Set<String> uniqueLines = new HashSet<>();
        
        for(ChIPSeqPeak peak : ms.getChipSeqPeaks())
        {
            ChIPseqExperiment exp = peak.getExp();
            String line = exp.getCell().getTitle();
            if(!exp.getTreatment().isEmpty() && !exp.getTreatment().equalsIgnoreCase( "none" ))
                line += " treated with " + exp.getTreatment();
            uniqueLines.add( line );
        }
        for(ChIPexoPeak peak : ms.getChipExoPeaks())
        {
            ChIPexoExperiment exp = peak.getExp();
            String line = exp.getCell().getTitle();
            if(!exp.getTreatment().isEmpty() && !exp.getTreatment().equalsIgnoreCase( "none" ))
                line += " treated with " + exp.getTreatment();
            uniqueLines.add( line );
        }
        List<String> result = new ArrayList<>();
        result.addAll( uniqueLines );
        Collections.sort( result );
        return result;
    }
    
    public List<String> getSupportedByList(MasterSite ms)
    {
        List<String> result = new ArrayList<>();
        if(!ms.getChipSeqPeaks().isEmpty())
        {
            int n = countExperiments( ms.getChipSeqPeaks() );
            result.add( formatPlural("ChIP-seq experiment", n));
        }
        if(!ms.getChipExoPeaks().isEmpty())
        {
            int n = countExperiments( ms.getChipExoPeaks() );
            result.add( formatPlural( "ChIP-exo experiment", n) );
        }
        
        addSupportedByDNase( ms, result );
        
        if(!ms.getMnasePeaks().isEmpty())
        {
            int n = countExperiments( ms.getMnasePeaks() );
            result.add( formatPlural( "MNase-seq experiment", n) );
        }
        
        if(!ms.getMotifs().isEmpty())
        {
            int nMotifs = 0;
            for(PWMMotif m : ms.getMotifs())
                if(m.getInterval().intersects( ms.getInterval() ))
                    nMotifs++;
            if(nMotifs > 0)
                result.add( formatPlural("DNA motif", nMotifs) );
        }
        
        return result;
    }

    public void addSupportedByDNase(MasterSite ms, List<String> result)
    {
        Set<String> cells = new HashSet<>();
        for(ChIPSeqPeak peak : ms.getChipSeqPeaks())
        {
            String cellId = peak.getExp().getCell().getName();
            cells.add( cellId );
        }
        for(ChIPexoPeak peak : ms.getChipExoPeaks())
        {
            String cellId = peak.getExp().getCell().getName();
            cells.add( cellId );
        }
        Set<String> exps = new HashSet<>();
        for(DNasePeak peak : ms.getDnasePeaks())
        {
            DNaseExperiment exp = peak.getExp();
            if(!cells.contains( exp.getCell().getName() ))
                continue;
            exps.add( exp.getName() );
        }
        if(exps.size() > 0)
            result.add( formatPlural("DNase-seq experiment", exps.size()) );
        
        boolean hasFooprints = false;
        for( FootprintCluster fpc : ms.getFootprintClusters() )
        {
            if( cells.contains( fpc.getCell().getName() ) )
                hasFooprints = true;
        }
        if( !hasFooprints )
        {
            for( DNaseFootprint fp : ms.getDnaseFootprints() )
            {
                if( cells.contains( fp.getExp().getCell().getName() ) )
                {
                    hasFooprints = true;
                    break;
                }
            }
        }
        if(hasFooprints)
            result.add( "Has DNase-seq footprints" );
    }
    
    private int countExperiments(List<? extends Peak> peaks)
    {
        Set<String> idSet = new HashSet<>();
        for(Peak peak : peaks)
            idSet.add( peak.getExp().getName() );
        return idSet.size();
    }
    
    private String formatPlural(String what, int n)
    {
        return n + " " + what + (n>1?"s":"");
    }
    
    
}
