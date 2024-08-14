package ru.biosoft.bsa.analysis.maos;

import java.beans.PropertyDescriptor;

import com.developmentontheedge.beans.DynamicProperty;

import ru.biosoft.bsa.Interval;
import ru.biosoft.bsa.Sequence;
import ru.biosoft.bsa.Site;
import ru.biosoft.bsa.SiteModel;
import ru.biosoft.bsa.analysis.maos.coord_mapping.CoordinateMapping;
import ru.biosoft.util.bean.StaticDescriptor;

public class SiteMutation
{
    public final SiteModel model;
    
    public final Sequence refSeq;
    public final int refPos;
    public final double refScore;
    public final double refPValue;
    
    public final Sequence altSeq;
    public final int altPos;
    public final double altScore;
    public final double altPValue;
    
    public final double scoreDiff;
    public final double pValueLogFC;
    
    public final CoordinateMapping ref2alt;
    public final CoordinateMapping alt2ref;
    public final Variation[] variations;
    public TSSCollection genes;
    
    public SiteMutation(SiteModel model,
            Sequence refSeq, int refPos, double refScore, double refPValue,
            Sequence altSeq, int altPos, double altScore, double altPValue,
            double scoreDiff, double pValueLogFC,
            CoordinateMapping ref2alt, CoordinateMapping alt2ref,
            Variation[] variations)
    {
        this.model = model;
        this.refSeq = refSeq;
        this.refPos = refPos;
        this.refScore = refScore;
        this.refPValue = refPValue;
        this.altSeq = altSeq;
        this.altPos = altPos;
        this.altScore = altScore;
        this.altPValue = altPValue;
        this.scoreDiff = scoreDiff;
        this.pValueLogFC = pValueLogFC;
        this.ref2alt = ref2alt;
        this.alt2ref = alt2ref;
        this.variations = variations;
    }
    
    public SiteMutation(SiteMutation that)
    {
        this.model = that.model;
        this.refSeq = that.refSeq;
        this.refPos = that.refPos;
        this.refScore = that.refScore;
        this.refPValue = that.refPValue;
        this.altSeq = that.altSeq;
        this.altPos = that.altPos;
        this.altScore = that.altScore;
        this.altPValue = that.altPValue;
        this.scoreDiff = that.scoreDiff;
        this.pValueLogFC = that.pValueLogFC;
        this.ref2alt = that.ref2alt;
        this.alt2ref = that.alt2ref;
        this.variations = that.variations;
        that.genes = genes;
    }
    
    public boolean isSiteLoss()
    {
        return scoreDiff < 0;
    }
    
    public static final PropertyDescriptor SCORE_DIFF_PD = StaticDescriptor.create( "Score difference" );
    public static final PropertyDescriptor PVALUE_DIFF_PD = StaticDescriptor.create( "-log10(pvalue) difference" );
    public static final PropertyDescriptor REF_PVALUE_PD = StaticDescriptor.create( "Reference sequence p-value" );
    public static final PropertyDescriptor ALT_PVALUE_PD = StaticDescriptor.create( "Alternative sequence p-value" );
    
    public Site createReferenceSite(boolean addPValueProps)
    {
        if( refPos == -1 )//site can not be placed on reference
            return null;
        double score = model.getScore( refSeq, refPos );//called for side effect in IPSSiteModel
        Site s = model.constructSite( refSeq, refPos, score );//returned site is on chromosome coordinate system (not relative to refSeq)
        s.getProperties().add( new DynamicProperty( SCORE_DIFF_PD, Double.class, scoreDiff ) );
        if( addPValueProps )
        {
            s.getProperties().add( new DynamicProperty( PVALUE_DIFF_PD, Double.class, pValueLogFC ) );
            s.getProperties().add( new DynamicProperty( REF_PVALUE_PD, Double.class, refPValue ) );
            s.getProperties().add( new DynamicProperty( ALT_PVALUE_PD, Double.class, altPValue ) );
        }
        return s;
    }
    
    
    //Interval on reference relative to refSeq
    public Interval getSiteInterval()
    {
        Interval siteInterval;
        if(refPos == -1)
        {
            //site can not  be placed on reference (gain of site by full site insertion)
            //find insertion point
            int leftRefPos = -1;
            for(int x = altPos-1; x >= altSeq.getStart(); x--)
            {
                leftRefPos = alt2ref.get( x );
                if(leftRefPos != -1)
                    break;
            }
            int rightRefPos = -1;
            for(int x = altPos + model.getLength(); x < altSeq.getStart() + altSeq.getLength(); x++)
            {
                rightRefPos = alt2ref.get( x );
                if(rightRefPos == -1)
                    break;
            }
            if(leftRefPos == -1 || rightRefPos == -1)
            {
                return null;
            }
            siteInterval = new Interval( leftRefPos+1, rightRefPos-1 );
        }
        else
        {
            siteInterval = new Interval(refPos, refPos + model.getLength()-1);
        }
        return siteInterval;
    }
}
