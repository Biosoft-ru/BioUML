package biouml.plugins.gtrd.analysis;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.function.Consumer;
import java.util.zip.GZIPOutputStream;

import com.developmentontheedge.beans.DynamicPropertySet;

import biouml.plugins.gtrd.ChIPseqExperiment;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.DataCollectionUtils;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.file.FileDataElement;
import ru.biosoft.access.repository.DataElementPathEditor;
import ru.biosoft.analysiscore.AbstractAnalysisParameters;
import ru.biosoft.analysiscore.AnalysisMethodSupport;
import ru.biosoft.bsa.Site;
import ru.biosoft.bsa.SqlTrack;
import ru.biosoft.bsa.Track;
import ru.biosoft.jobcontrol.Iteration;
import ru.biosoft.table.SqlTableDataCollection;
import ru.biosoft.table.TableDataCollection;
import ru.biosoft.util.bean.BeanInfoEx2;

public class CreateFlatFiles extends AnalysisMethodSupport<CreateFlatFiles.Parameters>
{
    public CreateFlatFiles(DataCollection<?> origin, String name)
    {
        super( origin, name, new Parameters() );
    }
    
    @Override
    public Object justAnalyzeAndPut() throws Exception
    {
        DataElementPath resultPath = parameters.getResult();
        File resultFile = DataCollectionUtils.getChildFile(resultPath.optParentCollection(), resultPath.getName());
        FileDataElement result = new FileDataElement(resultPath.getName(), resultPath.optParentCollection(), resultFile);
        
        exportToFile(resultFile);
        
        resultPath.save( result );
        return result;
    }

    private void exportToFile(File file) throws IOException, SQLException
    {
        loadClusterToExp();
        
        Track track = parameters.getClusters().getDataElement( Track.class );
        DataCollection<Site> sites = track.getAllSites();
        
        GZIPOutputStream outStream = new GZIPOutputStream( new FileOutputStream( file ) );
        try( BufferedWriter writer = new BufferedWriter( new OutputStreamWriter( outStream, "UTF-8" ) ) )
        {
            jobControl.forCollection( DataCollectionUtils.asCollection( sites, Site.class ), new SiteIteration(writer, parameters.isMetaClusters()) );
        }
    }
    
    private List<ChIPseqExperiment> experiments = new ArrayList<>();
    private int[] clusterIdList;
    private int[] experimentIdList;

    private void loadClusterToExp() throws SQLException
    {
        experiments = new ArrayList<>();
        parameters.getExperiments().getDataCollection( ChIPseqExperiment.class ).forEach( experiments::add );
        
        Map<String, Integer> experimentsIndex = new HashMap<>();
        for(int i = 0; i < experiments.size(); i++)
            experimentsIndex.put( experiments.get( i ).getName(), i);
        
        SqlTableDataCollection table = parameters.getClusterToExpTable().getDataElement( SqlTableDataCollection.class );
        
        clusterIdList = new int[table.getSize()];
        experimentIdList = new int[clusterIdList.length];
        
        Connection con = table.getConnection();
        String tableName = table.getTableId();
        
        int index = 0;
        try(Statement st = con.createStatement())
        {
            st.setFetchSize( Integer.MIN_VALUE );//otherwise jdbc will load all rows into memory
            try(ResultSet rs = st.executeQuery( "SELECT Cluster_id, Exp_id FROM " + tableName + " ORDER BY Cluster_id, Exp_id" ))
            {
                while(rs.next())
                {
                    int clusterId = rs.getInt( 1 );
                    String expId = rs.getString( 2 );
                    
                    int expIndex = experimentsIndex.get( expId );
                    
                    clusterIdList[index] = clusterId;
                    experimentIdList[index] = expIndex;
                    index++;
                }
            }
        }
    }
    
    private void forEachExperimentOfCluster(int clusterId, Consumer<ChIPseqExperiment> action)
    {
        int idx = Arrays.binarySearch( clusterIdList, clusterId );
        if(idx < 0)
            throw new IllegalArgumentException("Cluster " + clusterId + " not found");
        //skip to the first occurence of clusterId
        while(idx > 0 && clusterIdList[idx-1] == clusterId)
            idx--;
        
        while(idx < clusterIdList.length && clusterIdList[idx] == clusterId)
        {
            int expIndex = experimentIdList[idx];
            ChIPseqExperiment exp = experiments.get( expIndex );
            action.accept( exp );
            idx++;
        }
    }
    
    private class SiteIteration implements Iteration<Site>
    {
        private BufferedWriter writer;
        private boolean metaClusters;
        public SiteIteration(BufferedWriter writer, boolean metaClusters) throws IOException
        {
            this.writer = writer;
            this.metaClusters = metaClusters;
            writeHeader();
        }

        private void writeHeader() throws IOException
        {
            writer.append( "#CHROM\tSTART\tEND\tsummit\tuniprotId\ttfTitle\tcell.set\ttreatment.set\texp.set" );
            if(metaClusters)
                writer.append( "\tpeak-caller.set\tpeak-caller.count");
            writer.append( "\texp.count\tpeak.count\n" );
        }

        @Override
        public boolean run(Site site)
        {
            DynamicPropertySet props = site.getProperties();
            
            int clusterId = Integer.parseInt( site.getName() );

            TreeSet<String> cellSet = new TreeSet<>();
            TreeSet<String> treatmentSet = new TreeSet<>();
            TreeSet<String> expSet = new TreeSet<>();
            
            forEachExperimentOfCluster( clusterId, exp -> {
                String cellLine = exp.getCell().getTitle();
                if(cellLine == null)
                    cellLine = "";
                cellSet.add( cellLine );
                
                String treatment = exp.getTreatment();
                if(treatment == null)
                    treatment = "";
                treatmentSet.add( treatment );
                
                expSet.add( exp.getName() );
            } );
            
            String chr = site.getOriginalSequence().getName();
            
            try
            {
                writer
                    .append( "chr" ).append( chr ).append( '\t' )
                    .append( String.valueOf( site.getFrom() - 1 ) ).append( '\t' )
                    .append( String.valueOf( site.getTo() ) ).append( '\t' )
                    .append( props.getValueAsString( "summit" ) ).append( '\t' )
                    .append( props.getValueAsString( "uniprotId" ) ).append( '\t' )
                    .append( props.getValueAsString( "tfTitle" ) ).append( '\t' )
                    .append( String.join( ";", cellSet ) ).append( '\t' )
                    .append( String.join( ";", treatmentSet ) ).append( '\t' )
                    .append( String.join( ";", expSet ) ).append( '\t' );
                if(metaClusters)
                    writer
                        .append( props.getValueAsString( "peak-caller.set" ) ).append( '\t' )
                        .append( props.getValueAsString( "peak-caller.count" ) ).append( '\t' );
                writer
                    .append( String.valueOf( expSet.size() ) ).append( '\t' )
                    .append( props.getValueAsString( "peak.count" ) ).append( '\n' );
            }
            catch( IOException ex )
            {
                throw new RuntimeException( ex );
            }
            return true;
        }
        
    }

    public static class Parameters extends AbstractAnalysisParameters
    {
        private DataElementPath experiments;
        public DataElementPath getExperiments()
        {
            return experiments;
        }

        public void setExperiments(DataElementPath experiments)
        {
            Object oldValue = this.experiments;
            this.experiments = experiments;
            firePropertyChange( "experiments", oldValue, experiments );
        }
        
        private DataElementPath clusters;
        public DataElementPath getClusters()
        {
            return clusters;
        }
        public void setClusters(DataElementPath clusters)
        {
            Object oldValue = this.clusters;
            this.clusters = clusters;
            firePropertyChange( "clusters", oldValue, clusters );
        }
        
        private boolean metaClusters = true;
        public boolean isMetaClusters()
        {
            return metaClusters;
        }
        public void setMetaClusters(boolean metaClusters)
        {
            boolean oldValue = this.metaClusters;
            this.metaClusters = metaClusters;
            firePropertyChange( "metaClusters", oldValue, metaClusters );
        }

        private DataElementPath clusterToExpTable;
        public DataElementPath getClusterToExpTable()
        {
            return clusterToExpTable;
        }

        public void setClusterToExpTable(DataElementPath clusterToExpTable)
        {
            Object oldValue = this.clusterToExpTable;
            this.clusterToExpTable = clusterToExpTable;
            firePropertyChange( "clusterToExpTable", oldValue, clusterToExpTable );
        }
        
        private DataElementPath result;
        public DataElementPath getResult()
        {
            return result;
        }
        public void setResult(DataElementPath result)
        {
            Object oldValue = this.result;
            this.result = result;
            firePropertyChange( "result", oldValue, result );
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
            add( DataElementPathEditor.registerInputChild( "experiments", beanClass, ChIPseqExperiment.class ) );
            property("clusters").inputElement( SqlTrack.class ).add();
            add( "metaClusters" );
            property("clusterToExpTable").inputElement( TableDataCollection.class ).add();
            property("result").outputElement( FileDataElement.class ).add();
        }
    }
}
