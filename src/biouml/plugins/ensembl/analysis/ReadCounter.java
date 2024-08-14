package biouml.plugins.ensembl.analysis;

import java.util.NavigableMap;
import java.util.TreeMap;
import java.util.stream.Collectors;

import com.developmentontheedge.beans.annot.PropertyDescription;
import com.developmentontheedge.beans.annot.PropertyName;

import biouml.plugins.ensembl.access.EnsemblDatabase;
import biouml.plugins.ensembl.access.EnsemblDatabaseSelector;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.core.ClassIcon;
import ru.biosoft.analysiscore.AbstractAnalysisParameters;
import ru.biosoft.analysiscore.AnalysisMethodSupport;
import ru.biosoft.bsa.AnnotatedSequence;
import ru.biosoft.bsa.BAMTrack;
import ru.biosoft.bsa.Interval;
import ru.biosoft.bsa.Sequence;
import ru.biosoft.bsa.Site;
import ru.biosoft.bsa.Track;
import ru.biosoft.bsa.TrackUtils;
import ru.biosoft.table.TableDataCollection;
import ru.biosoft.table.TableDataCollectionUtils;
import ru.biosoft.util.bean.BeanInfoEx2;

@ClassIcon("resources/ReadCounter.gif")
public class ReadCounter extends AnalysisMethodSupport<ReadCounter.Parameters>
{
    public ReadCounter(DataCollection<?> origin, String name)
    {
        super( origin, name, new Parameters() );
    }

    @Override
    public Object justAnalyzeAndPut() throws Exception
    {
        TableDataCollection genesTable = null;
        if( parameters.getGenesTablePath() != null )
            genesTable = parameters.getGenesTablePath().getDataElement(TableDataCollection.class);

        TableDataCollection result = TableDataCollectionUtils.createTableDataCollection( parameters.getOutputTablePath() );
        result.getColumnModel().addColumn( "Read count", Integer.class );

        BAMTrack track = parameters.getTrackPath().getDataElement(BAMTrack.class);
        DataElementPath chromosomesPath = track.getGenomeSelector().getSequenceCollectionPath();
        Track genes = parameters.getGeneTrack().getDataElement(Track.class);
        if(chromosomesPath == null)
        {
            chromosomesPath = TrackUtils.getTrackSequencesPath(genes);
        }
        DataCollection<AnnotatedSequence> chromosomes = chromosomesPath.getDataCollection(AnnotatedSequence.class);

        int i = 0;
        for( AnnotatedSequence chr : chromosomes )
        {
            Sequence seq = chr.getSequence();
            String chrPath = DataElementPath.create( chr ).toString();
            for( Site gene : genes.getSites( chrPath, seq.getStart(), seq.getLength() + seq.getStart() ) )
            {
                String geneId = gene.getProperties().getValueAsString( "id" );
                if( genesTable != null && !genesTable.contains( geneId ) )
                    continue;

                NavigableMap<Integer, Long> fivePrimeCoords = track.getSites(chrPath, gene.getFrom(), gene.getTo())
                        .stream().collect( Collectors.groupingBy( Site::getStart, TreeMap::new, Collectors.counting() ) );

                long readCount = 0;
                if( !fivePrimeCoords.isEmpty() )
                    for( String exon : gene.getProperties().getValueAsString( "exons" ).split( ";" ) )
                    {
                        Interval exonInterval = new Interval( exon ).translateFromSite( gene );
                        for( Long count : fivePrimeCoords.subMap( exonInterval.getFrom(), exonInterval.getTo() + 1 ).values() )
                            readCount += count;
                    }
                TableDataCollectionUtils.addRow( result, geneId, new Object[] {readCount}, true );
            }
            jobControl.setPreparedness( ( ++i ) / chromosomes.getSize() );
        }

        result.finalizeAddition();
        return result;
    }

    @SuppressWarnings ( "serial" )
    public static class Parameters extends AbstractAnalysisParameters
    {
        private DataElementPath trackPath, genesTablePath, outputTablePath, geneTrack;
        private EnsemblDatabase ensemblDB = EnsemblDatabaseSelector.getDefaultEnsembl();

        @PropertyName("Track with reads")
        @PropertyDescription("Track with reads")
        public DataElementPath getTrackPath()
        {
            return trackPath;
        }

        public void setTrackPath(DataElementPath trackPath)
        {
            Object oldValue = this.trackPath;
            this.trackPath = trackPath;
            firePropertyChange( "trackPath", oldValue, trackPath );
        }

        @PropertyName("Genes")
        @PropertyDescription("Table with genes to map reads")
        public DataElementPath getGenesTablePath()
        {
            return genesTablePath;
        }

        public void setGenesTablePath(DataElementPath genesTablePath)
        {
            Object oldValue = this.genesTablePath;
            this.genesTablePath = genesTablePath;
            firePropertyChange( "genesTablePath", oldValue, genesTablePath );
        }

        @PropertyName("Output table")
        @PropertyDescription("Output table with genes and read counts")
        public DataElementPath getOutputTablePath()
        {
            return outputTablePath;
        }

        public void setOutputTablePath(DataElementPath outputTablePath)
        {
            Object oldValue = this.outputTablePath;
            this.outputTablePath = outputTablePath;
            firePropertyChange( "outputTablePath", oldValue, outputTablePath );
        }

        @PropertyName ( "Ensembl version" )
        @PropertyDescription ( "Ensembl version to take gene track from" )
        public EnsemblDatabase getEnsemblDB()
        {
            return ensemblDB;
        }

        public void setEnsemblDB(EnsemblDatabase ensemblDB)
        {
            EnsemblDatabase oldValue = this.ensemblDB;
            this.ensemblDB = ensemblDB;
            firePropertyChange( "ensemblDB", oldValue, ensemblDB );
            setGeneTrack( ensemblDB.getGenesTrack().getCompletePath() );
        }

        @PropertyName("Gene track")
        @PropertyDescription("Track with gene locations")
        public DataElementPath getGeneTrack()
        {
            return geneTrack;
        }

        public void setGeneTrack(DataElementPath geneTrack)
        {
            Object oldValue = this.geneTrack;
            this.geneTrack = geneTrack;
            firePropertyChange( "geneTrack", oldValue, geneTrack );
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
            property( "trackPath" ).inputElement( BAMTrack.class ).add();
            property( "genesTablePath" ).inputElement( TableDataCollection.class ).canBeNull().add();
            //            property( "geneTrack" ).inputElement( Track.class ).add();
            add( "ensemblDB" );
            property( "outputTablePath" ).outputElement( TableDataCollection.class ).add();
        }
    }
}
