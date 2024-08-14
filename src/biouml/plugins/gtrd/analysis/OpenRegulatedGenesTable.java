package biouml.plugins.gtrd.analysis;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import com.developmentontheedge.beans.annot.PropertyDescription;
import com.developmentontheedge.beans.annot.PropertyName;

import biouml.standard.type.Species;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.DataCollectionUtils;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.analysiscore.AnalysisMethodSupport;
import ru.biosoft.table.TableDataCollection;
import ru.biosoft.util.bean.BeanInfoEx2;
import ru.biosoft.workbench.editors.GenericComboBoxEditor;

public class OpenRegulatedGenesTable extends AnalysisMethodSupport<OpenRegulatedGenesTable.Parameters>
{
    public OpenRegulatedGenesTable(DataCollection<?> origin, String name)
    {
        super( origin, name, new Parameters() );
    }
    
    @Override
    public Object justAnalyzeAndPut() throws Exception
    {
        DataElementPath tablePath = parameters.getFolder().getChildPath( parameters.getUniprotId() );
        return tablePath.getDataElement( TableDataCollection.class );
    }
    
    @Override
    public void validateParameters() throws IllegalArgumentException
    {
        super.validateParameters();
        if( TFSelector.NOT_SELECTED.equals( parameters.getTf() ) )
            throw new IllegalArgumentException("Transcription factor not selected");
    }
    
    @Override
    protected void writeProperties(DataElement de) throws Exception
    {
        //Don't write properties to results since we just return existing element
    }

    public static class Parameters extends OpenPerTFView.Parameters
    {
        private static final String METHOD_EXPRESSION = "Analysis of RNA expression";
        private static final String METHOD_TF_BINDING = "Analysis of TF binding";
        private static final String[] METHOD_LIST = {METHOD_TF_BINDING, METHOD_EXPRESSION};
        private String method = METHOD_TF_BINDING;

        @PropertyName("Method")
        @PropertyDescription("Method used to identify regulated genes")
        public String getMethod()
        {
            return method;
        }
        public void setMethod(String method)
        {
            String oldValue = this.method;
            this.method = method;
            firePropertyChange( "method", oldValue, method );
            setOrganism( Species.getSpecies( "Homo sapiens" ) );
        }
        
        private static final String TYPE_PROMOTER_1000_100 = "promoter[-1000,+100]";
        private static final String TYPE_PROMOTER_500_50 = "promoter[-500,+50]";
        private static final String TYPE_PROMOTER_100_10 = "promoter[-100,+10]";
        public static final String TYPE_WHOLE_GENE = "whole gene[-5000,+5000]";
        public static final String[] TYPE_LIST = {TYPE_PROMOTER_100_10, TYPE_PROMOTER_500_50, TYPE_PROMOTER_1000_100, TYPE_WHOLE_GENE };
        private String type = TYPE_PROMOTER_100_10;

        @PropertyName("TF binding site location")
        @PropertyDescription("Where TF binding site should be located in target gene")
        public String getType()
        {
            return type;
        }
        public void setType(String type)
        {
            String oldValue = this.type;
            this.type = type;
            firePropertyChange( "type", oldValue, type );
        }
        public boolean isTypeHidden()
        {
            return !getMethod().equals( METHOD_TF_BINDING );
        }
        private static String getFolderByType(String type)
        {
            if(type.equals( Parameters.TYPE_WHOLE_GENE ))
                return "genes whole[-5000,+5000]";
            return "genes " + type;
        }
        
        
        DataElementPath getFolder(Species organism)
        {
            DataElementPath res = DataElementPath.create( "databases/GTRD/Data/generic/target genes" ).getChildPath( organism.getLatinName() );
            if(getMethod().equals( METHOD_EXPRESSION ))
                res = res.getChildPath( PrepareTargetGenesByExpression.FOLDER_NAME );
            else
                res = res.getChildPath( getFolderByType(getType()) );
            return res;
        }
        
        DataElementPath getFolder()
        {
            return getFolder(getOrganism());
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
           property("method").tags( Parameters.METHOD_LIST ).add();
           property("type").tags( Parameters.TYPE_LIST ).hidden( "isTypeHidden" ).add();
           property( "organism" ).editor( SpeciesSelector.class ).hideChildren().add();
           add( "tf", TFSelector.class );
        }
    }
    
    public static class SpeciesSelector extends GenericComboBoxEditor
    {
        @Override
        protected Object[] getAvailableValues()
        {
            Parameters params = (Parameters)getBean();
            return Species.allSpecies().filter( s->params.getFolder( s ).exists() ).toArray();
        }
    }
    
    public static class TFSelector extends GenericComboBoxEditor
    {
        public static final String NOT_SELECTED = "(not selected)";
        @Override
        protected Object[] getAvailableValues()
        {
            Connection con = DataCollectionUtils.getSqlConnection( DataElementPath.create( "databases/GTRD/Data/experiments" ).getDataElement() );
            Parameters parameters = (Parameters)getBean();
            Species organism = parameters.getOrganism();
            
            DataCollection<?> dc = parameters.getFolder().optDataCollection();

            String query = "SELECT DISTINCT uniprot.id, uniprot.gene_name FROM uniprot "
                    + "WHERE species=? ORDER BY 2";
            

            List<String> result = new ArrayList<>();
            result.add( NOT_SELECTED );
            try(PreparedStatement ps = con.prepareStatement( query ))
            {
                ps.setString( 1, organism.getLatinName() );
                try( ResultSet rs = ps.executeQuery() )
                {
                    while( rs.next() )
                    {
                        String uniprotId = rs.getString( 1 );
                        String geneName = rs.getString( 2 );
                        
                        if(dc.contains( uniprotId ))
                            result.add( geneName + " " + uniprotId );
                    }
                }
            }
            catch( SQLException e )
            {
                throw new RuntimeException(e);
            }

            return result.toArray();
        }
    }
    
}
