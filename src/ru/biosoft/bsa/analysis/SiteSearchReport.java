package ru.biosoft.bsa.analysis;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.Stream;

import org.apache.commons.lang.StringEscapeUtils;

import com.developmentontheedge.beans.DynamicProperty;

import biouml.model.Module;
import one.util.streamex.EntryStream;
import one.util.streamex.IntStreamEx;
import one.util.streamex.Joining;
import one.util.streamex.StreamEx;
import ru.biosoft.access.CollectionFactoryUtils;
import ru.biosoft.access.DataCollectionUtils;
import ru.biosoft.access.biohub.ReferenceType;
import ru.biosoft.access.biohub.ReferenceTypeRegistry;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.exception.DataElementNotAcceptableException;
import ru.biosoft.bsa.Site;
import ru.biosoft.bsa.SiteModel;
import ru.biosoft.bsa.SqlTrack;
import ru.biosoft.bsa.Track;
import ru.biosoft.bsa.TrackUtils;
import ru.biosoft.bsa.WritableTrack;
import ru.biosoft.bsa.project.Project;
import ru.biosoft.jobcontrol.AbstractJobControl;
import ru.biosoft.jobcontrol.JobControl;
import ru.biosoft.jobcontrol.JobControlException;
import ru.biosoft.table.ColumnModel;
import ru.biosoft.table.RowDataElement;
import ru.biosoft.table.StandardTableDataCollection;
import ru.biosoft.table.StringSet;
import ru.biosoft.table.TableColumn;
import ru.biosoft.table.TableDataCollection;
import ru.biosoft.table.TableDataCollectionUtils;

public class SiteSearchReport extends AbstractJobControl
{
    private static Logger log = Logger.getLogger( SiteSearchReport.class.getName() );
    private final List<String> matrices;
    private TableDataCollection result;
    private final Track sitesTrack;
    private final DataElementPath outputPath;
    private final boolean createPositions;
    private final boolean useOriginalIds;
    private String titleProperty;
    public static final String TRACK_PROPERTY = DataCollectionUtils.PERSISTENT_PROPERTY_PREFIX+"track";
    public static final String YES_TRACK_PROPERTY = DataCollectionUtils.PERSISTENT_PROPERTY_PREFIX+"yesTrack";
    public static final String NO_TRACK_PROPERTY = DataCollectionUtils.PERSISTENT_PROPERTY_PREFIX+"noTrack";

    public SiteSearchReport(DataElementPath outputPath, Track sitesTrack, List<String> matrices, boolean createPositions, boolean useOriginalIds)
    {
        super(log);
        this.outputPath = outputPath;
        this.sitesTrack = sitesTrack;
        this.matrices = matrices;
        this.createPositions = createPositions;
        this.useOriginalIds = useOriginalIds;
    }

    static class SiteInfo<T> implements Comparable<SiteInfo<?>>
    {
        int from, to;
        String sequence;
        T rowId;

        SiteInfo(Site site, T id)
        {
            from = site.getFrom();
            to = site.getTo();
            sequence = site.getOriginalSequence().getName();
            rowId = id;
        }

        @Override
        public int compareTo(SiteInfo<?> s)
        {
            int result = sequence.compareTo(s.sequence);
            if(result != 0) return result;
            return from-s.from;
        }

        public boolean inside(SiteInfo<?> s)
        {
            if(!sequence.equals(s.sequence)) return false;
            return from<=s.from && to>=s.to;
        }

        public static <T> StreamEx<SiteInfo<T>> binSearch(List<SiteInfo<T>> list, Site site) {
            SiteInfo<Object> s = new SiteInfo<>(site, null);
            int idx = Math.abs(Collections.binarySearch(list, s)+1);
            StreamEx<SiteInfo<T>> s1 = IntStreamEx.rangeClosed( idx - 1, 0, -1 )
                .mapToObj( list::get )
                .takeWhile( si -> si.inside(s) );
            StreamEx<SiteInfo<T>> s2 = IntStreamEx.range( idx, list.size() )
                .mapToObj( list::get )
                .takeWhile( si -> si.inside(s) );
            return s1.append( s2 );
        }
    }

    public TableDataCollection getResult()
    {
        return result;
    }

    public void setTitleProperty(String promoterTitle)
    {
        titleProperty = promoterTitle;
    }

    @Override
    public void doRun() throws JobControlException
    {
        try
        {
            setPreparedness(0);
            result = new StandardTableDataCollection(null, outputPath.getName());
            SiteSearchTrackInfo trackInfo;
            Track promotersTrack;
            DataElementPath ensemblPath = null;
            int offset = 0;
            try
            {
                trackInfo = new SiteSearchTrackInfo(sitesTrack);
                promotersTrack = trackInfo.getIntervals();
                offset = trackInfo.getIntervalsOffset();
                if(promotersTrack != null)
                {
                    DataElementPath sequencesPath = TrackUtils.getTrackSequencesPath(promotersTrack);
                    Module module = Module.optModule(sequencesPath.optDataElement());
                    if(module != null && "Ensembl".equals(module.getInfo().getProperty("database")))
                    {
                        ensemblPath = module.getCompletePath();
                    }
                } else
                {
                    promotersTrack = trackInfo.getSequencesDC().cast( Track.class );
                }
            }
            catch( Exception e )
            {
                throw new DataElementNotAcceptableException(e, DataElementPath.create(sitesTrack), "not a valid site search result");
            }
            ColumnModel model = result.getColumnModel();
            model.addColumn("Symbol", String.class);
            TableColumn geneColumn = model.addColumn("Sites view", Project.class);
            model.addColumn("Total count", Integer.class);
            Map<String, Integer> modelOrder = EntryStream.of( matrices ).invert().toMap();
            matrices.forEach( name -> model.addColumn( name, Integer.class ) );
            if(createPositions)
            {
                matrices.forEach( name -> model.addColumn( name+" positions", StringSet.class ) );
            }
            model.addColumn( "Sequence name", String.class ).setHidden( true );
            if(getStatus() == JobControl.TERMINATED_BY_REQUEST) return;
            setPreparedness(1);
            List<SiteInfo<Integer>> promoterList = new ArrayList<>();
            int rowNum = 0;
            for(Site site : promotersTrack.getAllSites())
            {
                String geneName = titleProperty != null ? site.getProperties().getValueAsString( titleProperty )
                        : site.getProperties().getValueAsString( GeneSetToTrack.GENE_NAME_PROPERTY );
                String geneId = useOriginalIds ? site.getName() : site.getProperties().getValueAsString(GeneSetToTrack.GENE_ID_PROPERTY);
                if( geneId == null )
                    geneId = site.getName();

                StreamEx<Object> row = StreamEx.<Object>of(geneName == null?geneId:geneName, null)
                    .append( StreamEx.constant( 0, matrices.size()+1 ) );
                if(createPositions)
                    row = row.append( Stream.generate( StringSet::new ).limit( matrices.size() ) );
                row = row.append( site.getName() );

                TableDataCollectionUtils.addRow(result, geneId, row.toArray());
                promoterList.add(new SiteInfo<>(site, rowNum++));
            }
            if(getStatus() == JobControl.TERMINATED_BY_REQUEST) return;
            setPreparedness(10);
            Collections.sort(promoterList);
            Iterator<Site> iterator = trackInfo.getTrackIterator();
            int sitesCount = trackInfo.getSitesCount();
            int curSite = 0;
            Set<SiteModel> modelCache = new HashSet<>();
            Track track = trackInfo.getTrack();
            DataElementPath trackPath = outputPath.getSiblingPath( result.getName() + " track" );
            WritableTrack resultTrack = SqlTrack.createTrack( trackPath, track );
            while(iterator.hasNext())
            {
                Site site = iterator.next();
                DynamicProperty modelProperty = site.getProperties().getProperty("siteModel");
                curSite++;
                setPreparedness(85*curSite/sitesCount+10);
                if(modelProperty == null || !(modelProperty.getValue() instanceof SiteModel)) continue;
                modelCache.add((SiteModel)modelProperty.getValue());
                String name = ((SiteModel)modelProperty.getValue()).getName();
                if(!modelOrder.containsKey(name)) continue;
                int key = modelOrder.get(name);
                for(SiteInfo<Integer> prom : SiteInfo.binSearch( promoterList, site ))
                {
                    int rowId = prom.rowId;
                    result.setValueAt(rowId, 2, (Integer)result.getValueAt(rowId, 2)+1);
                    result.setValueAt(rowId, key+3, (Integer)result.getValueAt(rowId, key+3)+1);
                    if(createPositions) {
                        ( (StringSet)result.getValueAt( rowId, key + 3 + matrices.size() ) ).add( String.valueOf( site.getFrom() - prom.from
                                + offset ) );
                    }
                }
                resultTrack.addSite( site );
                if(getStatus() == JobControl.TERMINATED_BY_REQUEST) return;
            }
            if(createPositions)
            {
                for(RowDataElement rde : result)
                {
                    StreamEx.of(rde.getValues()).select( StringSet.class )
                        .forEach( set -> {
                            List<String> data = set.stream().sortedByInt( Integer::valueOf ).toList();
                            set.clear();
                            set.addAll( data );
                        } );
                }
            }
            setPreparedness(95);
            resultTrack.finalizeAddition();
            CollectionFactoryUtils.save( resultTrack );
            setPreparedness(98);

            StreamEx<ru.biosoft.access.core.DataElementPath> tracks =
                    ensemblPath == null ? StreamEx.of( DataElementPath.create( resultTrack ) ) :
                        StreamEx.of(ensemblPath.getChildPath("Tracks", "ExtendedGeneTrack"),
                                    DataElementPath.create( resultTrack ) );

            String tracksArray =
                    tracks.map( path -> StringEscapeUtils.escapeJavaScript( path.toString() ) )
                    .map( str -> "data.get('"+str+"', '"+Track.class.getName()+"')" )
                    .joining( ", ", "[", "]" );

            boolean isPromoterNameEsembl = false;
            if( promotersTrack instanceof DataCollection )
            {
                List<String> siteNames = ( (DataCollection<?>)promotersTrack ).getNameList();
                if( !siteNames.isEmpty() )
                {
                    ReferenceType type = ReferenceTypeRegistry.detectReferenceType( siteNames.toArray( new String[0] ) );
                    isPromoterNameEsembl = type.getSource().equals( "Ensembl" );
                }
            }

            String siteName = isPromoterNameEsembl ? "ID" : "Sequence_name";
            geneColumn.setExpression("bsa.createProject(data.get(\""
                    + StringEscapeUtils.escapeJavaScript(DataElementPath.create(promotersTrack).toString())
                    + "/\"+" + siteName + "), " + tracksArray + ")" );

            TableDataCollection clone = result.clone(outputPath.getParentCollection(), result.getName());
            TableDataCollectionUtils.setSortOrder(clone, "Total count", false);
            clone.getInfo().getProperties().setProperty( TRACK_PROPERTY, DataElementPath.create( resultTrack ).toString() );
            ReferenceType geneType = detectGeneType( promotersTrack );
            if( ensemblPath != null && ( geneType.getSource().equals( "Ensembl" ) ) )
                clone.setReferenceType("Genes: Ensembl");
            CollectionFactoryUtils.save( clone );
            result = clone;

            setPreparedness(100);
            resultsAreReady(new Object[] {clone});
        }
        catch( Exception e )
        {
            result = null;
            throw new JobControlException(e);
        }
    }

    private ReferenceType detectGeneType(Track promotersTrack)
    {
        DataCollection<Site> sites = promotersTrack.getAllSites();
        Iterator<Site> sitesIter = sites.iterator();
        List<String> geneIds = new ArrayList<>();
        int i = 0;
        while( sitesIter.hasNext() && i < 10 )
        {
            Site site = sitesIter.next();
            String geneId = site.getProperties().getValueAsString( GeneSetToTrack.GENE_ID_PROPERTY );
            if( geneId != null )
                geneIds.add( geneId );
            i++;
        }
        return ReferenceTypeRegistry.detectReferenceType( geneIds.toArray( new String[0] ) );
    }

    public static DataElementPath generateName(DataCollection<?> parent, List<String> matrices)
    {
        String baseName = StreamEx.of(matrices).collect( Joining.with( "," ).maxChars( 30 ).cutAfterDelimiter() );
        String name = baseName;
        int i=0;
        while(parent.contains(name))
        {
            name = baseName+" ("+(++i)+")";
        }
        return parent.getCompletePath().getChildPath( name );
    }
}
