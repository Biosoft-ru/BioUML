package biouml.plugins.mirprom;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.stream.Collectors;

import com.developmentontheedge.beans.DynamicProperty;
import com.developmentontheedge.beans.DynamicPropertySet;
import com.developmentontheedge.beans.DynamicPropertySetAsMap;
import com.developmentontheedge.beans.annot.PropertyDescription;
import com.developmentontheedge.beans.annot.PropertyName;
import com.eclipsesource.json.JsonObject;

import biouml.plugins.ensembl.access.EnsemblDatabase;
import biouml.plugins.ensembl.access.EnsemblDatabaseSelector;
import one.util.streamex.StreamEx;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.DataCollectionUtils;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.core.ClassIcon;
import ru.biosoft.analysiscore.AbstractAnalysisParameters;
import ru.biosoft.analysiscore.AnalysisMethodSupport;
import ru.biosoft.bsa.AnnotatedSequence;
import ru.biosoft.bsa.Basis;
import ru.biosoft.bsa.Precision;
import ru.biosoft.bsa.Sequence;
import ru.biosoft.bsa.Site;
import ru.biosoft.bsa.SiteImpl;
import ru.biosoft.bsa.SiteType;
import ru.biosoft.bsa.SqlTrack;
import ru.biosoft.bsa.StrandType;
import ru.biosoft.exception.ExceptionRegistry;
import ru.biosoft.table.TableDataCollection;
import ru.biosoft.util.JsonUtils;
import ru.biosoft.util.Pair;
import ru.biosoft.util.bean.BeanInfoEx2;
import ru.biosoft.util.bean.StaticDescriptor;

@ClassIcon ( "resources/create_mirna_promoters.gif" )
public class CreateMiRNAPromoters extends AnalysisMethodSupport<CreateMiRNAPromoters.Parameters>
{
    private static final StaticDescriptor MIRNA_DESCRIPTOR = StaticDescriptor.create( "miRNA" );
    private static String MIRPROM_DEFAULT_PATH = null;

    private static String CELL_LINE_QUERY = "SELECT DISTINCT name FROM cell";
    private static String CELL_TYPE_QUERY = "SELECT DISTINCT tissue FROM cell";
    private final List<String> canonicalChr = Arrays.asList( "1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11", "12", "13", "14",
            "15", "16", "17", "18", "19", "20", "21", "22", "X", "Y", "MT" );
    private static List<String> cellLines = null;
    private static List<String> cellTypes = null;

    public CreateMiRNAPromoters(DataCollection<?> origin, String name)
    {
        super( origin, name, new Parameters() );

    }

    @Override
    public Object justAnalyzeAndPut() throws Exception
    {
        initConfiguration();
        DataElementPath dbPath = DataElementPath.create( MIRPROM_DEFAULT_PATH );
        if(!dbPath.exists())
            throw new Exception("MiRProm not installed on the server");

        String genomeBuild = dbPath.getDataCollection().getInfo().getProperty( "genomeBuild" );
        if(genomeBuild == null)
            throw new Exception("Unknown genome build for installed MirProm");

        EnsemblDatabase ensembl = StreamEx.of( EnsemblDatabaseSelector.getEnsemblDatabases() )
                .filter( db -> genomeBuild.equals( db.getGenomeBuild() ) )
                .maxBy( db -> db.getVersion() )
                .orElseThrow( () -> new Exception( "Ensembl for " + genomeBuild + " not found" ) );

        List<String> miRNANames = parameters.getInTable().getDataCollection().getNameList();
        Map<String, List<TSS>> tssResults = loadTSSFromMirprom( miRNANames, dbPath );

        if( parameters.isUseEnsembl() )
        {
            Set<String> missingMiRNAs = new HashSet<>( miRNANames );
            missingMiRNAs.removeAll( tssResults.keySet() );
            Map<String, List<TSS>> ensemblTSS = loadTSSFromEnsembl( missingMiRNAs, ensembl );

            tssResults.putAll( ensemblTSS );
        }

        SqlTrack result = SqlTrack.createTrack( parameters.getOutTrack(), null, ensembl.getPrimarySequencesPath() );

        for(Map.Entry<String, List<TSS>> e : tssResults.entrySet())
        {
            String miRNA = e.getKey();
            List<TSS> tssList = e.getValue();
            tssList = selectTSS(tssList);
            tssList = filterByCell( tssList );
            if( !checkTSSList( tssList, miRNA ) )
                continue;
            for(TSS tss : tssList)
            {
                Site site = createSite( tss, miRNA, ensembl );
                if( site == null )
                    continue;
                result.addSite( site );
            }
        }

        result.finalizeAddition();
        parameters.getOutTrack().save( result );
        return result;
    }

    private List<TSS> selectTSS(List<TSS> tssList)
    {
        if( parameters.getPromoterSelectMode().equals( Parameters.SELECT_ALL ) )
            return tssList;
        TSS first = tssList.get( 0 );
        Comparator<TSS> byPos = Comparator.comparingInt( tss->tss.pos );
        if( (parameters.getPromoterSelectMode().equals( Parameters.SELECT_5_PRIME ) && first.forwardStrand)
         || (parameters.getPromoterSelectMode().equals( Parameters.SELECT_3_PRIME ) && !first.forwardStrand) )
            return Collections.singletonList( Collections.min( tssList, byPos ) );
        else
            return Collections.singletonList( Collections.max( tssList, byPos ) );
    }

    private List<TSS> filterByCell(List<TSS> tssList)

    {
        if( parameters.getCellSelection().equals( Parameters.ALL_TYPES_SELECTED ) )
            return tssList;
        //Filter tss without cell information from Ensembl
        List<TSS> result = StreamEx.of( tssList ).filter( tss -> tss.cells.isEmpty() ).toList();
        if( parameters.getCellSelection().equals( Parameters.CELL_LINE_SELECTED ) )
        {
            String cl = parameters.getCellLine();
            result.addAll( StreamEx.of( tssList ).filter( tss -> StreamEx.of( tss.cells ).anyMatch( cell -> cell.getFirst().equals( cl ) ) )
                    .toList() );

        }
        else if( parameters.getCellSelection().equals( Parameters.CELL_TYPE_SELECTED ) )
        {
            String ct = parameters.getCellType();
            result.addAll( StreamEx.of( tssList )
                    .filter( tss -> StreamEx.of( tss.cells ).anyMatch( cell -> cell.getSecond().equals( ct ) ) ).toList() );
        }
        return result;
    }

    private static class TSS
    {
        String chr;
        int pos;
        boolean forwardStrand;
        List<Pair<String, String>> cells;
        TSS(String chr, int pos, boolean forwardStrand)
        {
            this.chr = chr;
            this.pos = pos;
            this.forwardStrand = forwardStrand;
            this.cells = new ArrayList<>();
        }
        public void addCell(List<Pair<String, String>> cells)
        {
            this.cells.addAll( cells );
        }
    }

    private Map<String, List<TSS>> loadTSSFromMirprom(Collection<String> miRNANames, DataElementPath dbPath) throws Exception
    {
        Connection con = DataCollectionUtils.getSqlConnection( dbPath.getDataCollection() );
        Map<String, List<TSS>> result = new HashMap<>();
        String cellQuery = "SELECT name, tissue FROM cell JOIN promoter2cell ON (cell.id=cell_id) WHERE promoter2cell.promoter_id=?";

        try (PreparedStatement ps = con
                .prepareStatement(
                        "SELECT promoter.id,chrom,position,strand FROM promoter JOIN mirna ON(mirna_id=mirna.id) WHERE mirna.name=?" ))
        {

            for( String miRNAName : miRNANames )
            {
                List<TSS> tssList = new ArrayList<>();
                ps.setString( 1, miRNAName );
                try (ResultSet rs = ps.executeQuery())
                {
                    while( rs.next() )
                    {
                        int promoterId = rs.getInt( 1 );
                        String chr = rs.getString( 2 );
                        int pos = rs.getInt( 3 );
                        String curStrandStr = rs.getString( 4 );
                        TSS tss = new TSS( chr, pos, curStrandStr.equals( "+" ) );
                        List<Pair<String, String>> cells = new ArrayList<>();
                        try (PreparedStatement ps2 = con.prepareStatement( cellQuery ))
                        {
                            ps2.setInt( 1, promoterId );
                            try (ResultSet rs2 = ps2.executeQuery())
                            {
                                while( rs2.next() )
                                {
                                    String line = rs2.getString( 1 );
                                    String type = rs2.getString( 2 );
                                    cells.add( new Pair( line, type ) );
                                }
                            }
                        }
                        tss.addCell( cells );
                        tssList.add( tss );
                    }

                    if( tssList.isEmpty() || !checkTSSList( tssList, miRNAName ) )
                        continue;

                    result.put( miRNAName, tssList );
                }

            }
        }
        return result;
    }

    private Map<String, List<TSS>> loadTSSFromEnsembl(Set<String> missingMiRNAs, EnsemblDatabase ensembl) throws Exception
    {
        Map<String, List<TSS>> result = new HashMap<>();
        String query = "SELECT seq_region.name, seq_region_start, seq_region_end, seq_region_strand FROM transcript"
                + " JOIN object_xref on(ensembl_id=gene_id)"
                + " JOIN xref USING(xref_id) JOIN external_db USING(external_db_id)"
                + " JOIN seq_region using(seq_region_id)"
                + " WHERE db_name='miRBase' AND ensembl_object_type='Gene' AND display_label=?";
        Connection con = ensembl.getConnection();
        try(PreparedStatement ps = con.prepareStatement( query ))
        {
            for( String name : missingMiRNAs )
            {
                ps.setString( 1, name );
                List<TSS> tssList = new ArrayList<>();
                try (ResultSet rs = ps.executeQuery())
                {
                    while(rs.next())
                    {
                        String chr = rs.getString( 1 );
                        int start = rs.getInt( 2 );
                        int end = rs.getInt( 3 );
                        int strand = rs.getInt( 4 );
                        boolean forwardStrand = strand == 1;
                        TSS tss = new TSS( chr, forwardStrand ? start : end, forwardStrand );
                        tssList.add( tss );
                    }
                    if( tssList.isEmpty() || !checkTSSList( tssList, name ) )
                        continue;
                    result.put( name, tssList );
                }
            }
        }
        return result;
    }

    private boolean checkTSSList(List<TSS> tssList, String miRNAName) throws Exception
    {
        log.info( "Checking TSS for " + miRNAName );
        if(tssList.isEmpty())
            return true;
        TSS first = tssList.get( 0 );
        for(int i = 1; i < tssList.size(); i++)
        {
            TSS tss = tssList.get( i );
            if(!tss.chr.equals( first.chr ))
            {
                log.log(Level.SEVERE,  "Promoters of " + miRNAName + " on distinct chromosomes, would skip" );
                return false;
            }
            if(tss.forwardStrand != first.forwardStrand)
            {
                log.log(Level.SEVERE,  "Promoters of " + miRNAName + " on distinct strands, would skip" );
                return false;
            }
            if( !canonicalChr.contains( toEnsemblChr( tss.chr ) ) )
            {
                log.log(Level.SEVERE,  "Promoters of " + miRNAName + " on non-canonical chromosome " + toEnsemblChr( tss.chr ) + ", would skip" );
                return false;
            }
        }
        return true;
    }

    private Site createSite(TSS tss, String miRNAName, EnsemblDatabase ensembl)
    {
        int strand = tss.forwardStrand ? StrandType.STRAND_PLUS : StrandType.STRAND_MINUS;
        try{
        DataElementPath chrPath = ensembl.getPrimarySequencesPath().getChildPath( toEnsemblChr( tss.chr ) );
        Sequence seq = chrPath.getDataElement( AnnotatedSequence.class ).getSequence();
        int start = strand == StrandType.STRAND_PLUS ? tss.pos + parameters.getFrom() : tss.pos - parameters.getFrom();
        int length = parameters.getTo() - parameters.getFrom() + 1;
        DynamicPropertySet properties = new DynamicPropertySetAsMap();
        properties.add( new DynamicProperty( MIRNA_DESCRIPTOR, String.class, miRNAName ) );
        if( !tss.cells.isEmpty() )
        {
            String lines = StreamEx.of( tss.cells ).map( p -> p.getFirst() ).distinct().collect( Collectors.joining( "; " ) );
            String types = StreamEx.of( tss.cells ).map( p -> p.getSecond() ).distinct().collect( Collectors.joining( "; " ) );
            properties.add( new DynamicProperty( "Cell_line", String.class, lines ) );
            properties.add( new DynamicProperty( "Tissue", String.class, types ) );
        }
        return new SiteImpl( null, seq.getName(), SiteType.TYPE_PROMOTER, Basis.BASIS_ANNOTATED, start, length, Precision.PRECISION_EXACTLY, strand, seq, properties  );
        }
        catch( Exception e )
        {
            log.log(Level.SEVERE,  "Promoters of " + miRNAName + " on non-canonical chromosome " + toEnsemblChr( tss.chr ) + ", would skip" );
        }
        return null;
    }

    private String toEnsemblChr(String chr)
    {
        if( chr.startsWith("chr") )
            chr = chr.substring("chr".length());
        if( chr.equals( "M" ) )
            chr = "MT";
        return chr;
    }

    private static void initConfiguration()
    {
        try
        {
            String confFile = System.getProperty( "biouml.server.path" ) + "/appconfig/mirprom/CreateMiRNAPromoters.json";
            JsonObject json = JsonUtils.fromFile( confFile );
            MIRPROM_DEFAULT_PATH = json.getString( "database", null );
        }
        catch( Exception e )
        {
            throw new IllegalArgumentException(
                    "Analysis 'CreateMiRNAPromoters' could not be run on this server. Can not read configuration file. Please contact server administrator." );
        }
    }

    public static String[] getCellLines()
    {
        if( cellLines == null )
            try
            {
                initLists();
            }
            catch( Exception e )
            {
                return new String[0];
            }

        return cellLines.toArray( new String[0] );
    }

    public static String[] getCellTypes()
    {
        if( cellTypes == null )
            try
            {
                initLists();
            }
            catch( Exception e )
            {
                return new String[0];
            }

        return cellTypes.toArray( new String[0] );
    }

    private static void initLists() throws Exception
    {
        if( MIRPROM_DEFAULT_PATH == null )
            initConfiguration();
        DataElementPath dbPath = DataElementPath.create( MIRPROM_DEFAULT_PATH );
        if( !dbPath.exists() )
            throw new Exception( "MiRProm not installed on the server" );

        Connection connection = DataCollectionUtils.getSqlConnection( dbPath.getDataCollection() );

        try (Statement st = connection.createStatement(); ResultSet rs = st.executeQuery( CELL_LINE_QUERY ))
        {
            cellLines = new ArrayList<>();
            while( rs.next() )
            {
                cellLines.add( rs.getString( 1 ) );
            }

        }
        catch( SQLException e )
        {
            ExceptionRegistry.log( e );
        }

        try (Statement st = connection.createStatement(); ResultSet rs = st.executeQuery( CELL_TYPE_QUERY ))
        {
            cellTypes = new ArrayList<>();
            while( rs.next() )
            {
                cellTypes.add( rs.getString( 1 ) );
            }
        }
        catch( SQLException e )
        {
            ExceptionRegistry.log( e );
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
            property("inTable").inputElement( TableDataCollection.class ).add();
            add("from");
            add("to");
            addHidden( "modeSuffix" );
            property("promoterSelectMode").tags( Parameters.SELECT_ALL, Parameters.SELECT_5_PRIME, Parameters.SELECT_3_PRIME ).add();
            property( "cellSelection" ).tags( Parameters.CELL_LINE_SELECTED, Parameters.CELL_TYPE_SELECTED, Parameters.ALL_TYPES_SELECTED )
                    .structureChanging().add();
            property( "cellLine" ).hidden( "isCellLineHidden" ).tags( CreateMiRNAPromoters.getCellLines() ).add();
            property( "cellType" ).hidden( "isCellTypeHidden" ).tags( CreateMiRNAPromoters.getCellTypes() ).add();
            addHidden( "cellSuffix" );
            add( "useEnsembl" );
            property( "outTrack" ).outputElement( SqlTrack.class ).auto( "$inTable$ promoters$modeSuffix$$cellSuffix$" ).add();
        }
    }

    public static class Parameters extends AbstractAnalysisParameters
    {
        private DataElementPath inTable;

        @PropertyName("miRNA table")
        @PropertyDescription("Table with miRBase identifiers")
        public DataElementPath getInTable()
        {
            return inTable;
        }
        public void setInTable(DataElementPath inTable)
        {
            Object oldValue = this.inTable;
            this.inTable = inTable;
            firePropertyChange( "inTable", oldValue, inTable );
        }

        private int from = -1000, to = 100;

        @PropertyName("Promoter start relative to TSS")
        public int getFrom()
        {
            return from;
        }
        public void setFrom(int from)
        {
            int oldValue = this.from;
            this.from = from;
            firePropertyChange( "from", oldValue, from );
        }

        @PropertyName("Promoter end relative to TSS")
        public int getTo()
        {
            return to;
        }
        public void setTo(int to)
        {
            int oldValue = this.to;
            this.to = to;
            firePropertyChange( "to", oldValue, to );
        }

        public static final String SELECT_ALL="all", SELECT_5_PRIME="5' most", SELECT_3_PRIME="3' most";
        private String promoterSelectMode = SELECT_3_PRIME;
        public String getPromoterSelectMode()
        {
            return promoterSelectMode;
        }
        public void setPromoterSelectMode(String promoterSelectMode)
        {
            Object oldValue = this.promoterSelectMode;
            this.promoterSelectMode = promoterSelectMode;
            setModeSuffix( "" );
            firePropertyChange( "promoterSelectMode", oldValue, promoterSelectMode );
        }

        private DataElementPath outTrack;
        @PropertyName("Resulting promoters")
        @PropertyDescription("Track with resulting promoters")
        public DataElementPath getOutTrack()
        {
            return outTrack;
        }
        public void setOutTrack(DataElementPath outTrack)
        {
            Object oldValue = this.outTrack;
            this.outTrack = outTrack;
            firePropertyChange( "outTrack", oldValue, outTrack );
        }

        private String modeSuffix;
        @PropertyName ( "Mode suffix" )
        public String getModeSuffix()
        {
            return modeSuffix;
        }
        public void setModeSuffix(String val)
        {
            modeSuffix = promoterSelectMode.equals( SELECT_5_PRIME ) ? "_5prime"
                    : promoterSelectMode.equals( SELECT_3_PRIME ) ? "_3prime" : "_all";
        }

        private String cellLine;
        private String cellType;
        private static final String CELL_LINE_SELECTED = "Select cell line", CELL_TYPE_SELECTED = "Select tissue",
                ALL_TYPES_SELECTED = "All cell lines and types";
        private String cellSelection = ALL_TYPES_SELECTED;

        public boolean isCellLineHidden()
        {
            return !cellSelection.equals( CELL_LINE_SELECTED );
        }

        public boolean isCellTypeHidden()
        {
            return !cellSelection.equals( CELL_TYPE_SELECTED );
        }
        @PropertyName ( "Cell line" )
        public String getCellLine()
        {
            return cellLine;
        }
        public void setCellLine(String cellLine)
        {
            Object oldValue = this.cellLine;
            this.cellLine = cellLine;
            setCellSuffix( "" );
            firePropertyChange( "cellLine", oldValue, cellLine );
        }

        @PropertyName ( "Tissue" )
        public String getCellType()
        {
            return cellType;
        }
        public void setCellType(String cellType)
        {
            Object oldValue = this.cellType;
            this.cellType = cellType;
            setCellSuffix( "" );
            firePropertyChange( "cellType", oldValue, cellType );
        }

        @PropertyName ( "Selection mode" )
        @PropertyDescription ( "Specify selection mode" )
        public String getCellSelection()
        {
            return cellSelection;
        }
        public void setCellSelection(String cellSelection)
        {
            Object oldValue = this.cellSelection;
            this.cellSelection = cellSelection;
            setCellSuffix( "" );
            firePropertyChange( "cellSelection", oldValue, cellSelection );
        }

        private String cellSuffix;
        @PropertyName ( "Cell suffix" )
        public String getCellSuffix()
        {
            return cellSuffix;
        }
        public void setCellSuffix(String v)
        {
            if( cellSelection.equals( ALL_TYPES_SELECTED ) )
                cellSuffix = "";
            String val = cellSelection.equals( CELL_LINE_SELECTED ) ? getCellLine() : getCellType();
            cellSuffix = val != null ? "_" + val : "";
        }

        private boolean useEnsembl = false;

        @PropertyName ( "Add Ensembl promoters" )
        @PropertyDescription ( "Add promoter from Ensembl database if not found in MiRProm database " )
        public boolean isUseEnsembl()
        {
            return useEnsembl;
        }
        public void setUseEnsembl(boolean useEnsembl)
        {
            Object oldValue = this.useEnsembl;
            this.useEnsembl = useEnsembl;
            firePropertyChange( "this.useEnsembl", oldValue, useEnsembl );
        }

    }
}
