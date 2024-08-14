package biouml.plugins.enrichment;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileWriter;
import java.security.InvalidParameterException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import javax.imageio.ImageIO;

import one.util.streamex.IntStreamEx;
import one.util.streamex.StreamEx;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.DataCollectionUtils;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.ImageDataElement;
import ru.biosoft.access.biohub.BioHub;
import ru.biosoft.access.biohub.ReferenceTypeRegistry;
import ru.biosoft.access.exception.BiosoftSQLException;
import ru.biosoft.access.core.ClassIcon;
import ru.biosoft.access.script.LogScriptEnvironment;
import ru.biosoft.access.script.ScriptTypeRegistry;
import ru.biosoft.access.security.SecurityManager;
import ru.biosoft.access.sql.SqlConnectionPool;
import ru.biosoft.analysiscore.AnalysisMethod;
import ru.biosoft.analysiscore.AnalysisMethodSupport;
import ru.biosoft.analysiscore.AnalysisParameters;
import ru.biosoft.analysiscore.AnalysisParametersFactory;
import ru.biosoft.plugins.jri.RUtility;
import ru.biosoft.table.ColumnModel;
import ru.biosoft.table.RowDataElement;
import ru.biosoft.table.StringSet;
import ru.biosoft.table.TableColumn;
import ru.biosoft.table.TableDataCollection;
import ru.biosoft.table.TableDataCollectionUtils;
import ru.biosoft.table.columnbeans.ColumnNameSelector;
import ru.biosoft.util.TempFiles;

@ClassIcon ( "resources/treemap.gif" )
public class TreeMapAnalysis extends AnalysisMethodSupport<TreeMapAnalysisParameters>
{
    private Connection connection = null;
    private Map<String, GroupInfo> groupInfo = null;
    private Map<String, Integer> total = null;

    private static double frequencyCutoff = 0.05;
    private static int sizeLimit = 500;

    public TreeMapAnalysis(DataCollection<?> origin, String name)
    {
        super( origin, name, new TreeMapAnalysisParameters() );
    }

    @Override
    public void validateParameters() throws IllegalArgumentException
    {
        super.validateParameters();
        TreeMapAnalysisParameters params = getParameters();
        DataElementPath inputPath = params.getSourcePath();

        DataElement functionalClassification = inputPath.getDataElement();
        if( ! ( functionalClassification instanceof TableDataCollection ) )
            throw new InvalidParameterException( "This element is not a table" );

        AnalysisParameters parameters = AnalysisParametersFactory.read( functionalClassification );
        if( parameters == null || ! ( parameters instanceof FunctionalClassificationParameters ) )
            throw new InvalidParameterException( "This table is not a result of Functional classification" );

        BioHub bioHub = ( (FunctionalClassificationParameters)parameters ).getBioHub().getBioHub();
        if( ! ( bioHub instanceof FunctionalGOHub ) )
            throw new InvalidParameterException( "This Functional classification is not Gene Ontology" );

        checkNotEmptyCollection( "sourcePath" );

        if( ColumnNameSelector.NONE_COLUMN.equals( params.getPvalueColumn() ) )
            throw new InvalidParameterException( "P-value column must be specified" );

    }

    @Override
    public Object justAnalyzeAndPut() throws Exception
    {
        log.info( "Started" );
        jobControl.pushProgress( 0, 2 );
        TableDataCollection input = getParameters().getSource();

        DataElementPath resultPath = getParameters().getOutputPath();

        if( resultPath.exists() )
            resultPath.remove();
        DataCollection<DataElement> resultsFolder = DataCollectionUtils.createSubCollection( resultPath );

        String column = getParameters().getPvalueColumn();
        ColumnModel model = input.getColumnModel();
        int colIndex = model.optColumnIndex( column );
        double similarityCutoff = getParameters().getSimilarity();
        Map<String, Double> goscores = StreamEx.of( input.stream() )
                .mapToEntry( RowDataElement::getName, rde -> getDouble( rde, colIndex ) ).nonNull()
                .toMap();

        AnalysisMethod method = AnalysisParametersFactory.readAnalysis( input );
        BioHub bioHub = ( (FunctionalClassificationParameters)method.getParameters() ).getBioHub().getBioHub();
        DataElementPath modulePath = bioHub.getModulePath();

        jobControl.popProgress();
        if( jobControl.isStopped() )
            return null;

        jobControl.pushProgress( 2, 80 );
        doGetConnection( modulePath );
        groupInfo = new HashMap<>();
        total = new HashMap<>();
        log.info( "Filling group info" );
        fillGroupInfos( goscores.keySet() );
        reduceInputSize( goscores );

        log.info( "Grouping to clusters" );
        Map<String, List<String>> res = getClusters( goscores, similarityCutoff );
        jobControl.popProgress();

        if( jobControl.isStopped() )
            return null;

        jobControl.pushProgress( 70, 100 );
        log.info( "Generating output..." );
        Object[] results = fillResults( resultsFolder, res, goscores, input );
        jobControl.popProgress();
        log.info( "Done" );
        return results;
    }

    private void reduceInputSize(Map<String, Double> goscores)
    {
        Set<String> top = new HashSet<>();
        for( String type : total.keySet() )
        {
            top.addAll( StreamEx.ofKeys( goscores )
                    .filter( term -> groupInfo.containsKey( term ) && groupInfo.get( term ).getType().equals( type ) )
                    .sorted( Comparator.comparingDouble( term -> goscores.get( term ) ) ).limit( sizeLimit ).toSet() );
        }
        if( top.size() < goscores.size() )
        {
            log.info( "Input group list is too long, only " + sizeLimit + " groups with better p-value will be taken for each category." );
            goscores.keySet().retainAll( top );
        }
    }

    private double getDouble(RowDataElement rde, int index)
    {
        return Double.parseDouble( rde.getValues()[index].toString() );
    }

    private Object[] fillResults(DataCollection<DataElement> resultsFolder, Map<String, List<String>> clusters,
            Map<String, Double> goscores, TableDataCollection input) throws Exception
    {
        jobControl.pushProgress( 2, 60 );
        TableDataCollection resTable = TableDataCollectionUtils
                .createTableDataCollection( resultsFolder.getCompletePath().getChildPath( "treemap summary table" ) );
        ColumnModel columnModel = resTable.getColumnModel();
        columnModel.addColumn( "Title", String.class );
        columnModel.addColumn( "Ontology type", String.class );
        columnModel.addColumn( "Hierarchical #", String.class ).setSorting( TableColumn.SORTING_ASCENT );
        TableColumn parentCol = columnModel.addColumn( "Parent in cluster", String.class );
        columnModel.addColumn( "Level", String.class );
        columnModel.addColumn( "Frequency in %", Double.class );
        columnModel.addColumn( "-log10(P-value)", Double.class );

        ColumnModel model = input.getColumnModel();
        int[] hitcolumns = IntStreamEx.range( model.getColumnCount() )
                .filter( i -> model.getColumn( i ).getValueClass() == StringSet.class ).toArray();
        if( hitcolumns.length > 1 )
        {
            columnModel.addColumn( "Hits", StringSet.class ).setHidden( true );
            columnModel.addColumn( "Hit names", StringSet.class );
        }

        int size = clusters.size();
        int cnt = 0;
        Map<String, AtomicInteger> groupCounter = new HashMap<>();
        Map<String, List<String>> rdata = new HashMap<>();
        Iterator<String> iter = clusters.keySet().stream().sorted( Comparator.comparingDouble( term -> goscores.get( term ) ) ).iterator();
        int groupLimit = getParameters().getDisplayLimit();
        while( iter.hasNext() )
        {
            String term = iter.next();
            cnt++;
            GroupInfo gi = groupInfo.get( term );
            String type = gi.getType();
            List<String> curRData = rdata.computeIfAbsent( type, t -> new ArrayList<>() );
            AtomicInteger curGroupCounter = groupCounter.computeIfAbsent( type, t -> new AtomicInteger() );
            boolean addToScript = curGroupCounter.incrementAndGet() <= groupLimit;
            //We assume input table has two StringSet columns: hits and hit names. Copy both of them to result set. Add nothing if no enough hits column found in input
            Object[] hits = new Object[] {null, null};
            Object[] inputValues;
            if( hitcolumns.length > 1 )
            {
                inputValues = input.get( term ).getValues();
                hits = new Object[] {inputValues[hitcolumns[0]], inputValues[hitcolumns[1]]};
            }
            String clustNum = Integer.toString( cnt );
            String groupScriptData = addData( term, goscores.get( term ), term, hits, clustNum, resTable );
            if( addToScript )
                curRData.add( groupScriptData );

            List<String> cl = clusters.get( term );
            int cntInCluster = 0;
            for( String term2 : cl )
            {
                cntInCluster++;
                if( hitcolumns.length > 1 )
                {
                    inputValues = input.get( term2 ).getValues();
                    hits = new Object[] {inputValues[hitcolumns[0]], inputValues[hitcolumns[1]]};
                }
                String scriptData = addData( term2, goscores.get( term2 ), term, hits, clustNum + "." + Integer.toString( cntInCluster ),
                        resTable );
                if( addToScript )
                    curRData.add( scriptData );
            }

            jobControl.setPreparedness( 100 * cnt / size );
            if( jobControl.isStopped() )
            {
                resTable.getCompletePath().remove();
                return null;
            }
        }
        resTable.setReferenceType( input.getReferenceType() );
        resTable.setSpecies( input.getSpecies() );
        parentCol.setValue( ReferenceTypeRegistry.REFERENCE_TYPE_PROPERTY, input.getReferenceType() );
        jobControl.popProgress();

        jobControl.pushProgress( 60, 98 );
        List<DataElement> results = new ArrayList<>();
        results.add( resTable );
        for( Map.Entry<String, List<String>> entry : rdata.entrySet() )
        {
            String type = entry.getKey();
            File rOutput = TempFiles.file( type );
            rOutput.delete();
            String outputFilePath = RUtility.escapeRString( rOutput.getAbsolutePath() );

            File rInput = TempFiles.file( type );
            rInput.delete();
            String inputFilePath = RUtility.escapeRString( rInput.getAbsolutePath() );
            try( FileWriter fw = new FileWriter( rInput ) )
            {
                for( String str : entry.getValue() )
                {
                    fw.write( str );
                    fw.write( "\n" );
                }
            }

            StringBuilder sb = new StringBuilder();
            sb.append( getRCommand( inputFilePath, outputFilePath, type ) );

            File rOutputScript = TempFiles.file( type + "_script" );
            try (FileWriter fw = new FileWriter( rOutputScript ))
            {
                fw.write( sb.toString() );
            }

            executeRScript( sb.toString() );

            BufferedImage treemapImage = ImageIO.read( rOutput );
            String imageName = type;
            ImageDataElement image = new ImageDataElement( imageName, resultsFolder, treemapImage );
            resultsFolder.put( image );
            results.add( image );
        }
        jobControl.popProgress();

        resultsFolder.put( resTable );
        return results.toArray();
    }

    private String addData(String term, double pvalue, String parentTerm, Object[] hits, String clustNum, TableDataCollection resTable)
            throws Exception
    {
        GroupInfo gi = groupInfo.get( term );
        GroupInfo gip = groupInfo.get( parentTerm );

        if( hits != null )
        TableDataCollectionUtils.addRow( resTable, term,
                    new Object[] {gi.getTitle(), gi.getType(), clustNum, parentTerm, gi.getLevel(),
                            100 * gi.getFrequency(),
                            -Math.log10( pvalue ), hits[0], hits[1]} );
        else
            TableDataCollectionUtils.addRow( resTable, term,
                    new Object[] {gi.getTitle(), gi.getType(), parentTerm, 100 * gi.getFrequency(), -Math.log10( pvalue )} );
        return "\"" + term + "\"\t" + "\"" + gi.getTitle() + "\"\t" + 100 * gi.getFrequency() + "\t" + -Math.log10( pvalue ) + "\t" + "\""
                + gip.getTitle() + "\"";
    }

    private void executeRScript(String rCommand) throws Exception
    {
        final LogScriptEnvironment env = new LogScriptEnvironment( log );
        env.reportErrorsAsWarnings( true );
        log.info( "Invoking R command (that will take some time)..." );
        SecurityManager.runPrivileged( () -> ScriptTypeRegistry.execute( "R", rCommand, env, false ) );
    }

    private String getRCommand(String inputFileName, String outputFileName, String title)
    {
        String tmIndex, tmFont, tmAlign, tmInflate;
        if( parameters.isRepresentativeOnly() )
        {
            tmIndex = "c(\"representative\")";
            tmFont = "c(\"#000000\")";
            tmInflate = "TRUE";
            tmAlign = "list(c(\"center\", \"center\"))";
        }
        else
        {
            tmIndex = "c(\"representative\",\"description\")";
            tmFont = "c(\"#000000\",\"#555555\")";
            tmInflate = "FALSE";
            tmAlign = "list(c(\"center\", \"bottom\"), c(\"center\", \"top\"))";
        }
        String rCommand = "library(treemap)\n"
                + "biouml.names <- c(\"term_ID\",\"description\",\"freqInDbPercent\",\"abslog10pvalue\",\"representative\");\n"
                + "biouml.data = read.table(\"" + inputFileName + "\");\n" + "stuff <- data.frame(biouml.data);\n"
                + "names(stuff) <- biouml.names;\n"
                + "stuff$abslog10pvalue <- as.numeric( as.character(stuff$abslog10pvalue) );\n"
                + "stuff$freqInDbPercent <- as.numeric( as.character(stuff$freqInDbPercent) );\n" + "png( file=\"" + outputFileName
                + "\", width=1200, height=800) # width and height are in pixels\n" + "treemap(\n"
                + "stuff,\n"
                //+ "index = c(\"representative\",\"description\"),\n" + "vSize = \"abslog10pvalue\",\n" + "type = \"categorical\",\n"
                + "index = " + tmIndex + ",\n" + "vSize = \"abslog10pvalue\",\n" + "type = \"categorical\",\n"
                + "vColor = \"representative\",\n" + "title = \"" + title + " Gene Ontology treemap\",\n" + "fontsize.labels=c(22,12),\n"
                + "palette=\"Set3\","
                //+ "fontcolor.labels=c(\"#000000\",\"#555555\"),\n"
                + "fontcolor.labels=" + tmFont + ",\n"
                //+ "inflate.labels = FALSE,      # set this to TRUE for space-filling group labels - good for posters\n"
                + "inflate.labels = " + tmInflate + ",      # set this to TRUE for space-filling group labels - good for posters\n"
                + "lowerbound.cex.labels = 0,   # try to draw as many labels as possible (still, some small squares may not get a label)\n"
                + "bg.labels = \"#CCCCCC00\",   # define background color of group labels\n" + "position.legend = \"none\",\n"
                //+ "align.labels=list(c(\"center\", \"bottom\"), c(\"center\", \"top\")),\n" + "ymod.labels=c(0, -0.1)\n)\n"
                + "align.labels=" + tmAlign + ",\n" + "ymod.labels=c(0, -0.1)\n)\n"
                + "invisible(dev.off());\n";
        return rCommand;
    }

    private void doGetConnection(DataElementPath path) throws BiosoftSQLException
    {
        if( path != null )
        {
            connection = SqlConnectionPool.getConnection( path.getDataCollection() );
        }
    }

    private Map<String, List<String>> getClusters(Map<String, Double> goscores, double cutoff) throws Exception
    {

        Map<String, List<String>> clusters = new HashMap<>();

        List<String> terms = new ArrayList<>( goscores.keySet() );


        jobControl.setPreparedness( 10 );

        log.info( "Caluculating matrix of pairwise semantic similarity" );
        int n = terms.size();
        double[][] distMatrix = new double[n][n];
        calcSimilarity( terms, distMatrix );
        jobControl.setPreparedness( 50 );

        log.info( "Choosing cluster representatives" );
        for( String t : terms )
        {
            List<String> cl = new ArrayList<>();
            clusters.put( t, cl );
        }

        while( true )
        {
            int[] x = findMax( distMatrix );
            double v = distMatrix[x[0]][x[1]];
            if( v < cutoff )
            {
                break;
            }
            else
            {
                String term1 = terms.get( x[0] );
                String term2 = terms.get( x[1] );
                GroupInfo gi1 = groupInfo.get( term1 );
                GroupInfo gi2 = groupInfo.get( term2 );
                double f1 = gi1.getFrequency();
                double f2 = gi2.getFrequency();
                String parent = null, child = null;
                int index = 0;
                //1) reject term with broad interpretation (frequency > 5%)
                if( f1 > frequencyCutoff )
                {//remove term1
                    parent = term2;
                    child = term1;
                    index = x[0];
                }
                else if( f2 > frequencyCutoff )
                {//remove term2
                    parent = term1;
                    child = term2;
                    index = x[1];
                }
                else
                {
                    //2) reject term with less significant p-value
                    if( goscores.get( term1 ) < goscores.get( term2 ) )
                    {//remove term2
                        parent = term1;
                        child = term2;
                        index = x[1];
                    }
                    else if( goscores.get( term1 ) > goscores.get( term2 ) )
                    {
                        //remove term1
                        parent = term2;
                        child = term1;
                        index = x[0];
                    }
                    else
                    {
                        //reject child term
                        if( gi2.getParents().contains( term1 ) ) //term1 is parent of term2, reject term2
                        {
                            if( gi2.getFrequency() / gi1.getFrequency() > 0.75 ) //parent group consist primarily of child term, reject parent
                            {
                                //reject term1
                                parent = term2;
                                child = term1;
                                index = x[0];
                            }
                            else
                            {
                                parent = term1;
                                child = term2;
                                index = x[1];
                            }
                        }
                        else if( gi1.getParents().contains( term2 ) ) //term2 is parent of term1, reject term1
                        {
                            if( gi1.getFrequency() / gi2.getFrequency() > 0.75 ) //parent group consist primarily of child term, reject parent
                            {
                                //reject term2
                                parent = term1;
                                child = term2;
                                index = x[1];
                            }
                            else
                            {
                                parent = term2;
                                child = term1;
                                index = x[0];
                            }
                        }
                        else //reject at random, now reject term1
                        {
                            parent = term2;
                            child = term1;
                            index = x[0];
                        }
                    }
                }
                List<String> cl = clusters.get( parent );
                cl.add( child );
                cl.addAll( clusters.get( child ) );
                clusters.remove( child );
                for( int k = 0; k < n; k++ )
                {
                    distMatrix[k][index] = 0.0;
                    distMatrix[index][k] = 0.0;
                }
            }
        }
        jobControl.setPreparedness( 100 );
        return clusters;
    }

    private void fillGroupInfos(Collection<String> terms) throws SQLException
    {
        List<String> requiredTerms = terms.stream().filter( e -> !groupInfo.containsKey( e ) ).collect( Collectors.toList() );
        if( requiredTerms.isEmpty() )
            return;
        String groupInfoInitQuery = "SELECT id, name, term_type FROM term WHERE acc=?";
        String ancestorAccQuery = "SELECT DISTINCT ancestor.acc FROM term AS ancestor INNER JOIN is_a_flat ON ancestor.id=is_a_flat.term1_id "
                + "INNER JOIN graph_path USING(term1_id, term2_id) WHERE term2_id=? AND ancestor.acc != 'all' GROUP BY ancestor.acc";
        String minDistanceQuery = "SELECT min(distance) FROM graph_path INNER JOIN term ON term.id=term1_id WHERE term2_id=? AND name=?";
        try( PreparedStatement groupInfoPS = connection.prepareStatement( groupInfoInitQuery );
                PreparedStatement ancestorAccPS = connection.prepareStatement( ancestorAccQuery );
                PreparedStatement minDistancePS = connection.prepareStatement( minDistanceQuery ); )
        {
            GroupInfo g;
            String type = null;
            String species = getParameters().getSpecies().getLatinName();
            for( String term : requiredTerms )
            {
                if( groupInfo.containsKey( term ) )
                    continue;
                groupInfoPS.setString( 1, term );
                try( ResultSet rs = groupInfoPS.executeQuery() )
                {
                    if( rs.next() )
                    {
                        type = rs.getString( 3 );
                        g = new GroupInfo( rs.getInt( 1 ), term, type, rs.getString( 2 ) );
                    }
                    else
                    {
                        g = null;
                        log.warning( "Cannot find group for '" + term + "'." );//TODO: fix message
                    }
                }
                if( g == null )
                    continue;

                //fill parent count
                if( !total.containsKey( g.getType() ) )
                {
                    String cntIdQuery = "SELECT count(distinct ensembl_id) FROM BioUML_groups_" + type + " WHERE species='" + species + "'";
                    try( PreparedStatement ps = connection.prepareStatement( cntIdQuery ); ResultSet rs = ps.executeQuery() )
                    {
                        if( rs.next() )
                            total.put( type, rs.getInt( 1 ) );
                    }
                }

                String countQuery = "SELECT count(*) FROM BioUML_groups_" + type + " WHERE pathway_id=? AND species='" + species + "'";
                try( PreparedStatement ps = connection.prepareStatement( countQuery ) )
                {
                    ps.setString( 1, term );
                    try( ResultSet rs = ps.executeQuery() )
                    {
                        if( rs.next() )
                            g.setSize( rs.getDouble( 1 ) / total.get( type ) );
                    }
                }

                //parents with distance
                List<String> parents = new ArrayList<>();
                ancestorAccPS.setInt( 1, g.getId() );
                try( ResultSet rs = ancestorAccPS.executeQuery() )
                {
                    while( rs.next() )
                    {
                        parents.add( rs.getString( 1 ) );
                    }
                }
                g.setParents( parents );

                minDistancePS.setInt( 1, g.getId() );
                minDistancePS.setString( 2, type );
                try( ResultSet rs = minDistancePS.executeQuery() )
                {
                    if( rs.next() )
                        g.setLevel( rs.getInt( 1 ) );
                }

                groupInfo.put( term, g );
            }
        }
    }

    private int[] findMax(double[][] matrix)
    {
        int maxi = 0, maxj = 0;
        double maxval = 0.0;
        for( int i = 0; i < matrix.length; i++ )
        {
            for( int j = 0; j < i; j++ )
            {
                if( matrix[i][j] > maxval )
                {
                    maxval = matrix[i][j];
                    maxi = i;
                    maxj = j;
                }
            }
        }
        return new int[] {maxi, maxj};
    }

    private void calcSimilarity(List<String> all, double[][] matrix) throws Exception
    {
        int n = all.size();
        for( int i = 0; i < n; i++ )
        {
            for( int j = 0; j < i; j++ )
            {
                String term1 = all.get( i );
                String term2 = all.get( j );
                double d = calcDistance( term1, term2 );
                matrix[i][j] = d;
                matrix[j][i] = d;
            }
        }
    }

    private double calcDistance(String term1, String term2) throws Exception
    {
        double value = 0.0;
        if( groupInfo.get( term1 ).getType().equals( groupInfo.get( term2 ).getType() ) )
        {
            Set<String> ancs = commonAncestors( term1, term2 );
            fillGroupInfos( ancs );
            value = StreamEx.of( ancs ).mapToDouble( v -> getSimRel( v, term1, term2 ) ).filter( v -> v < 1.0 ).max().orElse( 0.0 );
        }
        return value;
    }

    private double getSimRel(String parent, String child1, String child2)
    {
        double probability = getProbability( parent );
        return 2.0 * Math.log( probability ) * ( 1.0 - probability )
                / ( Math.log( getProbability( child1 ) ) + Math.log( getProbability( child2 ) ) );
    }

    private double getProbability(String term)
    {
        return groupInfo.containsKey( term ) ? groupInfo.get( term ).getFrequency() : 0.0;
    }

    private Set<String> commonAncestors(String go1, String go2) throws Exception
    {
        GroupInfo g1 = groupInfo.get( go1 );
        GroupInfo g2 = groupInfo.get( go2 );
        if( !g1.getType().equals( g2.getType() ) )
            return null;

        List<String> par1 = g1.getParents();
        List<String> par2 = g2.getParents();

        //common terms
        Set<String> ancs = new HashSet<>( par1 );
        ancs.retainAll( par2 );
        return ancs;
    }

    private static class GroupInfo
    {
        private int id;
        private String accession;
        private String type;
        private String title;
        private double frequency;
        private int level; //GO depth level, calculate by distance to top category term
        private List<String> parents;

        public GroupInfo(int id, String accession, String type, String title)
        {
            this.id = id;
            this.accession = accession;
            this.type = type;
            this.title = title;
        }

        public int getId()
        {
            return id;
        }

        public double getFrequency()
        {
            return frequency;
        }
        public void setSize(double size)
        {
            this.frequency = size;
        }
        public List<String> getParents()
        {
            return parents;
        }
        public void setParents(List<String> parents)
        {
            this.parents = parents;
        }
        public String getType()
        {
            return type;
        }
        public void setType(String type)
        {
            this.type = type;
        }

        public String getTitle()
        {
            return title;
        }

        public void setTitle(String title)
        {
            this.title = title;
        }
        public int getLevel()
        {
            return level;

        }
        public void setLevel(int level)
        {
            this.level = level;
        }
    }

}
