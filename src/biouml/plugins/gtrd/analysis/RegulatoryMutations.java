package biouml.plugins.gtrd.analysis;


import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NavigableSet;
import java.util.TreeSet;
import java.util.logging.Logger;

import com.developmentontheedge.application.ApplicationUtils;

import biouml.plugins.gtex.meos.GTEXMutationEffectOnSites;
import ru.biosoft.access.core.FolderCollection;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.script.LogScriptEnvironment;
import ru.biosoft.access.script.ScriptDataElement;
import ru.biosoft.access.script.ScriptEnvironment;
import ru.biosoft.access.script.ScriptTypeRegistry;
import ru.biosoft.access.security.SecurityManager;
import ru.biosoft.analysiscore.AbstractAnalysisParameters;
import ru.biosoft.analysiscore.AnalysisJobControl;
import ru.biosoft.analysiscore.AnalysisMethodSupport;
import ru.biosoft.bsa.Site;
import ru.biosoft.bsa.SiteModel;
import ru.biosoft.bsa.SqlTrack;
import ru.biosoft.bsa.Track;
import ru.biosoft.jobcontrol.JobControlEvent;
import ru.biosoft.jobcontrol.JobControlListener;
import ru.biosoft.table.RowDataElement;
import ru.biosoft.table.TableDataCollection;
import ru.biosoft.table.TableDataCollectionUtils;
import ru.biosoft.table.datatype.DataType;
import ru.biosoft.util.TempFile;
import ru.biosoft.util.TempFiles;
import ru.biosoft.util.bean.BeanInfoEx2;

public class RegulatoryMutations extends AnalysisMethodSupport<RegulatoryMutations.Parameters>
{
    
    private static final Logger serverLog = Logger.getLogger( RegulatoryMutations.class.getName() );

    public RegulatoryMutations(DataCollection<?> origin, String name)
    {
        super( origin, name, new Parameters());
    }
    
    @Override
    public Object justAnalyzeAndPut() throws Exception
    {
        Track inputTrack = parameters.getInputVCF().getDataElement(Track.class);
        
        Table featuresTable = buildFeatures(inputTrack);
        File featuresFile = TempFiles.file( ".txt" );
        writeFeaturesTable(featuresTable, featuresFile);
        
        File outFile = TempFiles.file( "_predict.txt" );
        predict( featuresFile, outFile );
        
        
        TableDataCollection result = importTable(outFile, parameters.getOutputFolder().getChildPath( "Regulatory score" ));
        
        featuresFile.delete();
        outFile.delete();
        
        return result;
    }
    
    private TableDataCollection importTable(File inFile, DataElementPath path) throws IOException
    {
        TableDataCollection table = TableDataCollectionUtils.createTableDataCollection( path );
        table.getColumnModel().addColumn( "Score", DataType.Float );
        
        BufferedReader reader = new BufferedReader(new FileReader( inFile ));
        String line;
        while((line = reader.readLine()) != null)
        {
            String[] parts = line.split( "\t" );
            String name = parts[0];
            double score = Double.parseDouble( parts[1] );
            TableDataCollectionUtils.addRow( table, name, new Object[] {score}, false );
        }
        reader.close();
        
        table.finalizeAddition();
        path.save( table );
        return table;
    }

    private void writeFeaturesTable(Table ft, File featuresFile) throws IOException
    {
        try(BufferedWriter writer = new BufferedWriter(new FileWriter( featuresFile )))
        {
            writer.append( String.join( "\t", ft.colNames ) ).append( '\n' );
            for(int i = 0; i < ft.rowNames.size(); i++)
            {
                writer.append( ft.rowNames.get( i ) );
                for(int j = 0; j < ft.columns.size(); j++)
                {
                    Object col = ft.columns.get( j );
                    String val;
                    if(col instanceof double[])
                    {
                        double[] dcol = (double[])col;
                        val = String.valueOf( dcol[i] );
                    }else if(col instanceof int[])
                    {
                        int[] icol = (int[])col;
                        val = String.valueOf( icol[i] );
                    }else if(col instanceof String[])
                    {
                        String[] scol = (String[])col;
                        val = scol[i];
                    }else
                        throw new IllegalArgumentException();
                    writer.append( '\t' ).append( val );
                }
                writer.append( '\n' );
            }
        }
    }

    static class Table
    {
        List<String> rowNames = new ArrayList<>();
        List<String> colNames = new ArrayList<>();
        List<Object> columns = new ArrayList<>();
    }

    private Table buildFeatures(Track inputTrack) throws Exception
    {
        
        Table res = new Table();
        for(Site s : inputTrack.getAllSites())
        {
            res.rowNames.add( getVariationName( s ) );
        }
        Map<String, Integer> mutIndex = new HashMap<>();
        for(int i = 0; i < res.rowNames.size(); i++)
        {
            mutIndex.put(res.rowNames.get( i ), i);
        }
        
        prepareHocomocoFeatures( inputTrack, res, mutIndex );
        prepareSpliceSiteFeatures( inputTrack, res, mutIndex );
        
        return res;
    }
    
    private String getVariationName(Site site)
    {
        String name = site.getProperties().getValueAsString( "name" );
        if(name == null)
            name = site.getName();
        return name;
    }

    public void prepareHocomocoFeatures(Track inputTrack, Table result, Map<String, Integer> mutIndex) throws Exception
    {
        GTEXMutationEffectOnSites gmeos = new GTEXMutationEffectOnSites( null, "" );
        ru.biosoft.bsa.analysis.maos.Parameters params = gmeos.getParameters();
        params.setVcfTrack( parameters.getInputVCF() );
        DataElementPath smPATH = DataElementPath.create( "databases/HOCOMOCO v11/Data/PWM_HUMAN_mono_pval=0.0001" );
        params.setSiteModels( smPATH );
        params.setScoreType( ru.biosoft.bsa.analysis.maos.Parameters.PVALUE_SCORE_TYPE );
        params.setScoreDiff( 0 );
        params.setIndependentVariations( true );
        params.setOneNearestTargetGene( true );
        DataElementPath gmeosOutFolder = parameters.getOutputFolder().getChildPath( "gmeos" );
        params.setOutputTable( gmeosOutFolder.getChildPath( "mutation effects" ) );
        params.setSiteGainTrack( gmeosOutFolder.getChildPath( "site gain" ) );
        params.setSiteLossTrack( gmeosOutFolder.getChildPath( "site loss" ) );
        params.setImportantMutationsTrack( gmeosOutFolder.getChildPath( "important mutations" ) );
        params.setSummaryTable( gmeosOutFolder.getChildPath( "summary" ) );
        
        gmeos.validateParameters();
        gmeos.justAnalyzeAndPut();

        

        List<String> smNames = smPATH.getDataCollection( SiteModel.class ).getNameList();
        Map<String, Integer> smIndex = new HashMap<>();
        for(int i = 0 ;i < smNames.size(); i++)
        {
            smIndex.put(smNames.get( i ), i);
        }
        
        double[][] hocoFeatures = new double[smNames.size()][mutIndex.size()];
        
        TableDataCollection table = gmeosOutFolder.getChildPath( "mutation effects" ).getDataElement( TableDataCollection.class );
        int siteModelColumn = table.getColumnModel().getColumnIndex( "Site model" );
        int mutationColumn = table.getColumnModel().getColumnIndex( "Mutations" );
        int valueColumn = table.getColumnModel().getColumnIndex( "p-value log10 fold change" );
        int typeColumn = table.getColumnModel().getColumnIndex( "Type" );
        for(RowDataElement row : table)
        {
            Object[] values = row.getValues();
            String siteModel = (String)values[siteModelColumn];
            String mutation = (String)values[mutationColumn];
            
            int idx = mutation.lastIndexOf( ':' );
            if(idx != -1)
                mutation = mutation.substring( 0, idx );
            
            String type = (String)values[typeColumn];
            if(!type.equals( "MAX_CHANGE_P0.001" ))
                continue;
            double value = ((Number)values[valueColumn]).doubleValue();
            value = Math.abs( value );
            hocoFeatures[smIndex.get( siteModel )][mutIndex.get( mutation )] = value;
        }
        
        result.colNames.addAll( smNames );
        for(int i = 0; i < smNames.size(); i++)
            result.columns.add( hocoFeatures[i] );
    }
    
    public void prepareSpliceSiteFeatures(Track inputTrack, Table resTable, Map<String, Integer> mutIndex) throws IOException
    {
        String serverPath = java.lang.System.getProperty("biouml.server.path");
        File spliceSitesFile = new File(serverPath + "/gtrd-nosql/splice_sites/splice_sites.txt");
        
        Map<String, NavigableSet<Integer>> spliceSites = new HashMap<>();
        try(BufferedReader reader = new BufferedReader(new FileReader(spliceSitesFile)))
        {
            String line;
            while((line = reader.readLine()) != null)
            {
                String[] parts = line.split("\t");
                String chr = parts[0];
                int pos = Integer.parseInt(parts[1]);
                spliceSites.computeIfAbsent(chr, k->new TreeSet<>()).add(pos);
            }
        }

        double[] result = new double[mutIndex.size()];
        for(Site var : inputTrack.getAllSites())
        {
            String chr = var.getOriginalSequence().getName();
            NavigableSet<Integer> splicePositions = spliceSites.get(chr);
            if(splicePositions == null)
            {
                log.warning("unknown chromosome " + chr);
                continue;
            }
            int distance;
            if(!splicePositions.subSet( var.getFrom(), true, var.getTo(), true ).isEmpty())
            {
                distance = 0;
            }
            else
            {
                Integer floor = splicePositions.floor( var.getFrom() );
                Integer ceiling = splicePositions.ceiling( var.getTo() );
                if(floor == null)
                    distance = ceiling - var.getTo();
                else if(ceiling == null)
                    distance = var.getFrom() - floor;
                else
                {
                    int d1 = var.getFrom() - floor;
                    int d2 = ceiling - var.getTo();
                    distance = d1 < d2 ? d1 : d2;
                }
            }
            String varName = getVariationName( var );
            result[mutIndex.get( varName )] = Math.abs(distance);
        }
        
        resTable.colNames.add( "SPLICE_SITE" );
        resTable.columns.add( result );
    }

    
    private void predict(File featuresFile, File outFile) throws IOException, Exception
    {
        String scriptContent = ApplicationUtils.readAsString( getClass().getResourceAsStream( "resources/regmut_predict.R" ) );

        ScriptEnvironment env = new LogScriptEnvironment(serverLog);

        Map<String, Object> outVars = new HashMap<>();
      
        Map<String, Object> scope = new HashMap<>();
        scope.put( "featuresFile", featuresFile.getAbsolutePath() );
        scope.put( "outFile", outFile.getAbsolutePath() );

        try( TempFile modelFile = TempFiles.file( "model.RData", getClass().getResourceAsStream( "resources/regmut_model.RData" ) ) )
        {
            scope.put( "modelFile", modelFile.getAbsolutePath() );
            SecurityManager.runPrivileged( () -> {
                ScriptDataElement script = ScriptTypeRegistry.createScript( "R", null, scriptContent );
                script.execute( scriptContent, env, scope, outVars, false );
                return null;
            } );
        }
    }

    public static class Parameters extends AbstractAnalysisParameters
    {
        private DataElementPath inputVCF;
        public DataElementPath getInputVCF()
        {
            return inputVCF;
        }
        public void setInputVCF(DataElementPath inputVCF)
        {
            Object oldValue = this.inputVCF;
            this.inputVCF = inputVCF;
            firePropertyChange( "inputVCF", oldValue, inputVCF );
        }

        private DataElementPath outputFolder;
        public DataElementPath getOutputFolder()
        {
            return outputFolder;
        }
        public void setOutputFolder(DataElementPath outputFolder)
        {
            Object oldValue = this.outputFolder;
            this.outputFolder = outputFolder;
            firePropertyChange( "outputFolder", oldValue, outputFolder );
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
            property( "inputVCF" ).inputElement( SqlTrack.class ).add();
            property( "outputFolder" ).outputElement( FolderCollection.class ).expert().add();
        }
    }
}
