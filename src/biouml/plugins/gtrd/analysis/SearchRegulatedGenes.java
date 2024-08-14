package biouml.plugins.gtrd.analysis;

import java.sql.Connection;
import java.util.List;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.biohub.ReferenceTypeRegistry;
import ru.biosoft.access.repository.DataElementPathEditor;
import ru.biosoft.access.sql.SqlUtil;
import ru.biosoft.analysis.FakeProgress;
import ru.biosoft.bsa.SqlTrack;
import ru.biosoft.table.SqlTableDataCollection;
import ru.biosoft.table.TableDataCollection;
import ru.biosoft.table.TableDataCollectionUtils;
import com.developmentontheedge.beans.annot.PropertyDescription;
import com.developmentontheedge.beans.annot.PropertyName;

import biouml.plugins.ensembl.tabletype.EnsemblGeneTableType;
import one.util.streamex.StreamEx;

import com.developmentontheedge.beans.PropertyDescriptorEx;

public class SearchRegulatedGenes extends SearchByRegulation<SearchRegulatedGenes.Parameters>
{

    public SearchRegulatedGenes(DataCollection<?> origin, String name)
    {
        super( origin, name, new Parameters() );
    }

    @Override
    public Object justAnalyzeAndPut() throws Exception
    {
        SqlTrack preparedTrack = parameters.getPreparedTrack();
        Connection con = preparedTrack.getConnection();
        String preparedTrackId = getTableId( preparedTrack );
        String preparedTableId = getTableId( parameters.getPreparedTable() );
        
        if(parameters.getResultingGenes().exists())
            parameters.getResultingGenes().remove();
        SqlTableDataCollection resultingGenes = (SqlTableDataCollection)TableDataCollectionUtils.createTableDataCollection( parameters.getResultingGenes() );
        resultingGenes.getColumnModel().addColumn( "SiteCount", Integer.class );
        ReferenceTypeRegistry.setCollectionReferenceType( resultingGenes, EnsemblGeneTableType.class );
        String resultingGenesId = getTableId( resultingGenes );
        
        String query = "INSERT INTO " + resultingGenesId 
                +" SELECT GeneID as row_id, COUNT(distinct SiteID) as SiteCount FROM " + preparedTableId;
        query += StreamEx.of( getJoins() )
                .prepend( preparedTrackId + " track" + " ON(track.id=SiteID)" )
                .map( s->" JOIN " + s ).joining();
        List<String> restrictions = getRestrictions();
        restrictions.add( "Distance <= " + parameters.getMaxGeneDistance() );
        if(!restrictions.isEmpty())
            query += " WHERE " + String.join( " AND ", restrictions );
        query += " GROUP BY GeneID ORDER BY SiteCount DESC";
        
        FakeProgress progress = new FakeProgress( jobControl, 10000 );
        progress.start();
        SqlUtil.execute( con, query );
        progress.stop();
        
        parameters.getResultingGenes().save( resultingGenes );
        return resultingGenes;
    }

    @Override
    public void validateParameters() throws IllegalArgumentException
    {
        super.validateParameters();
        checkLesser( "maxGeneDistance", 50001 );
    }

    public static class Parameters extends SearchByRegulation.Parameters
    {
        public Parameters()
        {
            TFSelector tfSelector = new TFSelector();
            tfSelector.setBean( this );
            Object[] availableTFs = tfSelector.getAvailableValues();
            if(availableTFs.length > 0)
                setTf( (String)availableTFs[0] );
        }
        
        private DataElementPath resultingGenes = DataElementPath.create( "data/Collaboration/Demo/tmp/Regulated genes" );

        @PropertyName ( "Regulated genes" )
        @PropertyDescription ( "Found genes regulated by transcription factor" )
        public DataElementPath getResultingGenes()
        {
            return resultingGenes;
        }

        public void setResultingGenes(DataElementPath resultingGenes)
        {
            Object oldValue = this.resultingGenes;
            this.resultingGenes = resultingGenes;
            firePropertyChange( "resultingGenes", oldValue, resultingGenes );
        }
        
        @Override
        public boolean requiresAnyTF()
        {
            return false;
        }
    }

    public static class ParametersBeanInfo extends SearchByRegulation.ParametersBeanInfo
    {
        public ParametersBeanInfo()
        {
            super( Parameters.class );
        }

        @Override
        protected void initProperties() throws Exception
        {
            super.initProperties();
            PropertyDescriptorEx pde = DataElementPathEditor.registerOutput( "resultingGenes", beanClass, TableDataCollection.class );
            pde.setExpert( true );
            add( pde );
        }
    }

}
