package biouml.plugins.gtrd.analysis;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import com.developmentontheedge.beans.annot.PropertyName;
import com.developmentontheedge.beans.model.ComponentFactory;
import com.developmentontheedge.beans.model.ComponentModel;

import biouml.standard.type.Species;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.DataCollectionUtils;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.analysiscore.AbstractAnalysisParameters;
import ru.biosoft.analysiscore.AnalysisMethodSupport;
import ru.biosoft.bsa.project.Project;
import ru.biosoft.util.TextUtil;
import ru.biosoft.util.bean.BeanInfoEx2;
import ru.biosoft.workbench.editors.GenericComboBoxEditor;

public class OpenPerTFView extends AnalysisMethodSupport<OpenPerTFView.Parameters>
{
    public OpenPerTFView(DataCollection<?> origin, String name)
    {
        super( origin, name, new Parameters() );
    }
    
    @Override
    public Object justAnalyzeAndPut() throws Exception
    {
        DataElementPath viewPath = DataElementPath.create( "databases/GTRD/Data/clusters" )
            .getChildPath( parameters.getOrganism().getLatinName(), "By TF", parameters.getUniprotId(), "view" );
        return viewPath.getDataElement( Project.class );
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

    public static class Parameters extends AbstractAnalysisParameters
    {
        private Species organism = Species.getSpecies( "Homo sapiens" );
        @PropertyName("Organism")
        public Species getOrganism()
        {
            return organism;
        }
        public void setOrganism(Species organism)
        {
            Species oldValue = this.organism;
            this.organism = organism;
            firePropertyChange( "organism", oldValue, organism );
            setTf( TFSelector.NOT_SELECTED );
        }
        
        private String tf = TFSelector.NOT_SELECTED;
        @PropertyName("Transcription factor")
        public String getTf()
        {
            return tf;
        }
        public void setTf(String tf)
        {
            String oldValue = this.tf;
            this.tf = tf;
            firePropertyChange( "tf", oldValue, tf );
        }
        
        public String getUniprotId()
        {
            return TextUtil.split( getTf(), ' ' )[1];
        }
    }
    
    public static class ParametersBeanInfo extends BeanInfoEx2<Parameters>
    {
        public ParametersBeanInfo()
        {
            super(Parameters.class);
        }
        
        protected ParametersBeanInfo(Class<? extends Parameters> beanClass)
        {
            super( beanClass );
        }
        
        @Override
        protected void initProperties() throws Exception
        {
            property( "organism" ).editor( SpeciesSelector.class ).hideChildren().add();
            add( "tf", TFSelector.class );
        }
    }
    
    public static class TFSelector extends GenericComboBoxEditor
    {
        public static final String NOT_SELECTED = "(not selected)";
        @Override
        public String[] getAvailableValues()
        {
            Connection con = DataCollectionUtils.getSqlConnection( DataElementPath.create( "databases/GTRD" ).getDataElement() );
            Object parameters = getBean();
            ComponentModel model = ComponentFactory.getModel( parameters );
            Species organism = (Species)model.findProperty( "organism" ).getValue();

            String query = "SELECT DISTINCT uniprot.id, uniprot.gene_name FROM uniprot "
                    + "JOIN chip_experiments ce on(uniprot.id=ce.tf_uniprot_id) "
                    + "JOIN clusters_finished cf on(uniprot.id=cf.uniprot_id AND ce.specie=cf.specie) "
                    + "WHERE ce.specie=? ORDER BY 2";
            

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
                        result.add( geneName + " " + uniprotId );
                    }
                }
            }
            catch( SQLException e )
            {
                throw new RuntimeException(e);
            }

            return result.toArray(new String[0]);
        }
    }
    
    public static class SpeciesSelector extends GenericComboBoxEditor
    {
        @Override
        protected Object[] getAvailableValues()
        {
            return Species.allSpecies().toArray();
        }
    }
}
