package biouml.plugins.gtrd.analysis;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import com.developmentontheedge.beans.annot.PropertyDescription;
import com.developmentontheedge.beans.annot.PropertyName;
import com.developmentontheedge.beans.model.ComponentFactory;
import com.developmentontheedge.beans.model.ComponentModel;

import biouml.plugins.gtrd.analysis.OpenPerTFView.SpeciesSelector;
import biouml.standard.type.Species;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.DataCollectionUtils;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.sql.SqlDataElement;
import ru.biosoft.access.sql.SqlUtil;
import ru.biosoft.analysiscore.AbstractAnalysisParameters;
import ru.biosoft.analysiscore.AnalysisMethodSupport;
import ru.biosoft.bsa.SqlTrack;
import ru.biosoft.table.SqlTableDataCollection;
import ru.biosoft.util.TextUtil2;
import ru.biosoft.util.bean.BeanInfoEx2;
import ru.biosoft.workbench.editors.GenericComboBoxEditor;

public class SearchByRegulation<T extends SearchByRegulation.Parameters> extends AnalysisMethodSupport<T>
{

    public SearchByRegulation(DataCollection<?> origin, String name, T parameters)
    {
        super( origin, name, parameters);
    }
    
    protected List<String> getRestrictions()
    {
        List<String> restrictions = new ArrayList<>();
        if(parameters.getDataSet().contains( "clusters" ))
        {
            if( !parameters.getTf().equals( "Any" ) )
                restrictions.add( "tf_uniprot_id=" + SqlUtil.quoteString( parameters.getUniprotId() ) );
            if( !parameters.getCellLine().equals( "Any" ) )
                restrictions.add( "cell_line=" + SqlUtil.quoteString( parameters.getCellLine() ) );
            if( !parameters.getTreatment().equals( "Any" ) )
                restrictions.add( "treatment=" + SqlUtil.quoteString( parameters.getTreatment() ) );
        }
        else
        {
            if( !parameters.getTf().equals( "Any" ) )
                restrictions.add( "prop_uniprotId=" + SqlUtil.quoteString( parameters.getUniprotId() ) );
            if( !parameters.getCellLine().equals( "Any" ) )
                restrictions.add( "prop_cellLine=" + SqlUtil.quoteString( parameters.getCellLine() ) );
            if( !parameters.getTreatment().equals( "Any" ) )
                restrictions.add( "prop_treatment=" + SqlUtil.quoteString( parameters.getTreatment() ) );
        }
        return restrictions;
    }
    
    protected List<String> getJoins() throws SQLException
    {
        List<String> result = new ArrayList<>();
        if(parameters.getDataSet().contains( "clusters" ))
        {
            String clusterToExpTable = getTableId( parameters.getClusterToExpTable() );
            result.add( clusterToExpTable + " ON(Cluster_id=track.id)" );
            result.add( "chip_experiments ON(Exp_id=chip_experiments.id)" );
        }
        return result;
    }
    
    protected static String getTableId(SqlDataElement e) throws SQLException
    {
        return SqlUtil.quoteIdentifier( e.getConnection().getCatalog() ) + "." + SqlUtil.quoteIdentifier( e.getTableId() );
    }


    public static class Parameters extends AbstractAnalysisParameters
    {
        private String cellLine = "Any", treatment = "Any";
        private Species organism = Species.getDefaultSpecies( null );
        private int maxGeneDistance = 5000;
        private String dataSet = "meta clusters";
        private String tf = "Any";
        
        private DataElementPath preparedTables = DataElementPath.create( "databases/GTRD/Data/generic/search by regulation" );
        private DataElementPath peaksFolder = DataElementPath.create("databases/GTRD/Data/peaks");
        private DataElementPath clustersFolder = DataElementPath.create("databases/GTRD/Data/clusters");

        @PropertyName( "Data set" )
        @PropertyDescription( "Data set" )
        public String getDataSet()
        {
            return dataSet;
        }

        public void setDataSet(String dataSet)
        {
            Object oldValue = this.dataSet;
            this.dataSet = dataSet;
            firePropertyChange( "dataSet", oldValue, dataSet );
        }
        
        public boolean isClusters()
        {
            return dataSet.contains( "clusters" );
        }

        @PropertyName( "Cell line" )
        @PropertyDescription( "Cell line" )
        public String getCellLine()
        {
            return cellLine;
        }

        public void setCellLine(String cellLine)
        {
            Object oldValue = this.cellLine;
            this.cellLine = cellLine;
            firePropertyChange( "cellLine", oldValue, cellLine );
        }

        @PropertyName( "Treatment" )
        @PropertyDescription( "Treatment/condition" )
        public String getTreatment()
        {
            return treatment;
        }

        public void setTreatment(String treatment)
        {
            Object oldValue = this.treatment;
            this.treatment = treatment;
            firePropertyChange( "treatment", oldValue, treatment );
        }

        @PropertyName( "Organism" )
        @PropertyDescription( "Organism" )
        public Species getOrganism()
        {
            return organism;
        }

        public void setOrganism(Species organism)
        {
            Object oldValue = this.organism;
            this.organism = organism;
            firePropertyChange( "organism", oldValue, organism );
            setCellLine( "Any" );
            setTreatment( "Any" );
            if( requiresAnyTF() )
                setTf( "Any" );
        }

        @PropertyName ( "Max gene distance" )
        @PropertyDescription ( "Maximal distance from site to gene" )
        public int getMaxGeneDistance()
        {
            return maxGeneDistance;
        }

        public void setMaxGeneDistance(int maxGeneDistance)
        {
            Object oldValue = this.maxGeneDistance;
            this.maxGeneDistance = maxGeneDistance;
            firePropertyChange( "maxGeneDistance", oldValue, maxGeneDistance );
        }

        @PropertyName( "Transcription factor" )
        @PropertyDescription( "Transcription factor" )
        public String getTf()
        {
            return tf;
        }

        public void setTf(String tf)
        {
            Object oldValue = this.tf;
            this.tf = tf;
            firePropertyChange( "tf", oldValue, tf );
        }
        
        public boolean requiresAnyTF()
        {
            return true;
        }
        
        public String getUniprotId()
        {
            return TextUtil2.split( getTf(), ' ' )[1];
        }

        
        public DataElementPath getPreparedTables()
        {
            return preparedTables;
        }

        public void setPreparedTables(DataElementPath preparedTables)
        {
            Object oldValue = this.preparedTables;
            this.preparedTables = preparedTables;
            firePropertyChange( "preparedTables", oldValue, preparedTables );
        }
        
        public SqlTableDataCollection getPreparedTable()
        {
            return getPreparedTables().getChildPath( getOrganism().getLatinName()  + " " + getDataSet() + " table").getDataElement( SqlTableDataCollection.class );
        }
        
        public SqlTableDataCollection getClusterToExpTable()
        {
            return getPreparedTables().getChildPath( getOrganism().getLatinName() + " " + getDataSet() + " to exp table" ).getDataElement( SqlTableDataCollection.class );
        }
        
        public DataElementPath getPeaksFolder()
        {
            return peaksFolder;
        }

        public void setPeaksFolder(DataElementPath peaksFolder)
        {
            Object oldValue = this.peaksFolder;
            this.peaksFolder = peaksFolder;
            firePropertyChange( "peaksFolder", oldValue, peaksFolder );
        }
        
        public DataElementPath getClustersFolder()
        {
            return clustersFolder;
        }

        public void setClustersFolder(DataElementPath clustersFolder)
        {
            Object oldValue = this.clustersFolder;
            this.clustersFolder = clustersFolder;
            firePropertyChange( "clustersFolder", oldValue, clustersFolder );
        }

        public SqlTrack getPreparedTrack()
        {
            String dataSet = getDataSet();
            String[] fields = dataSet.split( " " );
            String caller = fields[0];
            String type = fields[1];
            if( type.equals( "peaks" ) )
            {
                return getPeaksFolder().getChildPath( "union" ).getChildPath( getOrganism().getLatinName() + " " + caller ).getDataElement( SqlTrack.class );
            } else if( type.equals( "clusters" ) )
            {
                caller = caller.equals( "meta" ) ? caller : caller.toUpperCase();
                return getClustersFolder().getChildPath( getOrganism().getLatinName(), "all " + caller + " clusters" ).getDataElement(SqlTrack.class);    
            }else
            {
                throw new IllegalArgumentException( "Wrong dataset: " + type );
            }
        }

    }

    public static class ParametersBeanInfo extends BeanInfoEx2<Parameters>
    {
        public ParametersBeanInfo()
        {
            this( Parameters.class );
        }
        
        protected ParametersBeanInfo(Class<? extends Parameters> beanClass)
        {
            super( beanClass );
        }

        @Override
        protected void initProperties() throws Exception
        {
            super.initProperties();

            property( "preparedTables" ).inputElement( ru.biosoft.access.core.DataCollection.class ).expert().add();
            property( "peaksFolder").inputElement( ru.biosoft.access.core.DataCollection.class ).expert().add();
            property( "organism" ).editor( SpeciesSelector.class ).hideChildren().add();
            
            initAfterOrganism();
            add( "tf", TFSelector.class );
            add( "dataSet", DataSetEditor.class );
            addHidden( "cellLine", CellLineSelector.class, "isClusters" );
            addHidden( "treatment", TreatmentSelector.class, "isClusters" );
            add( "maxGeneDistance" );
        }
        
        protected void initAfterOrganism() throws Exception
        {
            
        }
    }

    public static class DataSetEditor extends GenericComboBoxEditor
    {
        String cachedOrganismLatinName, cachedCell, cachedTreatment, cachedTf;
        Object[] cachedValues;
        
        @Override
        public synchronized Object[] getAvailableValues()
        {
            ComponentModel model = ComponentFactory.getModel( getBean() );
            Species organism = (Species)model.findProperty( "organism" ).getValue();
            String cell = model.findProperty( "cellLine" ).getValue().toString();
            String treatment = model.findProperty( "treatment" ).getValue().toString();
            String tf = model.findProperty( "tf" ).getValue().toString();
            
            if(Objects.equals( organism.getLatinName(), cachedOrganismLatinName ) && Objects.equals( cell, cachedCell ) && Objects.equals( treatment, cachedTreatment ) && Objects.equals( tf, cachedTf ))
                return cachedValues;
            
            Set<String> dataSets = new HashSet<>();
            Connection con = DataCollectionUtils.getSqlConnection( DataElementPath.create( "databases/GTRD" ).getDataElement() );
            
            String query = "SELECT DISTINCT pf.peak_type FROM peaks_finished pf JOIN chip_experiments ce ON(ce.id=pf.exp_id) JOIN cells on(ce.cell_id=cells.id) WHERE ce.specie=" + SqlUtil.quoteString( organism.getLatinName() ) ;
            if( !cell.equals( "Any" ) )
            {
                query += " AND cells.title=" + SqlUtil.quoteString( cell );
            }
            if( !treatment.equals( "Any" ) )
                query += " AND ce.treatment=" + SqlUtil.quoteString( treatment );
            if( !tf.equals( "Any" ) )
                query += " AND ce.tf_uniprot_id=" + SqlUtil.quoteString( TextUtil2.split( tf, ' ' )[1] );
            
            dataSets.addAll( SqlUtil.stringStream( con, query ).map( x->x+" peaks" ).toList() );
            
            query = "SELECT DISTINCT cluster_type FROM clusters_finished WHERE specie=" + SqlUtil.quoteString( organism.getLatinName() );
            if( !tf.equals( "Any" ) )
                    query += " AND uniprot_id=" + SqlUtil.quoteString( TextUtil2.split( tf, ' ' )[1] );
            dataSets.addAll( SqlUtil.stringStream( con, query ).map( x->x+" clusters" ).toList() );
            
            String[] order = {"meta clusters", "macs2 clusters", "macs clusters", "sissrs clusters", "gem clusters", "pics clusters",
                    "macs2 peaks", "macs peaks", "sissrs peaks", "gem peaks", "pics peaks"};
            Object[] result = new Object[dataSets.size()];
            int j = 0;
            for(int i = 0; i < order.length; i++)
                if(dataSets.contains( order[i] ))
                    result[j++] = order[i];
            
            cachedOrganismLatinName = organism.getLatinName();
            cachedCell = cell;
            cachedTreatment = treatment;
            cachedTf = tf;
            cachedValues = result;
            
            return result;
        }
    }

    public static class CellLineSelector extends GenericComboBoxEditor
    {
        String cachedOrganismLatinName, cachedTreatment, cachedTf, cachedDataSet;
        Object[] cachedValues;
        
        @Override
        protected synchronized Object[] getAvailableValues()
        {
            
            ComponentModel model = ComponentFactory.getModel( getBean() );
            Species organism = (Species)model.findProperty( "organism" ).getValue();
            String treatment = model.findProperty( "treatment" ).getValue().toString();
            String tf = model.findProperty( "tf" ).getValue().toString();
            String dataSet = model.findProperty( "dataSet" ).getValue().toString();
            

            if(Objects.equals( organism.getLatinName(), cachedOrganismLatinName ) && Objects.equals( dataSet, cachedDataSet ) && Objects.equals( treatment, cachedTreatment ) && Objects.equals( tf, cachedTf ))
                return cachedValues;

            Connection con = DataCollectionUtils.getSqlConnection( DataElementPath.create( "databases/GTRD" ).getDataElement() );
            String query = "SELECT DISTINCT cells.title FROM chip_experiments ce JOIN cells on(ce.cell_id=cells.id)"
                    + " LEFT JOIN peaks_finished pf on(ce.id=pf.exp_id)"
                    + " LEFT JOIN clusters_finished cf on(ce.tf_uniprot_id=cf.uniprot_id AND ce.specie=cf.specie)"
                    + " WHERE ce.specie=" + SqlUtil.quoteString( organism.getLatinName() );
            
            
            String dataSetType = dataSet.split( " " )[0].toLowerCase();
            if(dataSet.contains( "clusters" ))
            {
                query = "SELECT DISTINCT cells.title FROM chip_experiments ce JOIN cells on(ce.cell_id=cells.id)"
                        + " LEFT JOIN clusters_finished cf on(ce.tf_uniprot_id=cf.uniprot_id AND ce.specie=cf.specie)"
                        + " WHERE ce.specie=" + SqlUtil.quoteString( organism.getLatinName() )
                        + " AND cf.cluster_type=" + SqlUtil.quoteString( dataSetType );
            }
            else
            {
                query = "SELECT DISTINCT cells.title FROM chip_experiments ce JOIN cells on(ce.cell_id=cells.id)"
                        + " LEFT JOIN peaks_finished pf on(ce.id=pf.exp_id)"
                        + " WHERE ce.specie=" + SqlUtil.quoteString( organism.getLatinName() )
                        + " AND pf.peak_type=" + SqlUtil.quoteString( dataSetType );
                if( !treatment.equals( "Any" ) )
                    query += " AND ce.treatment=" + SqlUtil.quoteString( treatment );
            }
            if( !tf.equals( "Any" ) )
                query += " AND ce.tf_uniprot_id=" + SqlUtil.quoteString( TextUtil2.split( tf, ' ' )[1] );
            query += " ORDER by 1";
            
            cachedOrganismLatinName = organism.getLatinName();
            cachedDataSet = dataSet;
            cachedTreatment = treatment;
            cachedTf = tf;
            cachedValues = SqlUtil.stringStream( con, query ).prepend( "Any" ).nonNull().toArray();
            return cachedValues;
        }
    }

    public static class TreatmentSelector extends GenericComboBoxEditor
    {
        String cachedOrganismLatinName, cachedCell, cachedTf, cachedDataSet;
        Object[] cachedValues;
        
        @Override
        protected synchronized Object[] getAvailableValues()
        {
            ComponentModel model = ComponentFactory.getModel( getBean() );
            Species organism = (Species)model.findProperty( "organism" ).getValue();
            String cell = model.findProperty( "cellLine" ).getValue().toString();
            String tf = model.findProperty( "tf" ).getValue().toString();
            String dataSet = model.findProperty( "dataSet" ).getValue().toString();

            if(Objects.equals( organism.getLatinName(), cachedOrganismLatinName ) && Objects.equals( dataSet, cachedDataSet ) && Objects.equals( cell, cachedCell ) && Objects.equals( tf, cachedTf ))
                return cachedValues;

            
            Connection con = DataCollectionUtils.getSqlConnection( DataElementPath.create( "databases/GTRD" ).getDataElement() );
            String query;
            
            
            String dataSetType = dataSet.split( " " )[0].toLowerCase();
            if(dataSet.contains( "clusters" ))
            {
                query = "SELECT DISTINCT ce.treatment FROM chip_experiments ce JOIN cells on(ce.cell_id=cells.id)"
                        + " LEFT JOIN clusters_finished cf on(ce.tf_uniprot_id=cf.uniprot_id AND ce.specie=cf.specie)"
                        + " WHERE ce.specie=" + SqlUtil.quoteString( organism.getLatinName() )
                        + " AND NOT isNULL(treatment)"
                        + " AND cf.cluster_type=" + SqlUtil.quoteString( dataSetType );
            }
            else
            {
                query = "SELECT DISTINCT ce.treatment FROM chip_experiments ce JOIN cells on(ce.cell_id=cells.id)"
                        + " LEFT JOIN peaks_finished pf on(ce.id=pf.exp_id)"
                        + " WHERE ce.specie=" + SqlUtil.quoteString( organism.getLatinName() )
                        + " AND NOT isNULL(treatment)"
                        + " AND pf.peak_type=" + SqlUtil.quoteString( dataSetType );
                if( !cell.equals( "Any" ) )
                {
                    query += " AND cells.title=" + SqlUtil.quoteString( cell );
                }
            }
            if( !tf.equals( "Any" ) )
                query += " AND ce.tf_uniprot_id=" + SqlUtil.quoteString( TextUtil2.split( tf, ' ' )[1] );

            query += " ORDER by 1";
            
            cachedOrganismLatinName = organism.getLatinName();
            cachedDataSet = dataSet;
            cachedCell = cell;
            cachedTf = tf;
            cachedValues = SqlUtil.stringStream( con, query ).prepend( "Any" ).nonNull().toArray();
            
            return cachedValues;
        }
    }
    
    public static class TFSelector extends GenericComboBoxEditor
    {
        String cachedOrganismLatinName, cachedCell, cachedTreatment, cachedDataSet;
        Object[] cachedValues;
        
        
        @Override
        protected synchronized Object[] getAvailableValues()
        {
            Parameters parameters = (Parameters)getBean();
            ComponentModel model = ComponentFactory.getModel( parameters );
            Species organism = (Species)model.findProperty( "organism" ).getValue();
            String dataSet = model.findProperty( "dataSet" ).getValue().toString();
            String treatment = model.findProperty( "treatment" ).getValue().toString();
            String cell = model.findProperty( "cellLine" ).getValue().toString();
            
            if(Objects.equals( organism.getLatinName(), cachedOrganismLatinName ) && Objects.equals( dataSet, cachedDataSet ) && Objects.equals( cell, cachedCell ) && Objects.equals( treatment, cachedTreatment ))
                return cachedValues;
            
            Connection con = DataCollectionUtils.getSqlConnection( DataElementPath.create( "databases/GTRD" ).getDataElement() );
            String query;
            
            
            String dataSetType = dataSet.split( " " )[0].toLowerCase();
            if(dataSet.contains( "clusters" ))
            {
                query = "SELECT DISTINCT uniprot.id, uniprot.gene_name FROM uniprot "
                        + " JOIN chip_experiments ce on(uniprot.id=ce.tf_uniprot_id)"
                        + " LEFT JOIN clusters_finished cf on(uniprot.id=cf.uniprot_id AND ce.specie=cf.specie) "
                        + " WHERE ce.specie=" + SqlUtil.quoteString( organism.getLatinName() )
                        + " AND cf.cluster_type=" + SqlUtil.quoteString( dataSetType );
            }
            else
            {
                query = "SELECT DISTINCT uniprot.id, uniprot.gene_name FROM uniprot "
                        + " JOIN chip_experiments ce on(uniprot.id=ce.tf_uniprot_id) JOIN cells on(ce.cell_id=cells.id)"
                        + " LEFT JOIN peaks_finished pf on(ce.id=pf.exp_id)"
                        + " WHERE ce.specie=" + SqlUtil.quoteString( organism.getLatinName() )
                        + " AND pf.peak_type=" + SqlUtil.quoteString( dataSetType );
               
                if( !treatment.equals( "Any" ) )
                    query += " AND ce.treatment=" + SqlUtil.quoteString( treatment );
                if( !cell.equals( "Any" ) )
                    query += " AND cells.title=" + SqlUtil.quoteString( cell );
            }
            
            query += " ORDER by 2";

            List<String> result = new ArrayList<>();
            if(parameters.requiresAnyTF())
                result.add( "Any" );
                
            try(Statement st = con.createStatement();
                    ResultSet rs = st.executeQuery( query ))
            {
                while(rs.next())
                {
                    String uniprotId = rs.getString( 1 );
                    String geneName = rs.getString( 2 );
                    result.add( geneName + " " + uniprotId );
                }
            }
            catch( SQLException e )
            {
                throw new RuntimeException(e);
            }
            
            
            cachedOrganismLatinName = organism.getLatinName();
            cachedDataSet = dataSet;
            cachedCell = cell;
            cachedTreatment = treatment;
            cachedValues = result.toArray();
            
            return cachedValues;
        }
    }
    
}
