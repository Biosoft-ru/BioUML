package biouml.plugins.ensembl.analysis;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import com.developmentontheedge.beans.annot.PropertyDescription;
import com.developmentontheedge.beans.annot.PropertyName;

import biouml.plugins.ensembl.access.EnsemblDatabase;
import biouml.plugins.ensembl.access.EnsemblDatabaseSelector;
import biouml.plugins.ensembl.type.Transcript;
import ru.biosoft.access.core.ClassIcon;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.analysiscore.AbstractAnalysisParameters;
import ru.biosoft.analysiscore.AnalysisMethodSupport;
import ru.biosoft.bsa.Site;
import ru.biosoft.bsa.SqlTrack;
import ru.biosoft.bsa.Track;
import ru.biosoft.table.TableDataCollection;
import ru.biosoft.table.TableDataCollectionUtils;
import ru.biosoft.util.bean.BeanInfoEx2;

@ClassIcon ( "resources/get_transcripts_track.gif" )
public class GetTranscriptsTrack extends AnalysisMethodSupport<GetTranscriptsTrack.Parameters>
{

    public GetTranscriptsTrack(DataCollection<?> origin, String name)
    {
        super( origin, name, new Parameters() );
    }

    @Override
    public Object justAnalyzeAndPut() throws Exception
    {
        List<String> transcriptIds = parameters.getTranscriptsTable().getDataElement( TableDataCollection.class ).getNameList();
        SqlTrack outTrack = SqlTrack.createTrack( parameters.getOutputTrack(), null, parameters.getEnsembl().getPrimarySequencesPath() );

        TableDataCollection unmatched = parameters.getUnmatchedTranscripts() == null ? null
                : TableDataCollectionUtils.createTableDataCollection( parameters.getUnmatchedTranscripts() );

        AtomicInteger unmatchedCount = new AtomicInteger( 0 );
        DataCollection<Transcript> transcriptsCollection = parameters.getEnsembl().getTranscriptsCollection();
        jobControl.forCollection( transcriptIds, id -> {
            try
            {
                Transcript transcript = null;
                try
                {
                    transcript = transcriptsCollection.get( id );
                }
                catch( Exception e )
                {
                }
                if( transcript == null )
                {
                    unmatchedCount.incrementAndGet();
                    if( unmatched != null )
                        TableDataCollectionUtils.addRow( unmatched, id, new Object[] {}, true );
                }
                else
                {
                    Site site = transcript.getSite();
                    outTrack.addSite( site );
                }
            }
            catch( Exception e )
            {
                throw new RuntimeException( e );
            }
            return true;
        } );

        if( unmatchedCount.get() > 0 )
            log.warning( unmatchedCount.get() + " transcripts were not matched to Ensembl" );

        outTrack.finalizeAddition();
        parameters.getOutputTrack().save( outTrack );

        if( unmatched != null )
        {
            unmatched.finalizeAddition();
            parameters.getUnmatchedTranscripts().save( unmatched );
            return new Object[] {outTrack, unmatched};
        }

        return outTrack;
    }

    public static class Parameters extends AbstractAnalysisParameters
    {
        private DataElementPath transcriptsTable;
        @PropertyName ( "Transcripts table" )
        @PropertyDescription ( "Table with Ensembl transcripts" )
        public DataElementPath getTranscriptsTable()
        {
            return transcriptsTable;
        }
        public void setTranscriptsTable(DataElementPath transcriptsTable)
        {
            Object oldValue = this.transcriptsTable;
            this.transcriptsTable = transcriptsTable;
            firePropertyChange( "transcriptsTable", oldValue, transcriptsTable );
        }

        private EnsemblDatabase ensembl = EnsemblDatabaseSelector.getDefaultEnsembl();
        @PropertyName ( "Ensembl database" )
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

        private DataElementPath outputTrack;
        @PropertyName ( "Output track" )
        @PropertyDescription ( "Track with Ensembl transcripts" )
        public DataElementPath getOutputTrack()
        {
            return outputTrack;
        }
        public void setOutputTrack(DataElementPath outputTrack)
        {
            Object oldValue = this.outputTrack;
            this.outputTrack = outputTrack;
            firePropertyChange( "outputTrack", oldValue, outputTrack );
        }

        private DataElementPath unmatchedTranscripts;
        @PropertyName ( "Unmatched transcripts" )
        @PropertyDescription ( "Transcripts that were not found in Ensembl" )
        public DataElementPath getUnmatchedTranscripts()
        {
            return unmatchedTranscripts;
        }
        public void setUnmatchedTranscripts(DataElementPath unmatchedTranscripts)
        {
            Object oldValue = this.unmatchedTranscripts;
            this.unmatchedTranscripts = unmatchedTranscripts;
            firePropertyChange( "unmatchedTranscripts", oldValue, unmatchedTranscripts );
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
            property( "transcriptsTable" ).inputElement( TableDataCollection.class ).add();
            property( "ensembl" ).add();
            property( "outputTrack" ).outputElement( Track.class ).add();
            property( "unmatchedTranscripts" ).outputElement( TableDataCollection.class ).canBeNull().add();
        }
    }
}
