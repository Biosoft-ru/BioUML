package biouml.plugins.riboseq;

import java.util.List;

import com.developmentontheedge.beans.annot.PropertyName;

import biouml.plugins.riboseq.transcripts.Transcript;
import biouml.plugins.riboseq.transcripts.TranscriptSet;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.analysiscore.AbstractAnalysisParameters;
import ru.biosoft.analysiscore.AnalysisMethodSupport;
import ru.biosoft.bsa.Interval;
import ru.biosoft.table.TableDataCollection;
import ru.biosoft.table.TableDataCollectionUtils;
import ru.biosoft.util.bean.BeanInfoEx2;

public class TranscriptStructureAnalysis extends AnalysisMethodSupport<TranscriptStructureAnalysis.Parameters>
{

    public TranscriptStructureAnalysis(DataCollection<?> origin, String name)
    {
        super( origin, name, new Parameters() );
    }
    
    @Override
    public Object justAnalyzeAndPut() throws Exception
    {
        TableDataCollection outTable = TableDataCollectionUtils.createTableDataCollection( parameters.getOutputTable() );
        outTable.getColumnModel().addColumn( "Length", Integer.class );
        outTable.getColumnModel().addColumn( "FivePrimeUTRLength", Integer.class );
        outTable.getColumnModel().addColumn( "CDSLength", Integer.class );
        outTable.getColumnModel().addColumn( "ThreePrimeUTRLength", Integer.class );
        outTable.getColumnModel().addColumn( "CDSStart", Integer.class );
        outTable.getColumnModel().addColumn( "CDSEnd", Integer.class );
        List<Transcript> transcripts = parameters.getTranscriptSet().createTranscriptLoader().loadTranscripts( log );
        for(Transcript t : transcripts)
        {
            int length = t.getLength();
            int fivePrimeUTRLength = -1;
            int threePrimeUTRLength = -1;
            int cdsLength = -1;
            int cdsStart = -1;
            int cdsEnd = -1;
            if( t.isCoding() )
            {
                Interval cds = t.getCDSLocations().get( 0 );
                fivePrimeUTRLength = cds.getFrom();
                threePrimeUTRLength = t.getLength() - cds.getTo() - 1;
                cdsLength = cds.getLength();
                cdsStart = cds.getFrom();
                cdsEnd = cds.getTo();
            }
            TableDataCollectionUtils.addRow( outTable, t.getName(), new Object[] { length, fivePrimeUTRLength, cdsLength, threePrimeUTRLength, cdsStart, cdsEnd }, true );
        }
        outTable.finalizeAddition();
        parameters.getOutputTable().save( outTable );
        return outTable;
    }
    
    public static class Parameters extends AbstractAnalysisParameters
    {
        private TranscriptSet transcriptSet = new TranscriptSet();
        @PropertyName("Transcript set")
        public TranscriptSet getTranscriptSet()
        {
            return transcriptSet;
        }
        public void setTranscriptSet(TranscriptSet transcriptSet)
        {
            Object oldValue = this.transcriptSet;
            this.transcriptSet = transcriptSet;
            transcriptSet.setOnlyProteinCoding( true );
            firePropertyChange( "transcriptSet", oldValue, transcriptSet );
        }
        
        private DataElementPath outputTable;
        @PropertyName("Output table")
        public DataElementPath getOutputTable()
        {
            return outputTable;
        }
        public void setOutputTable(DataElementPath outputTable)
        {
            Object oldValue = this.outputTable;
            this.outputTable = outputTable;
            firePropertyChange( "outputTable", oldValue, outputTable );
        }
    }
    
    public static class ParametersBeanInfo extends BeanInfoEx2<Parameters>
    {
        public ParametersBeanInfo()
        {
            super(Parameters.class);
        }

        @Override
        protected void initProperties() throws Exception
        {
            add( "transcriptSet" );
            property( "outputTable" ).outputElement( TableDataCollection.class ).add();
        } 
        
    }
}
