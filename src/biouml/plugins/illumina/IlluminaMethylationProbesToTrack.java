package biouml.plugins.illumina;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashSet;

import com.developmentontheedge.beans.DynamicProperty;
import com.developmentontheedge.beans.annot.PropertyName;
import ru.biosoft.jobcontrol.Iteration;

import biouml.plugins.ensembl.access.EnsemblDatabase;
import biouml.plugins.ensembl.access.EnsemblDatabaseSelector;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.sql.Connectors;
import ru.biosoft.access.sql.SqlConnectionPool;
import ru.biosoft.analysiscore.AbstractAnalysisParameters;
import ru.biosoft.analysiscore.AnalysisMethodSupport;
import ru.biosoft.bsa.SiteImpl;
import ru.biosoft.bsa.SqlTrack;
import ru.biosoft.bsa.StrandType;
import ru.biosoft.table.TableDataCollection;
import ru.biosoft.util.bean.BeanInfoEx2;
import ru.biosoft.util.bean.StaticDescriptor;

public class IlluminaMethylationProbesToTrack extends AnalysisMethodSupport<IlluminaMethylationProbesToTrack.Parameters>
{

    //private static final String DB_URL = "jdbc:mysql://localhost:3306/illumina_methylation450";

    public IlluminaMethylationProbesToTrack(DataCollection<?> origin, String name)
    {
        super( origin, name, new Parameters() );
    }

    @Override
    public Object justAnalyzeAndPut() throws Exception
    {
        HashSet<String> chromosomes = new HashSet<>(
                parameters.getEnsembl().getPrimarySequencesPath().getDataCollection().getNameList() );
        SqlTrack result = SqlTrack.createTrack( parameters.getOutputTrack(), null, parameters.getEnsembl().getPrimarySequencesPath() );
        TableDataCollection table = parameters.getInputTable().getDataElement( TableDataCollection.class );
        
        try (
                //Connection con = SqlConnectionPool.getPersistentConnection( DB_URL, "illumina", "illumina" );
                Connection con = Connectors.getConnection( "illumina_methylation450" );
                PreparedStatement ps = con.prepareStatement( "SELECT chr,position,strand FROM probe_coord_hg38 WHERE id=?" );)
        {

            jobControl.forCollection( table.getNameList(), new Iteration<String>()
            {
                @Override
                public boolean run(String probeId)
                {
                    try
                    {
                        processProbe( probeId, chromosomes, ps, result );
                    }
                    catch( SQLException e )
                    {
                        throw new RuntimeException( e );
                    }
                    return true;
                }
            } );
        }
        
        result.finalizeAddition();
        parameters.getOutputTrack().save( result );
        return result;
    }
    
    private final StaticDescriptor PROBE_ID_DESCRIPTOR = StaticDescriptor.create( "Probe" );

    private void processProbe(String probeId, HashSet<String> chromosomes, PreparedStatement ps, SqlTrack result) throws SQLException
    {
        ps.setString( 1, probeId );
        try (ResultSet rs = ps.executeQuery())
        {
            if( rs.next() )
            {
                String chr = rs.getString( 1 );
                if( chr.startsWith( "chr" ) )
                    chr = chr.substring( "chr".length() );
                if( chr.equals( "M" ) )
                    chr = "MT";
                if( !chromosomes.contains( chr ) )
                    return;

                int pos = rs.getInt( 2 );
                int from = pos - parameters.getWidth() / 2;
                int to = from + parameters.getWidth() - 1;

                int strand = rs.getInt( 3 );
                int strandType = strand == 1 ? StrandType.STRAND_PLUS : StrandType.STRAND_MINUS;

                int start = strandType == StrandType.STRAND_PLUS ? from : to;

                SiteImpl site = new SiteImpl( null, chr, start, parameters.getWidth(), strandType, null );
                site.getProperties().add( new DynamicProperty( PROBE_ID_DESCRIPTOR, String.class, probeId ) );
                result.addSite( site );
            }
        }
    }

    public static class Parameters extends AbstractAnalysisParameters
    {
        private DataElementPath inputTable;
        @PropertyName ( "Input table" )
        public DataElementPath getInputTable()
        {
            return inputTable;
        }
        public void setInputTable(DataElementPath inputTable)
        {
            Object oldValue = this.inputTable;
            this.inputTable = inputTable;
            firePropertyChange( "inputTable", oldValue, inputTable );
        }

        private EnsemblDatabase ensembl = EnsemblDatabaseSelector.getEnsemblDatabases()[0];
        @PropertyName ( "Ensembl" )
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

        private int width = 400;
        @PropertyName ( "Width" )
        public int getWidth()
        {
            return width;
        }
        public void setWidth(int width)
        {
            int oldValue = this.width;
            this.width = width;
            firePropertyChange( "width", oldValue, width );
        }

        private DataElementPath outputTrack;
        @PropertyName ( "Output track" )
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
            property( "inputTable" ).inputElement( TableDataCollection.class ).add();
            add( "ensembl" );
            add( "width" );
            property( "outputTrack" ).outputElement( SqlTrack.class ).add();
        }
    }
}
