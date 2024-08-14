package biouml.plugins.ensembl.analysis.mutationeffect;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.bsa.AnnotatedSequence;
import ru.biosoft.bsa.Interval;
import ru.biosoft.bsa.Sequence;
import ru.biosoft.bsa.Site;
import ru.biosoft.bsa.analysis.maos.ChrTask;
import ru.biosoft.bsa.analysis.maos.IResultHandler;
import ru.biosoft.bsa.analysis.maos.IntervalData;
import ru.biosoft.bsa.analysis.maos.Parameters;
import ru.biosoft.bsa.analysis.maos.TSSCollection;
import ru.biosoft.bsa.analysis.maos.Variation;
import ru.biosoft.exception.ExceptionRegistry;
import ru.biosoft.util.Pair;

public class ChrTaskBySNP extends ChrTask
{
    List<Site> sites;
    Set<String> invertedVariations = new HashSet<>();

    public ChrTaskBySNP(DataElementPath chrPath, Parameters parameters, IResultHandler resultHandler, Logger analysisLog, List<Site> sites)
    {
        super( chrPath, parameters, resultHandler, analysisLog );
        this.sites = sites;
    }

    public Pair<Variation[], Variation[]> loadVariations(Sequence chr)
    {
        List<Variation> result = new ArrayList<>();
        int skipped = 0;
        List<Variation> inverted = new ArrayList<>();
        if( sites != null )
        {
        for(Site s : sites)
        {
            Variation var = null;
            try {
                var = Variation.createFromSite(s);
                String altString = s.getProperties().getValueAsString( "AltAllele" );
                if( s.getProperties().hasProperty( "RiskAllele" ) )
                {
                    String risk = s.getProperties().getValueAsString( "RiskAllele" );
                    if( altString.equals( risk ) )
                    {
                        result.add( var );
                    }
                    else
                    {
                        String refString = s.getProperties().getValueAsString( "RefAllele" );
                        if( refString.equals( risk ) )
                        {
                            inverted.add( var );
                        }
                        else
                        {
                            analysisLog.log( Level.INFO, "Unknown risk allele '" + risk + "' for site " + s.getName() + ": ref='"
                                    + refString + "', alt='" + altString + "'." );
                        }
                    }

                }
            } catch(IllegalArgumentException e)
            {
                //ignore vcf site parsing errors
                skipped++;
            }
        }
        if(skipped > 0)
            analysisLog.warning( skipped + " vcf sites were skipped" );
    }
    return new Pair<>( result.toArray( new Variation[0] ), inverted.toArray( new Variation[0] ) );
}

public void run()
{
    try
    {
        Sequence chr = chrPath.getDataElement( AnnotatedSequence.class ).getSequence();

        Pair<Variation[], Variation[]> allVariations = loadVariations( chr );
        Variation[] variations = allVariations.getFirst();
        Variation[] inverted = allVariations.getSecond();

        if( variations.length == 0 && inverted.length == 0 )
            return;

        genes = TSSCollection.loadPromoters( parameters, chrPath );

        if( parameters.getIndependentVariations() )
        {
            for( Variation v : variations )
            {
                Interval interval = v.grow( maxModelLength - 1 );
                IntervalData data = createIntervalData( interval, chr, new Variation[] {v}, 0 );
                searchAllSiteModels( chr, interval, data );
            }

            for( Variation v : inverted )
            {
                Interval interval = v.grow( maxModelLength - 1 );
                IntervalData data = createIntervalData( interval, chr, new Variation[] {v}, 0 );
                IntervalData invertedData = invertIntervalData( data );
                searchAllSiteModels( chr, interval, invertedData );
            }
        }
        else
        {
            variations = removeOverlaps( variations );
            Interval[] intervals = getIntervalsForSiteSearch( variations, chr.getInterval() );

            int variationStart = 0;
            for( Interval interval : intervals )
            {
                IntervalData data = createIntervalData( interval, chr, variations, variationStart );

                searchAllSiteModels( chr, interval, data );

                variationStart += data.getVariations().length;
            }

            inverted = removeOverlaps( inverted );
            Interval[] intervals2 = getIntervalsForSiteSearch( inverted, chr.getInterval() );

            variationStart = 0;
            for( Interval interval : intervals2 )
            {
                IntervalData data = createIntervalData( interval, chr, inverted, variationStart );
                IntervalData invertedData = invertIntervalData( data );

                searchAllSiteModels( chr, interval, invertedData );

                variationStart += invertedData.getVariations().length;
            }
        }
    }
    catch( Exception e )
    {
        throw ExceptionRegistry.translateException( e );
    }
}

//Switch alt and ref for data where risk column is reference allele
private IntervalData invertIntervalData(IntervalData original)
{
    IntervalDataBySNP data = new IntervalDataBySNP( original.getAlternative(), original.getReference(), original.getInvertedVariations(),
            original.getVariations(), original.getAlt2Ref(), original.getRef2Alt(), original.getGenes() );
    return data;
}

}
