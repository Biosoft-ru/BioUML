package ru.biosoft.bsa.analysis.maos;

import java.util.ArrayList;
import java.util.List;

import ru.biosoft.bsa.Interval;
import ru.biosoft.bsa.Sequence;
import ru.biosoft.bsa.SiteModel;

public class SiteModelTask
{
    protected SiteModel siteModel;
    protected IntervalData data;
    protected IResultHandler resultHandler;
    protected Parameters parameters;

    public SiteModelTask(SiteModel siteModel, IntervalData data, IResultHandler resultHandler, Parameters parameters)
    {
        this.siteModel = siteModel;
        this.data = data;
        this.resultHandler = resultHandler;
        this.parameters = parameters;
    }
    
    public void run()
    {
        Sequence reference = data.getReference();
        Sequence alternative = data.getAlternative();
        
        double[] referenceScores = new double[reference.getStart() + reference.getLength() - siteModel.getLength() + 1];
        double[] alternativeScores = new double[alternative.getStart() + alternative.getLength() - siteModel.getLength() + 1];
        for( int pos = reference.getStart(); pos < referenceScores.length; pos++ )
            referenceScores[pos] = siteModel.getScore( reference, pos );
        for( int pos = alternative.getStart(); pos < alternativeScores.length; pos++ )
            alternativeScores[pos] = siteModel.getScore( alternative, pos );

        for( int refPos = reference.getStart(); refPos < referenceScores.length; refPos++ )
            if( referenceScores[refPos] > siteModel.getThreshold() )
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
                if( diff >= parameters.getScoreDiff() )
                {
                    List<Variation> intersectedVariations = new ArrayList<>();
                    Variation[] variations = data.getVariations();
                    for( int i = 0; i < variations.length; i++ )
                    {
                        if( variations[i].intersects( new Interval( refPos, refPos + siteModel.getLength() - 1 ) ) )
                            intersectedVariations.add( variations[i] );
                            
                    }
                    SiteMutation siteMutation = new SiteMutation( siteModel,
                            reference, refPos, refScore, refPValue,
                            alternative, bestAltPos, altScore, altPValue,
                            -scoreDiff, -pValueLogFC,//site loss, scores should be negative
                            data.getRef2Alt(), data.getAlt2Ref(),
                            intersectedVariations.toArray( new Variation[0] ) );
                    siteMutationFound( siteMutation );
                }
            }
        
        
        for( int altPos = alternative.getStart(); altPos < alternativeScores.length; altPos++ )
            if( alternativeScores[altPos] > siteModel.getThreshold() )
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
                
                if( diff >= parameters.getScoreDiff() )
                {
                    List<Variation> intersectedVariations = new ArrayList<>();
                    Variation[] invVariations = data.getInvertedVariations();
                    for( int i = 0; i < invVariations.length; i++ )
                    {
                        if( invVariations[i].intersects( new Interval( altPos, altPos + siteModel.getLength() - 1 ) ) )
                            intersectedVariations.add( data.getVariations()[i] );//variations[i] corresponds to invVariations[i]
                            
                    }
                    
                    SiteMutation siteMutation = new SiteMutation( siteModel,
                            reference, bestRefPos, refScore, refPValue,
                            alternative, altPos, altScore, altPValue,
                            scoreDiff, pValueLogFC,
                            data.getRef2Alt(), data.getAlt2Ref(),
                            intersectedVariations.toArray( new Variation[0] ) );
                    siteMutationFound( siteMutation );
                }
            }
    }

    protected void siteMutationFound(SiteMutation siteMutation)
    {
        Interval interval = siteMutation.getSiteInterval();
        TSSCollection genes = data.getGenes();
        if(interval == null)
            siteMutation.genes = new TSSCollection( genes.upstream, genes.downstream );
        else
            siteMutation.genes = genes.subset( interval );
        resultHandler.siteMutationEvent(siteMutation);
    }
}
