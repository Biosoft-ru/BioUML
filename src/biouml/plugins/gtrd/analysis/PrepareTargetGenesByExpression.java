package biouml.plugins.gtrd.analysis;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.DataCollectionUtils;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.core.FolderCollection;
import ru.biosoft.access.biohub.ReferenceTypeRegistry;
import ru.biosoft.access.sql.SqlUtil;
import ru.biosoft.analysiscore.AbstractAnalysisParameters;
import ru.biosoft.analysiscore.AnalysisMethodSupport;
import ru.biosoft.table.ColumnModel;
import ru.biosoft.table.TableDataCollection;
import ru.biosoft.table.TableDataCollectionUtils;
import ru.biosoft.util.bean.BeanInfoEx2;

public class PrepareTargetGenesByExpression extends AnalysisMethodSupport<PrepareTargetGenesByExpression.Parameters>
{
    public static final String FOLDER_NAME = "DEG pval0.01";

    public PrepareTargetGenesByExpression(DataCollection<?> origin, String name)
    {
        super( origin, name, new Parameters() );
    }
    
    public static class DEG
    {
        String ensemblGeneId;
        String geneSymbol;
        double pValue;
        double log2FoldChange;
        
        void merge(DEG other)
        {
            if(other.pValue < pValue)
                pValue = other.pValue;
            if(Math.abs(other.log2FoldChange) > Math.abs( log2FoldChange ))
                log2FoldChange = other.log2FoldChange;
        }
    }

    @Override
    public Object justAnalyzeAndPut() throws Exception
    {
        Connection con = DataCollectionUtils.getSqlConnection( DataElementPath.create( "databases/GTRD/Data/experiments" ).getDataElement() );
        for(String organism : SqlUtil.queryStrings( con, "SELECT distinct organism FROM tfmod_rna_experiments" ))
        {
            for(String uniprotId : SqlUtil.queryStrings( con, "SELECT distinct tf_uniprot_id FROM tfmod_rna_experiments WHERE organism=" + SqlUtil.quoteString( organism ) ))
            {
                Map<String, DEG> genes = new HashMap<>();
                for(String expId : SqlUtil.queryStrings( con, "SELECT id FROM tfmod_rna_experiments WHERE tf_uniprot_id=" + SqlUtil.quoteString( uniprotId ) ))
                {
                    String query = "SELECT ensembl_gene_id,gene_id,p_value,log2_fold_change,tfmod_rna_experiments.experiment_type FROM tfmod_rna_expression JOIN tfmod_rna_experiments on(tfmod_rna_experiments.id=exp_id) WHERE p_value <= 0.01 AND exp_id=" + SqlUtil.quoteString( expId );
                    try(
                            Statement st = con.createStatement();
                            ResultSet rs = st.executeQuery( query );)
                    {
                        while(rs.next())
                        {
                            DEG deg = new DEG();
                            deg.ensemblGeneId = rs.getString( 1 );
                            deg.geneSymbol = rs.getString( 2 );
                            deg.pValue = rs.getDouble( 3 );
                            deg.log2FoldChange = rs.getDouble( 4 );
                            String expType = rs.getString( 5 );
                            if(expType.equals( "down" ))
                                deg.log2FoldChange *= -1;
                            
                            if(deg.ensemblGeneId == null)
                                continue;//was not matched to ensembl gene id
                            
                            DEG prev = genes.get( deg.ensemblGeneId );
                            if(prev == null)
                                genes.put( deg.ensemblGeneId, deg );
                            else
                                prev.merge( deg );
                        }
                    }
                }
                
                if(!genes.isEmpty())
                {
                    DataElementPath outPath = parameters.getOutFolder().getChildPath( organism, FOLDER_NAME, uniprotId );
                    DataCollectionUtils.createFoldersForPath( outPath );
                    List<DEG> degList = new ArrayList<>(genes.values());
                    Collections.sort( degList, Comparator.comparingDouble( deg->deg.pValue ) );
                    TableDataCollection table = TableDataCollectionUtils.createTableDataCollection( outPath );
                    ColumnModel cm = table.getColumnModel();
                    cm.addColumn( "Gene symbol", String.class );
                    cm.addColumn( "Log2FoldChange", Double.class );
                    cm.addColumn( "p-value", Double.class );
                    for(DEG deg : degList)
                        TableDataCollectionUtils.addRow( table, deg.ensemblGeneId, new Object[] {deg.geneSymbol, deg.log2FoldChange, deg.pValue}, true );
                    table.finalizeAddition();
                    TableDataCollectionUtils.setSortOrder( table, "p-value", true );
                    ReferenceTypeRegistry.setCollectionReferenceType( table, ReferenceTypeRegistry.getReferenceType( "Genes: Ensembl" ) );
                    outPath.save( table );
                }
            }
        }
        return new Object[0];
    }

    public static class Parameters extends AbstractAnalysisParameters
    {
        private DataElementPath outFolder = DataElementPath.create( "databases/GTRD/Data/generic/target genes" );

        public DataElementPath getOutFolder()
        {
            return outFolder;
        }

        public void setOutFolder(DataElementPath outFolder)
        {
            DataElementPath oldValue = this.outFolder;
            this.outFolder = outFolder;
            firePropertyChange( "outFolder", oldValue, outFolder );
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
            property( "outFolder" ).outputElement( FolderCollection.class ).add();
        }
    }
}
