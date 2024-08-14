package ru.biosoft.bsa.analysis;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;
import java.util.logging.Level;

import com.developmentontheedge.beans.DynamicProperty;
import com.developmentontheedge.beans.DynamicPropertySet;

import biouml.standard.type.Gene;
import ru.biosoft.access.DataCollectionUtils;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataCollectionConfigConstants;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.analysiscore.AnalysisMethodSupport;
import ru.biosoft.bsa.Sequence;
import ru.biosoft.bsa.Site;
import ru.biosoft.bsa.SiteImpl;
import ru.biosoft.bsa.SiteType;
import ru.biosoft.bsa.SqlTrack;
import ru.biosoft.bsa.Track;
import ru.biosoft.bsa.TrackUtils;
import ru.biosoft.bsa.WithSite;
import ru.biosoft.bsa.WritableTrack;
import ru.biosoft.exception.ExceptionRegistry;
import ru.biosoft.plugins.javascript.JavaScriptHostObjectBase;
import ru.biosoft.table.ColumnModel;
import ru.biosoft.table.RowDataElement;
import ru.biosoft.table.TableColumn;
import ru.biosoft.table.TableDataCollection;
import ru.biosoft.util.Pair;

public abstract class ExtractPromoters<T extends ExtractPromotersParameters> extends AnalysisMethodSupport<T>
{
    public ExtractPromoters(DataCollection<?> origin, String name, Class<? extends JavaScriptHostObjectBase> jsClass, T parameters)
    {
        super(origin, name, jsClass, parameters);
    }

    public ExtractPromoters(DataCollection<?> origin, String name, T parameters)
    {
        super(origin, name, parameters);
    }

    protected abstract DataCollection<?> getEnsemblDataCollection();

    protected String getLabelProperty()
    {
        return null;
    }

    @Override
    public Track justAnalyzeAndPut() throws Exception
    {
        int from = parameters.getFrom();
        int to = parameters.getTo();
        if(to < from)
        {
            int tmp = from;
            from = to;
            to = tmp;
        }
        DataCollection<?> outputCollection = parameters.getDestPath().optParentCollection();
        String outputName = parameters.getDestPath().getName();
        Properties properties = new Properties();
        properties.setProperty( DataCollectionConfigConstants.NAME_PROPERTY, outputName );
        properties.setProperty( DataCollectionUtils.SPECIES_PROPERTY, parameters.getSpecies().getLatinName() );
        DataElementPath ensemblPath = TrackUtils.getEnsemblPath(parameters.getSpecies(), parameters.getDestPath());
        properties.setProperty( Track.SEQUENCES_COLLECTION_PROPERTY, TrackUtils.getPrimarySequencesPath( ensemblPath ).toString() );
        String labelProperty = getLabelProperty();
        if(labelProperty != null)
        {
            properties.setProperty( SqlTrack.LABEL_PROPERTY, labelProperty );
        }
        parameters.getDestPath().remove();
        WritableTrack result = TrackUtils.createTrack(outputCollection, properties);
        try
        {
            getPromoters(ensemblPath, parameters.getSourcePath().getDataCollection(), from, to, result);
        }
        catch(Exception e)
        {
            parameters.getDestPath().remove();
            throw e;
        }
        if(jobControl.isStopped())
        {
            parameters.getDestPath().remove();
            return null;
        }
        parameters.getDestPath().save(result);
        return result;
    }

    protected void getPromoters(DataElementPath ensemblPath, DataCollection<?> source, int from, int to, WritableTrack result) throws Exception
    {
        log.info("Matching accessions...");
        DataCollection<?> ensemblCollection = getEnsemblDataCollection();

        jobControl.pushProgress( 0, 40 );
        List<String> ensemblIds = filterOverlaps( source, ensemblCollection );
        jobControl.popProgress();

        log.info("Fetching positions & creating track...");

        int i=0;
        int matched=0;
        for(String ensGene: ensemblIds)
        {
            jobControl.setPreparedness((i++)*60/ensemblIds.size()+40);
            if(jobControl.isStopped()) return;
            try
            {
                DataElement ensemblObject = ensemblCollection.get( ensGene );
                if( ensemblObject == null )
                    continue;
                Site promoter = getPromoterWindowBySite( ensemblPath, ((WithSite)ensemblObject).getSite(), from, to );
                if( promoter == null )
                    continue;
                initPromoterFromEnsemblElement( promoter, ensemblObject );
                initPromoterFromSourceElement( promoter, source.get( ensGene ) );
                result.addSite( promoter );
                matched++;
            }
            catch( Exception e )
            {
                log.log(Level.SEVERE, "While processing "+ensGene+": "+ExceptionRegistry.log(e));
                continue;
            }
        }
        result.finalizeAddition();
        jobControl.setPreparedness( 100 );
        log.info("IDs matched: "+matched+"/"+ensemblIds.size());

    }

    private List<String> filterOverlaps(DataCollection<?> source, final DataCollection<?> ensemblCollection)
            throws Exception
    {
        String mode = parameters.getOverlapMergingMode();
        if( mode.equals( ExtractPromotersParameters.MODE_DO_NOT_MERGE_OVERLAPPING ) )
            return source.getNameList();

        log.info("Filtering close transcription start sites...");

        jobControl.pushProgress( 0, 50 );
        final Map<String, List<Pair<Integer, String>>> tssByChr = new HashMap<>();
        jobControl.forCollection( source.getNameList(), ensGene -> {
            WithSite ensemblObject;
            try
            {
                ensemblObject = (WithSite) ensemblCollection.get(ensGene);
            }
            catch( Exception e )
            {
                throw ExceptionRegistry.translateException( e );
            }
            if( ensemblObject == null )
                return true;
            Site s = ensemblObject.getSite();
            String chr = s.getOriginalSequence().getName();
            int tss = s.getStart();

            List<Pair<Integer, String>> onChr = tssByChr.get( chr );
            if( onChr == null )
                tssByChr.put( chr, onChr = new ArrayList<>() );
            onChr.add( new Pair<>( tss, ensGene ) );
            return true;
        } );
        jobControl.popProgress();

        jobControl.pushProgress( 50, 100 );
        List<String> filteredIds = new ArrayList<>();
        for( List<Pair<Integer, String>> onChr : tssByChr.values() )
        {
            if( onChr.isEmpty() )
                continue;
            Collections.sort( onChr, Comparator.comparingInt( Pair::getFirst ) );

            List<Pair<Integer, String>> cluster = new ArrayList<>();

            Iterator<Pair<Integer, String>> it = onChr.iterator();
            Pair<Integer, String> cur = it.next();
            cluster.add( cur );
            int clusterEnd = cur.getFirst();
            while( it.hasNext() )
            {
                cur = it.next();
                if( cur.getFirst() > clusterEnd + parameters.getMinDistance() )
                {
                    filteredIds.addAll( selectTSS(cluster, (TableDataCollection)source) );
                    cluster.clear();
                }
                cluster.add( cur );
                clusterEnd = cur.getFirst();
            }
            filteredIds.addAll( selectTSS(cluster, (TableDataCollection)source) );
        }
        jobControl.popProgress();

        log.info("Done filtering, " + (source.getSize() - filteredIds.size()) + " objects removed");

        return filteredIds;
    }

    private List<String> selectTSS(List<Pair<Integer, String>> cluster, TableDataCollection source) throws Exception
    {
        if(cluster.size() == 1)
            return Collections.singletonList( cluster.get( 0 ).getSecond() );

        List<String> result = new ArrayList<>();
        String mode = parameters.getOverlapMergingMode();
        String leadingColumn = parameters.getLeadingColumn();
        while(!cluster.isEmpty())
        {
            Iterator<Pair<Integer, String>> it = cluster.iterator();
            Pair<Integer, String> best = it.next();
            double bestValue = ((Number)source.get( best.getSecond() ).getValue( leadingColumn )).doubleValue();
            while(it.hasNext())
            {
                Pair<Integer, String> cur = it.next();
                double curValue = ((Number)source.get( cur.getSecond() ).getValue( leadingColumn )).doubleValue();
                if( ( mode.equals( ExtractPromotersParameters.MODE_SELECT_ONE_MAX ) && curValue > bestValue )
                 || ( mode.equals( ExtractPromotersParameters.MODE_SELECT_ONE_MIN ) && curValue < bestValue )
                 || ( mode.equals( ExtractPromotersParameters.MODE_SELECT_ONE_EXTREME ) && Math.abs( curValue ) > Math.abs( bestValue ) ) )
                {
                    bestValue = curValue;
                    best = cur;
                }
            }

            result.add( best.getSecond() );

            List<Pair<Integer, String>> filtered = new ArrayList<>();
            for(Pair<Integer, String> tss : cluster)
            {
                if( tss == best || Math.abs(tss.getFirst() - best.getFirst()) < parameters.getMinDistance() )
                    continue;
                filtered.add( tss );
            }
            cluster = filtered;
        }
        return result;
    }

    protected Site getPromoterWindowBySite(DataElementPath ensPath, Site geneSite, int from, int to)
    {
        int geneFrom = geneSite.getFrom(), geneTo = geneSite.getTo();
        int strand = geneSite.getStrand();

        Sequence seq = geneSite.getOriginalSequence();

        if(from>0) from--;
        if(to>0) to--;

        int siteStart = strand == Site.STRAND_PLUS?geneFrom+from:geneTo-to;
        int siteEnd = strand == Site.STRAND_PLUS?geneFrom+to:geneTo-from;
        if(siteEnd <= seq.getStart() || siteStart >= seq.getStart()+seq.getLength()) return null;
        if(siteStart < seq.getStart()) siteStart = seq.getStart();
        if(siteEnd >= seq.getStart()+seq.getLength()) siteEnd = seq.getStart()+seq.getLength()-1;
        int siteLength = siteEnd-siteStart+1;
        int siteFrom = strand == Site.STRAND_PLUS?siteStart:siteEnd;

        return new SiteImpl( null, seq.getName(), SiteType.TYPE_MISC_FEATURE, Site.BASIS_USER, siteFrom, siteLength, strand, seq );
    }

    protected void initPromoterFromEnsemblElement(Site promoter, DataElement ensemblElement)
    {

    }

    protected void initPromoterFromSourceElement(Site promoter, DataElement sourceElement)
    {
        if(sourceElement instanceof RowDataElement)
        {
            RowDataElement rde = (RowDataElement)sourceElement;
            TableDataCollection table = rde.getOrigin();

            DynamicPropertySet promoterDPS = promoter.getProperties();
            ColumnModel tableCM = table.getColumnModel();
            initUniqueNames( tableCM, promoterDPS );

            for( TableColumn col : tableCM )
            {
                if(!col.getType().isNumeric() && col.getValueClass() != String.class)
                    continue;
                String name = col.getName();
                DynamicProperty property = rde.getProperty( col.getName() );
                String newName = uniquePropNamesMap.get( name );
                if( newName == null )
                {
                    log.log( Level.WARNING,
                            "Can not find '" + name + "' in map of unique names, table: '" + table.getCompletePath() + "'." );
                    promoterDPS.add( property );
                }
                else
                    promoterDPS.add( new DynamicProperty( newName, property.getType(), property.getValue() ) );
            }
        }
    }

    private Map<String, String> uniquePropNamesMap = null;
    private void initUniqueNames(ColumnModel cm, DynamicPropertySet properties)
    {
        if( uniquePropNamesMap == null )
        {
            //TODO: try to find better solution
            uniquePropNamesMap = new TreeMap<>( String.CASE_INSENSITIVE_ORDER );
            for( DynamicProperty dp : properties )
                uniquePropNamesMap.put( dp.getName(), dp.getName() );

            for( TableColumn col : cm )
            {
                String name = col.getName();
                while( uniquePropNamesMap.containsKey( name ) )
                    name = "_" + name;
                uniquePropNamesMap.put( col.getName(), name );
            }
        }
    }
}
