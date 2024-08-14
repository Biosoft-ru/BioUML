package ru.biosoft.bsa.analysis.maos;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import one.util.streamex.StreamEx;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.bsa.AnnotatedSequence;
import ru.biosoft.bsa.Interval;
import ru.biosoft.bsa.Sequence;
import ru.biosoft.bsa.Site;
import ru.biosoft.bsa.SiteModel;
import ru.biosoft.bsa.SiteModelCollection;
import ru.biosoft.bsa.Track;
import ru.biosoft.bsa.analysis.SequenceAccessor.CachedSequenceRegion;
import ru.biosoft.bsa.analysis.maos.coord_mapping.CoordinateMapping;
import ru.biosoft.bsa.analysis.maos.coord_mapping.InverseMapping;
import ru.biosoft.bsa.analysis.maos.coord_mapping.MappingByVCF;
import ru.biosoft.exception.ExceptionRegistry;

public class ChrTask
{
    protected DataElementPath chrPath;
    protected Parameters parameters;
    protected IResultHandler resultHandler;
    protected Logger analysisLog;
    
    protected Track vcfTrack;
    protected SiteModelCollection siteModels;
    protected int maxModelLength;
    
    protected TSSCollection genes;
    
    public ChrTask(DataElementPath chrPath, Parameters parameters, IResultHandler resultHandler, Logger analysisLog)
    {
        this.chrPath = chrPath;
        this.parameters = parameters;
        this.resultHandler = resultHandler;
        this.analysisLog = analysisLog;
        
        vcfTrack = parameters.getVcfTrackDataElement();
        siteModels = parameters.getSiteModelCollection();
        
        if( siteModels.isEmpty() )
            throw new IllegalArgumentException( "No site models found" );
        for( SiteModel model : siteModels )
            if( model.getLength() > maxModelLength )
                maxModelLength = model.getLength();
    }

    public void run()
    {
        try
        {
            Sequence chr = chrPath.getDataElement( AnnotatedSequence.class ).getSequence();

            DataCollection<Site> sites = vcfTrack.getSites( chrPath.toString(), chr.getStart(), chr.getStart() + chr.getLength() );
            Variation[] variations = loadVariations( sites );

            if( variations.length == 0 )
                return;

            genes = TSSCollection.loadPromoters( parameters, chrPath );
            
            if(parameters.getIndependentVariations())
            {
                for(Variation v : variations)
                {
                    Interval interval = v.grow( maxModelLength-1 );
                    IntervalData data = createIntervalData( interval, chr, new Variation[] {v}, 0 );
                    searchAllSiteModels( chr, interval, data );
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

                    variationStart += data.variations.length;
                }
            }
        }
        catch( Exception e )
        {
            throw ExceptionRegistry.translateException( e );
        }
    }

    public Variation[] loadVariations(DataCollection<Site> sites)
    {
        List<Variation> result = new ArrayList<>();
        int skipped = 0;
        for(Site s : sites)
        {
            Variation var = null;
            try {
                var = Variation.createFromSite(s);
            } catch(IllegalArgumentException e)
            {
                //ignore vcf site parsing errors
                skipped++;
            }
            if(var != null)
                result.add( var );
        }
        if(skipped > 0)
            analysisLog.warning( skipped + " vcf sites were skipped" );
        return result.toArray( new Variation[0] );
    }

    protected IntervalData createIntervalData(Interval interval, Sequence chr, Variation[] variations, int variationStart)
    {
        CachedSequenceRegion reference = new CachedSequenceRegion( chr, interval.getFrom(), interval.getLength(), false );
        
        int nVariations = 0;
        while( variationStart + nVariations < variations.length )
        {
            if( variations[variationStart + nVariations].getFrom() > interval.getTo() )
                break;
            nVariations++;
        }

        Variation[] curVariations = new Variation[nVariations];
        for(int i = 0; i < nVariations; i++)
        {
            Variation var = variations[variationStart + i];
            int relativeFrom = var.getFrom() - interval.getFrom() + reference.getStart();
            Variation relativeVar = new Variation(var.id, var.name, relativeFrom, relativeFrom + var.getLength() - 1, var.ref, var.alt);
            curVariations[i] = relativeVar;
        }
        
        Sequence alternative = Variation.applyVariations( reference, curVariations );
        
        CoordinateMapping ref2alt = new MappingByVCF( reference.getInterval(), curVariations );
        
        Variation[] invVariations = Variation.invertVariations( curVariations );
        CoordinateMapping alt2ref = new InverseMapping( alternative.getInterval(), ref2alt );
     
        TSSCollection geneSubset = genes
                .subset( interval.grow( Math.max( genes.upstream, genes.downstream) ) )
                .translateTo( interval, reference.getStart() );
        
        IntervalData data = new IntervalData( reference, alternative, curVariations, invVariations, ref2alt, alt2ref, geneSubset );
        return data;
    }
    
    
    protected Variation[] removeOverlaps(Variation[] intervals) throws Exception
    {
        if(intervals.length <= 1)
            return intervals;
        
        List<Variation> result = new ArrayList<>();
        result.add( intervals[0] );
        int maxTo = intervals[0].getTo();
        int iMax = 0;
        for(int i = 1 ; i < intervals.length; i++)
        {
            Variation v = intervals[i];
            if(v.getFrom() <= maxTo)
            {
                analysisLog.warning( "Variations should not overlap with each other, but " + intervals[iMax].name + " overlaps " + intervals[i].name + "." );
                analysisLog.warning( "Variation " + intervals[i].name + " will be skipped."  );
            }
            else
                result.add( v );
            if(v.getTo() > maxTo)
            {
                maxTo = v.getTo();
                iMax = i;
            }
        }
        return result.toArray( new Variation[0] );
    }

    protected Interval[] getIntervalsForSiteSearch(Variation[] variations, Interval bounds)
    {
        return StreamEx.of( variations ).map( variation -> variation.grow( maxModelLength ) ).filter( bounds::inside ).sorted()
                .collapse( Interval::intersects, Interval::union ).toArray( Interval[]::new );
    }

    protected void searchAllSiteModels(Sequence chr, Interval interval, IntervalData data)
    {
        IntervalData dataRC = data.getReverseComplement();
        for( SiteModel model : siteModels )
        {
            SiteModelTask task = createSiteModelTask( data, model );
            task.run();
            
            task = createSiteModelTask( dataRC, model );
            task.run();
        }
    }
    
    protected SiteModelTask createSiteModelTask(IntervalData data, SiteModel model)
    {
        return new SiteModelTask( model, data, resultHandler, parameters );
    }

}
