package biouml.plugins.gtex.meos;

import java.util.ArrayList;
import java.util.List;

import ru.biosoft.bsa.Interval;
import ru.biosoft.bsa.Sequence;
import ru.biosoft.bsa.SiteModel;
import ru.biosoft.bsa.analysis.maos.IResultHandler;
import ru.biosoft.bsa.analysis.maos.IntervalData;
import ru.biosoft.bsa.analysis.maos.Parameters;
import ru.biosoft.bsa.analysis.maos.SiteModelTask;
import ru.biosoft.bsa.analysis.maos.SiteMutation;
import ru.biosoft.bsa.analysis.maos.Variation;

public class GTEXSiteModelTask extends SiteModelTask
{
    public GTEXSiteModelTask(SiteModel siteModel, IntervalData data, IResultHandler resultHandler, Parameters parameters)
    {
        super( siteModel, data, resultHandler, parameters );
    }
    
    @Override
    public void run()
    {
        computeScores();
        
        findBestRefSite(referenceScores, alternativeScores);
        findBestAltSite(referenceScores, alternativeScores);
        findBestFC(referenceScores, alternativeScores, Double.NEGATIVE_INFINITY, "MAX_CHANGE");
        findBestFC(referenceScores, alternativeScores, siteModel.getPValueCutoff().getCutoff( 0.01 ), "MAX_CHANGE_P0.01");
        findBestFC(referenceScores, alternativeScores, siteModel.getPValueCutoff().getCutoff( 0.001 ), "MAX_CHANGE_P0.001");
        findBestFC(referenceScores, alternativeScores, siteModel.getPValueCutoff().getCutoff( 0.0001 ), "MAX_CHANGE_P0.0001");
    }
    
    
    double[] referenceScores, alternativeScores;
    int refFrom, refTo;
    int altFrom, altTo;
    private void computeScores()
    {
        Sequence reference = data.getReference();
        Sequence alternative = data.getAlternative();
        
        refFrom = data.getVariations()[0].getFrom() - siteModel.getLength() + 1;
        refTo = data.getVariations()[data.getVariations().length - 1].getTo();
        
        altFrom = data.getInvertedVariations()[0].getFrom() - siteModel.getLength() + 1;
        altTo = data.getInvertedVariations()[data.getInvertedVariations().length - 1].getTo();
        
        referenceScores = new double[reference.getStart() + reference.getLength() - siteModel.getLength() + 1];
        alternativeScores = new double[alternative.getStart() + alternative.getLength() - siteModel.getLength() + 1];
        for( int pos = refFrom; pos <= refTo; pos++ )
            referenceScores[pos] = siteModel.getScore( reference, pos );
        for( int pos = altFrom; pos <= altTo; pos++ )
            alternativeScores[pos] = siteModel.getScore( alternative, pos );

    }

    private void findBestFC(double[] referenceScores, double[] alternativeScores, double scoreCutoff, String type)
    {
        SiteMutation best = null;
        double maxDiff = 0;
        for( int refPos = refFrom; refPos <= refTo; refPos++ )
            if( referenceScores[refPos] > scoreCutoff )
            {
                double refScore = referenceScores[refPos];
                double refPValue = parameters.isPValueMode() ? siteModel.getPValueCutoff().getPvalue( refScore ) : 1;
                
                int bestAltPos = -1;
                double bestScore = -Double.MAX_VALUE;
                for( Integer altPos : data.getRef2Alt().mapInterval( new Interval( refPos, refPos + siteModel.getLength() - 1 ) ) )
                    if(alternativeScores[altPos] > bestScore)
                    {
                        bestAltPos = altPos;
                        bestScore = alternativeScores[altPos];
                    }
                
                double altScore;
                double altPValue;
                
                if(bestAltPos == -1)//complete deletion of site
                {
                    altScore  = 0;//TODO: model.getMinScore()
                    altPValue = 1;
                }
                else
                {
                    altScore = alternativeScores[bestAltPos];
                    altPValue = parameters.isPValueMode() ? siteModel.getPValueCutoff().getPvalue( altScore ) : 1;
                }
                
                double scoreDiff = refScore - altScore;
                double pValueLogFC = ( -Math.log10( refPValue ) ) - ( -Math.log10( altPValue ) );
                double diff = parameters.isPValueMode() ? pValueLogFC : scoreDiff;
                if( diff >= maxDiff )
                {
                    maxDiff = diff;
                    List<Variation> intersectedVariations = new ArrayList<>();
                    Variation[] variations = data.getVariations();
                    for( int i = 0; i < variations.length; i++ )
                    {
                        if( variations[i].intersects( new Interval( refPos, refPos + siteModel.getLength() - 1 ) ) )
                            intersectedVariations.add( variations[i] );
                            
                    }
                    best = new GTEXSiteMutation( type, siteModel,
                            data.getReference(), refPos, refScore, refPValue, data.getAlternative(), bestAltPos, altScore, altPValue,
                            -scoreDiff, -pValueLogFC,//site loss, scores should be negative
                            data.getRef2Alt(), data.getAlt2Ref(),
                            intersectedVariations.toArray( new Variation[0] ) );
                }
            }
        
        
        for( int altPos = altFrom; altPos <= altTo; altPos++ )
            if( alternativeScores[altPos] > scoreCutoff )
            {
                double altScore = alternativeScores[altPos];
                double altPValue = parameters.isPValueMode() ? siteModel.getPValueCutoff().getPvalue( altScore ) : 1;
                
                int bestRefPos = -1;
                double bestScore = -Double.MAX_VALUE;
                for( Integer refPos : data.getAlt2Ref().mapInterval( new Interval( altPos, altPos + siteModel.getLength() - 1 ) ) )
                    if(referenceScores[refPos] > bestScore)
                    {
                        bestRefPos = refPos;
                        bestScore = referenceScores[refPos];
                    }

                double refScore;
                double refPValue;

                if(bestRefPos == -1)//new site on alternative, but we can not place it on reference
                {
                    refScore = 0;//TODO: model.getMinScore();
                    refPValue = 1;
                }
                else
                {
                    refScore = referenceScores[bestRefPos];
                    refPValue = parameters.isPValueMode() ? siteModel.getPValueCutoff().getPvalue( refScore ) : 1;
                }
                
                double scoreDiff = altScore - refScore;
                double pValueLogFC = ( -Math.log10( altPValue ) ) - ( -Math.log10( refPValue ) );
                double diff = parameters.isPValueMode() ? pValueLogFC : scoreDiff;
                
                if( diff >= maxDiff )
                {
                    maxDiff = diff;
                    List<Variation> intersectedVariations = new ArrayList<>();
                    Variation[] invVariations = data.getInvertedVariations();
                    for( int i = 0; i < invVariations.length; i++ )
                    {
                        if( invVariations[i].intersects( new Interval( altPos, altPos + siteModel.getLength() - 1 ) ) )
                            intersectedVariations.add( data.getVariations()[i] );//variations[i] corresponds to invVariations[i]
                            
                    }
                    
                    best = new GTEXSiteMutation( type, siteModel,
                            data.getReference(), bestRefPos, refScore, refPValue, 
                            data.getAlternative(), altPos, altScore, altPValue,
                            scoreDiff, pValueLogFC,
                            data.getRef2Alt(), data.getAlt2Ref(),
                            intersectedVariations.toArray( new Variation[0] ) );
                }
            }
        if(best != null)
            siteMutationFound( best );
    }

    private void findBestAltSite(double[] referenceScores, double[] alternativeScores)
    {
        int bestAltPos = -1;
        double bestAltScore = -Double.MAX_VALUE;
        for( int altPos = altFrom; altPos <= altTo; altPos++ )
        {
            if(alternativeScores[altPos] > bestAltScore)
            {
                bestAltPos = altPos;
                bestAltScore = alternativeScores[altPos]; 
            }
        }
        
        double altPValue = parameters.isPValueMode() ? siteModel.getPValueCutoff().getPvalue( bestAltScore ) : 1;
        
        int bestRefPos = -1;
        double bestScore = -Double.MAX_VALUE;
        for( Integer refPos : data.getAlt2Ref().mapInterval( new Interval( bestAltPos, bestAltPos + siteModel.getLength() - 1 ) ) )
            if(referenceScores[refPos] > bestScore)
            {
                bestRefPos = refPos;
                bestScore = referenceScores[refPos];
            }

        double refScore;
        double refPValue;

        if(bestRefPos == -1)//new site on alternative, but we can not place it on reference
        {
            refScore = Double.NEGATIVE_INFINITY;//TODO: model.getMinScore();
            refPValue = 1;
        }
        else
        {
            refScore = referenceScores[bestRefPos];
            refPValue = parameters.isPValueMode() ? siteModel.getPValueCutoff().getPvalue( refScore ) : 1;
        }
        
        double scoreDiff = bestAltScore - refScore;
        double pValueLogFC = ( -Math.log10( altPValue ) ) - ( -Math.log10( refPValue ) );
        
        List<Variation> intersectedVariations = new ArrayList<>();
        Variation[] invVariations = data.getInvertedVariations();
        for( int i = 0; i < invVariations.length; i++ )
        {
            if( invVariations[i].intersects( new Interval( bestAltPos, bestAltPos + siteModel.getLength() - 1 ) ) )
                intersectedVariations.add( data.getVariations()[i] );//variations[i] corresponds to invVariations[i]

        }
            
        SiteMutation siteMutation = new GTEXSiteMutation( "BEST_ALT", siteModel,
                data.getReference(), bestRefPos, refScore, refPValue,
                data.getAlternative(), bestAltPos, bestAltScore, altPValue,
                scoreDiff, pValueLogFC,
                data.getRef2Alt(), data.getAlt2Ref(),
                intersectedVariations.toArray( new Variation[0] ) );
        siteMutationFound( siteMutation );
    }

    private void findBestRefSite(double[] referenceScores, double[] alternativeScores)
    {
        int bestRefPos = -1;
        double bestRefScore = -Double.MAX_VALUE;
        for( int refPos = refFrom; refPos <= refTo; refPos++ )
        {
            if(referenceScores[refPos] > bestRefScore)
            {
                bestRefPos = refPos;
                bestRefScore = referenceScores[refPos]; 
            }
        }
        double refPValue = parameters.isPValueMode() ? siteModel.getPValueCutoff().getPvalue( bestRefScore ) : 1;
        
        int bestAltPos = -1;
        double bestAltScore = -Double.MAX_VALUE;
        for( Integer altPos : data.getRef2Alt().mapInterval( new Interval( bestRefPos, bestRefPos + siteModel.getLength() - 1 ) ) )
            if(alternativeScores[altPos] > bestAltScore)
            {
                bestAltPos = altPos;
                bestAltScore = alternativeScores[altPos];
            }
        
        
        double altPValue;
        if(bestAltPos == -1)//complete deletion of site
        {
            bestAltScore  = Double.NEGATIVE_INFINITY;//TODO: model.getMinScore()
            altPValue = 1;
        }
        else
        {
            altPValue = parameters.isPValueMode() ? siteModel.getPValueCutoff().getPvalue( bestAltScore ) : 1;
        }
        
        double scoreDiff = bestRefScore - bestAltScore;
        double pValueLogFC = ( -Math.log10( refPValue ) ) - ( -Math.log10( altPValue ) );
        
        List<Variation> intersectedVariations = new ArrayList<>();
        Variation[] variations = data.getVariations();
        for( int i = 0; i < variations.length; i++ )
        {
            if( variations[i].intersects( new Interval( bestRefPos, bestRefPos + siteModel.getLength() - 1 ) ) )
                intersectedVariations.add( variations[i] );
        }
        SiteMutation siteMutation = new GTEXSiteMutation( "BEST_REF", siteModel,
                data.getReference(), bestRefPos, bestRefScore, refPValue,
                data.getAlternative(), bestAltPos, bestAltScore, altPValue,
                -scoreDiff, -pValueLogFC,//site loss, scores should be negative
                data.getRef2Alt(), data.getAlt2Ref(),
                intersectedVariations.toArray( new Variation[0] ) );
        siteMutationFound( siteMutation );
    }
}
