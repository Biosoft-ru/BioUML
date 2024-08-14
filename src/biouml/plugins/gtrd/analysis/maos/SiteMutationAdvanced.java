package biouml.plugins.gtrd.analysis.maos;

import java.beans.PropertyDescriptor;

import com.developmentontheedge.beans.DynamicProperty;
import com.developmentontheedge.beans.DynamicPropertySet;

import ru.biosoft.bsa.Site;
import ru.biosoft.bsa.analysis.maos.SiteMutation;
import ru.biosoft.util.bean.StaticDescriptor;

public class SiteMutationAdvanced extends SiteMutation
{
    public final String tfClass, cell, treatment;
    public final String[] tfUniprots;
    public final GTRDPeak[] peaks;
    public final String[] pathways;
    public String[] diseaseList;
    public double avgPeakScore;
    public double finalScore;
    
    public SiteMutationAdvanced(SiteMutation main, String tfClass, String[] tfUniprots,
            String cell, String treatment, GTRDPeak[] peaks,
            String[] pathways, String[] diseaseList)
    {
        super( main );
        this.tfClass = tfClass;
        this.tfUniprots = tfUniprots;
        this.cell = cell;
        this.treatment = treatment;
        this.peaks = peaks;
        this.pathways = pathways;
        this.diseaseList = diseaseList;
        
        computeAvgPeakScore( peaks );
        computeFinalScore();
    }

    private void computeAvgPeakScore(GTRDPeak[] peaks)
    {
        for(GTRDPeak peak : peaks)
            avgPeakScore += peak.getScore();
        avgPeakScore /= peaks.length;
    }
    
    private void computeFinalScore()
    {
        finalScore = Math.signum( scoreDiff ) * (Math.abs( pValueLogFC ) + avgPeakScore);
    }
    
    public static final PropertyDescriptor AFG_PEAK_SCORE_PD = StaticDescriptor.create( "Average peak score" );
    public static final PropertyDescriptor FINAL_SCORE_PD = StaticDescriptor.create( "Final score" );
    public static final PropertyDescriptor GTRD_CELL_PD = StaticDescriptor.create( "GTRD cell" );
    public static final PropertyDescriptor GTRD_TREATMENT_PD = StaticDescriptor.create( "GTRD treatment" );
    
    @Override
    public Site createReferenceSite(boolean addPValueProps)
    {
        Site site = super.createReferenceSite( addPValueProps );
        if(site == null) //can not be placed on reference
            return null;
        DynamicPropertySet props = site.getProperties();
        props.add( new DynamicProperty( AFG_PEAK_SCORE_PD, Double.class, avgPeakScore ) );
        props.add( new DynamicProperty( FINAL_SCORE_PD, Double.class, finalScore ) );
        props.add( new DynamicProperty( GTRD_CELL_PD, String.class, cell ) );
        props.add( new DynamicProperty( GTRD_TREATMENT_PD, String.class, treatment ) );
        return site;
    }
}
