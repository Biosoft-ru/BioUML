package biouml.plugins.gtrd.analysis;


import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang.mutable.MutableInt;

import com.developmentontheedge.beans.annot.PropertyDescription;
import com.developmentontheedge.beans.annot.PropertyName;

import biouml.plugins.ensembl.access.EnsemblDatabase;
import biouml.plugins.ensembl.access.EnsemblDatabaseSelector;
import biouml.standard.type.Species;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.DataCollectionUtils;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.biohub.ReferenceType;
import ru.biosoft.access.biohub.ReferenceTypeRegistry;
import ru.biosoft.analysiscore.AbstractAnalysisParameters;
import ru.biosoft.analysiscore.AnalysisMethodSupport;
import ru.biosoft.bsa.AnnotatedSequence;
import ru.biosoft.bsa.Interval;
import ru.biosoft.bsa.IntervalMap;
import ru.biosoft.bsa.Sequence;
import ru.biosoft.bsa.Site;
import ru.biosoft.bsa.StrandType;
import ru.biosoft.bsa.Track;
import ru.biosoft.exception.ExceptionRegistry;
import ru.biosoft.exception.InternalException;
import ru.biosoft.jobcontrol.JobControl;
import ru.biosoft.table.SqlColumnModel;
import ru.biosoft.table.SqlTableDataCollection;
import ru.biosoft.table.TableDataCollection;
import ru.biosoft.table.TableDataCollectionUtils;
import ru.biosoft.util.bean.BeanInfoEx2;

public class FindTargetGenes extends AnalysisMethodSupport<FindTargetGenes.Parameters>
{

    public FindTargetGenes(DataCollection<?> origin, String name)
    {
        super( origin, name, new Parameters() );
    }
    
    @Override
    public Object justAnalyzeAndPut() throws Exception
    {
        final Track targetTrack = getTargetTrack();
        log.info("Reading transcripts...");
        jobControl.pushProgress(0, 10);
        final Map<String, IntervalMap<String>> targetsMap = new HashMap<>();
        jobControl.forCollection(parameters.getEnsembl().getPrimarySequencesPath().getChildren(), chrPath -> {
            try
            {
                Sequence sequence = chrPath.getDataElement( AnnotatedSequence.class ).getSequence();
                DataCollection<Site> targetSites = targetTrack.getSites( chrPath.toString(), sequence.getStart(),
                        sequence.getLength() );
                if( targetSites != null && targetSites.getSize() > 0 )
                {
                    IntervalMap<String> targetIntervals = new IntervalMap<>();
                    for( Site site : targetSites )
                    {
                        Interval targetInterval = createTargetInterval(site);
                        String targetId = site.getProperties().getValueAsString( "id" );
                        targetIntervals.add( targetInterval.getFrom(), targetInterval.getTo(), targetId );
                    }
                    targetsMap.put( chrPath.getName(), targetIntervals );
                }
            }
            catch( Exception e )
            {
                throw ExceptionRegistry.translateException( e );
            }
            return true;
        });
        jobControl.popProgress();
        if(jobControl.getStatus() == JobControl.TERMINATED_BY_REQUEST) return null;

        Track input = parameters.getSourcePath().getDataElement(Track.class);

        log.info("Creating gene table...");
        jobControl.pushProgress(10, 70);
        
        Map<String, MutableInt> counts = new HashMap<>();
        jobControl.forCollection( DataCollectionUtils.asCollection( input.getAllSites(), Site.class ), site -> {
            IntervalMap<String> chromosomeIntervals = targetsMap.get(site.getOriginalSequence().getName());
            if(chromosomeIntervals != null)
            {
                for(String targetId : chromosomeIntervals.getIntervals(site.getFrom(), site.getTo()) )
                {
                    counts
                        .computeIfAbsent( targetId, k->new MutableInt( 0 ) )
                        .increment();
                }
            }
            return true;
        } );
        
        final SqlTableDataCollection outputTable = (SqlTableDataCollection)TableDataCollectionUtils.createTableDataCollection( parameters.getResPath() );
        SqlColumnModel cm = outputTable.getColumnModel();
        cm.addColumn( "SiteCount", Integer.class );        
        for(Map.Entry<String, MutableInt> e : counts.entrySet())
        {
            String transcriptId = e.getKey();
            int count = e.getValue().intValue();
            TableDataCollectionUtils.addRow( outputTable, transcriptId, new Object[]{count}, true );
        }
        outputTable.finalizeAddition();
        parameters.getResPath().save( outputTable );
        
        TableDataCollectionUtils.setSortOrder( outputTable, "SiteCount", false );
        setReferenceType( outputTable );
        
        jobControl.popProgress();
        return outputTable;
    }

    private void setReferenceType(final SqlTableDataCollection outputTable) throws InternalException, IllegalArgumentException
    {
        ReferenceType typeClass;
        if( parameters.getMapTo().equals( Parameters.MAP_TO_GENES ) )
            typeClass = ReferenceTypeRegistry.getReferenceType( "Genes: Ensembl" );
        else if(parameters.getMapTo().equals( Parameters.MAP_TO_TRANSCRIPTS ))
            typeClass = ReferenceTypeRegistry.getReferenceType( "Transcripts: Ensembl" );
        else
            throw new IllegalArgumentException();
        ReferenceTypeRegistry.setCollectionReferenceType( outputTable, typeClass  );
    }

    private Track getTargetTrack()
    {
        if(parameters.getMapTo().equals( Parameters.MAP_TO_TRANSCRIPTS ))
            return parameters.getEnsembl().getTranscriptsTrack();
        else if(parameters.getMapTo().equals( Parameters.MAP_TO_GENES ))
            return parameters.getEnsembl().getGenesTrack();
        else
            throw new IllegalArgumentException();
    }

    private Interval createTargetInterval(Site transcript)
    {
        if(parameters.getIntervalType().equals( Parameters.INTERVAL_TYPE_PROMOTER ))
        {
            Interval result;
            if( transcript.getStrand() == StrandType.STRAND_PLUS )
                result = new Interval(
                        transcript.getFrom() + parameters.getFrom(),
                        transcript.getFrom() + parameters.getTo() );
            else
                result = new Interval(
                        transcript.getTo() - parameters.getTo(),
                        transcript.getTo() - parameters.getFrom() );
            return result;
        } else if(parameters.getIntervalType().equals( Parameters.INTERVAL_TYPE_WHOLE ))
        {
            Interval result;
            if( transcript.getStrand() == StrandType.STRAND_PLUS )
                result = new Interval(
                        transcript.getFrom() + parameters.getFrom(),
                        transcript.getTo() + parameters.getTo() );
            else
                result = new Interval(
                        transcript.getFrom() - parameters.getTo(),
                        transcript.getTo() - parameters.getFrom() );
            return result;

        }else
            throw new IllegalArgumentException();
    }

    public static class Parameters extends AbstractAnalysisParameters
    {
        private DataElementPath sourcePath;
        private EnsemblDatabase ensembl = EnsemblDatabaseSelector.getDefaultEnsembl();
        
        public static final String MAP_TO_GENES = "Genes";
        public static final String MAP_TO_TRANSCRIPTS = "Transcripts";
        public static final String[] MAP_TO_VALUES = new String[] {MAP_TO_GENES, MAP_TO_TRANSCRIPTS};
        private String mapTo = MAP_TO_GENES;
        
        
        public static final String INTERVAL_TYPE_WHOLE ="Whole";
        public static final String INTERVAL_TYPE_PROMOTER = "Promoter";
        public static final String[] INTERVAL_TYPES = new String[] {INTERVAL_TYPE_WHOLE, INTERVAL_TYPE_PROMOTER};
        private String intervalType = INTERVAL_TYPE_WHOLE;
        
        private int from = -1000;
        private int to = 1000;
        private DataElementPath resPath;
        
        @PropertyName("Input track")
        public DataElementPath getSourcePath()
        {
            return sourcePath;
        }
        public void setSourcePath(DataElementPath sourcePath)
        {
            Object oldValue = this.sourcePath;
            this.sourcePath = sourcePath;
            firePropertyChange( "sourcePath", oldValue, sourcePath );
            
            Species species = Species.getDefaultSpecies(sourcePath == null ? null : sourcePath.optDataCollection());
            setEnsembl( EnsemblDatabaseSelector.getDefaultEnsembl( species ) );
        }
        
        public EnsemblDatabase getEnsembl()
        {
            return ensembl;
        }
        public void setEnsembl(EnsemblDatabase ensembl)
        {
            Object oldValue = this.ensembl;
            this.ensembl = ensembl;
            firePropertyChange( "ensembl", oldValue, ensembl );
        }
        
        @PropertyName("Map to")
        public String getMapTo()
        {
            return mapTo;
        }
        public void setMapTo(String mapTo)
        {
            Object oldValue = this.mapTo;
            this.mapTo = mapTo;
            firePropertyChange( "mapTo", oldValue, mapTo );
        }
        
        @PropertyName("Interval type")
        @PropertyDescription("Type of interval used to intersect with input sites")
        public String getIntervalType()
        {
            return intervalType;
        }
        public void setIntervalType(String intervalType)
        {
            Object oldValue = this.intervalType;
            this.intervalType = intervalType;
            firePropertyChange( "intervalType", oldValue, intervalType );
        }
        
        @PropertyName("Upstream")
        @PropertyDescription("Offset upstream to transcript interval")
        public int getFrom()
        {
            return from;
        }
        public void setFrom(int from)
        {
            int oldValue = this.from;
            this.from = from;
            firePropertyChange( "from", oldValue, from );
        }
        
        @PropertyName("Downstream")
        @PropertyDescription("Offset downstream to transcript interval")
        public int getTo()
        {
            return to;
        }
        public void setTo(int to)
        {
            int oldValue = this.to;
            this.to = to;
            firePropertyChange( "to", oldValue, to );
        }
        
        @PropertyName("Resulting transcripts")
        public DataElementPath getResPath()
        {
            return resPath;
        }
        public void setResPath(DataElementPath resPath)
        {
            Object oldValue = this.resPath;
            this.resPath = resPath;
            firePropertyChange( "resPath", oldValue, resPath );
        }
       
    }
    
    public static class ParametersBeanInfo extends BeanInfoEx2<Parameters>
    {
        public ParametersBeanInfo()
        {
            super( Parameters.class );
        }
        
        @Override
        protected void initProperties() throws Exception
        {
            property( "sourcePath" ).inputElement( Track.class ).add();
            add("ensembl");
            property("mapTo").tags( Parameters.MAP_TO_VALUES ).add();
            property("intervalType").tags( Parameters.INTERVAL_TYPES ).add();
            add("from");
            add("to");
            property("resPath").outputElement( TableDataCollection.class ).add();
            
        }
    }

}
