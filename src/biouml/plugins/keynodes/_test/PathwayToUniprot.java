package biouml.plugins.keynodes._test;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.developmentontheedge.application.ApplicationUtils;

import biouml.model.Diagram;
import biouml.model.Module;
import biouml.model.Node;
import biouml.standard.type.Base;
import biouml.standard.type.DatabaseReference;
import biouml.standard.type.Reaction;
import biouml.standard.type.Referrer;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import one.util.streamex.StreamEx;
import ru.biosoft.access.biohub.BioHub;
import ru.biosoft.access.biohub.BioHubRegistry;
import ru.biosoft.access.biohub.Element;
import ru.biosoft.access.biohub.TargetOptions;
import ru.biosoft.access.biohub.TargetOptions.CollectionRecord;
import ru.biosoft.access.core.CollectionFactory;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.sql.Connectors;
import ru.biosoft.util.Maps;
import ru.biosoft.util.TextUtil2;


public class PathwayToUniprot extends TestCase
{
    private DataCollection<?> repository = null;
    private List<String> table = null;
    private static final String serverPath = "http://46.51.191.19/bioumlweb/#de=";
    private static final String repositoryPath = "../data/test/PathwayToUniprot";
    private static final String repositoryResultPath = "../data/test/PathwayToUniprot/Result";
    private TargetOptions dbOptions;
    private BioHub bioHub;
    private Map<String, String> dbid2title;
    private Map<String, Set<String>> name2uniprot;
    private Connection connection;
    private PreparedStatement preparedStatement;
    private Map<String, Set<String>> keggpw;
    private final List<String> moduleNames = new ArrayList<>( Arrays.asList( new String[] {"Biopath", "KEGG", "Reactome", "Transpath"} ) );


    private static Set<String> amazonNameList = new HashSet<>( Arrays.asList( "DGR0004", "DGR0005", "DGR0006", "DGR0007", "DGR0008",
            "DGR0010a", "DGR0011", "DGR0012", "DGR0013", "DGR0014", "DGR0015", "DGR0017", "DGR0018", "DGR0019", "DGR0020", "DGR0021",
            "DGR0022", "DGR0023", "DGR0025", "DGR0026", "DGR0027", "DGR0028", "DGR0029", "DGR0030", "DGR0031", "DGR0032", "DGR0033",
            "DGR0034", "DGR0035", "DGR0037", "DGR0038", "DGR0039", "DGR0040", "DGR0041", "DGR0042", "DGR0044", "DGR0044a", "DGR0045",
            "DGR0046_pending", "DGR0047", "DGR0048", "DGR0049", "DGR0050", "DGR0051", "DGR0052", "DGR0053", "DGR0054", "DGR0055",
            "DGR0056", "DGR0057", "DGR0058", "DGR0059", "DGR0060", "DGR0061", "DGR0062", "DGR0063", "DGR0064", "DGR0065", "DGR0066",
            "DGR0068", "DGR0068b", "DGR0068_1", "DGR0068_a", "DGR0069", "DGR0070", "DGR0070_1", "DGR0072", "DGR0073", "DGR0074", "DGR0075",
            "DGR0076", "DGR0077", "DGR0078", "DGR0079", "DGR0080", "DGR0081", "DGR0082", "DGR0083", "DGR0084", "DGR0085", "DGR0086",
            "DGR0087", "DGR0088", "DGR0089", "DGR0090", "DGR0091", "DGR0092", "DGR0094", "DGR0095", "DGR0096", "DGR0097", "DGR0098",
            "DGR0100", "DGR0101", "DGR0102", "DGR0103", "DGR0104", "DGR0105", "DGR0106", "DGR0107", "DGR0108", "DGR0109", "DGR0110",
            "DGR0111", "DGR0112", "DGR0113", "DGR0114", "DGR0115", "DGR0116", "DGR0117", "DGR0118", "DGR0119", "DGR0119_1", "DGR0120",
            "DGR0121", "DGR0122", "DGR0123", "DGR0124", "DGR0124_1", "DGR0125", "DGR0126", "DGR0126_1", "DGR0127", "DGR0128", "DGR0128_1",
            "DGR0129", "DGR0130", "DGR0131", "DGR0131a", "DGR0132", "DGR0133", "DGR0134", "DGR0135", "DGR0136", "DGR0136a", "DGR0136b",
            "DGR0136c", "DGR0137", "DGR0139", "DGR0140", "DGR0141", "DGR0142", "DGR0143", "DGR0143_1", "DGR0144", "DGR0144_pending",
            "DGR0145", "DGR0145_1", "DGR0146", "DGR0147", "DGR0148", "DGR0148_pending", "DGR0149", "DGR0150", "DGR0151", "DGR0151_1",
            "DGR0152", "DGR0152_pending", "DGR0153", "DGR0154", "DGR0154_pending", "DGR0155", "DGR0155_pending", "DGR0156",
            "DGR0156_pending", "DGR0157", "DGR0158", "DGR0159", "DGR0160", "DGR0161", "DGR0162", "DGR0163", "DGR0164", "DGR0165",
            "DGR0172", "DGR0175", "DGR0181", "DGR0183", "DGR0183_1", "DGR0184", "DGR0184_1", "DGR0185", "DGR0185_1", "DGR0186", "DGR0187",
            "DGR0187_1", "DGR0188", "DGR0188_1", "DGR0189", "DGR0189_1", "DGR0190", "DGR0190_1", "DGR0191", "DGR0191_1", "DGR0192",
            "DGR0192_1", "DGR0193", "DGR0193_1", "DGR0194", "DGR0194_1", "DGR0195", "DGR0195_1", "DGR0196", "DGR0196_1", "DGR0197",
            "DGR0197_1", "DGR0198", "DGR0198_1", "DGR0199", "DGR0199_1", "DGR0200", "DGR0200_1", "DGR0201", "DGR0201_1", "DGR0202",
            "DGR0203", "DGR0204", "DGR0205", "DGR0206", "DGR0207", "DGR0207_1", "DGR0208", "DGR0208_1", "DGR0209", "DGR0209_1", "DGR0210",
            "DGR0210_1", "DGR0211", "DGR0212", "DGR0213", "DGR0214", "DGR0215", "DGR0216", "DGR0217", "DGR0218", "DGR0219", "DGR0220",
            "DGR0221", "DGR0222", "DGR0223", "DGR0224", "DGR0225", "DGR0226", "DGR0227", "DGR0228", "DGR0229", "DGR0230", "DGR0231",
            "DGR0232", "DGR0233", "DGR0234", "DGR0235", "DGR0236", "DGR0237", "DGR0238", "DGR0239", "DGR0240r", "DGR0240_2", "DGR0241",
            "DGR0242", "DGR0243", "DGR0244", "DGR0245", "DGR0246", "DGR0246a", "DGR0247", "DGR0248", "DGR0249", "DGR0250", "DGR0251",
            "DGR0252", "DGR0253", "DGR0254", "DGR0255", "DGR0256", "DGR0257", "DGR0258", "DGR0259", "DGR0260", "DGR0261", "DGR0262",
            "DGR0263", "DGR0264", "DGR0265", "DGR0266", "DGR0267", "DGR0267_1", "DGR0267_2", "DGR0267_3", "DGR0268", "DGR0269", "DGR0270",
            "DGR0271", "DGR0272", "DGR0273", "DGR0274", "DGR0275", "DGR0276", "DGR0277", "DGR0277a", "DGR0278", "DGR0281", "DGR0282",
            "DGR0283", "DGR0284", "DGR0285", "DGR0286", "DGR0287", "DGR0288", "DGR0289", "DGR0290", "DGR0291", "DGR0292", "DGR0293",
            "DGR0294", "DGR0295", "DGR0296", "DGR0297", "DGR0298", "DGR0300", "DGR0301", "DGR0302", "DGR0303", "DGR0304", "DGR0305",
            "DGR0306", "DGR0307", "DGR0308", "DGR0309", "DGR0310", "DGR0311", "DGR0312", "DGR0313", "DGR0314", "DGR0315", "DGR0316_1",
            "DGR0316_2", "DGR0316_3", "DGR0316_4", "DGR0316_4a", "DGR0317", "DGR0318", "DGR0318_1", "DGR0319", "DGR0320", "DGR0321",
            "DGR0322", "DGR0323", "DGR0324", "DGR0325", "DGR0326", "DGR0327", "DGR0329", "DGR0330", "DGR0331", "DGR0332", "DGR0332_1",
            "DGR0333", "DGR0338", "DGR0338_1", "DGR0339", "DGR0339_1", "DGR0340", "DGR0340_1", "DGR0341", "DGR0341_1", "DGR0342",
            "DGR0342_1", "DGR0342_2", "DGR0342_3", "DGR0343", "DGR0344_1", "DGR0345", "DGR0345_1", "DGR0346", "DGR0346_1", "DGR0347",
            "DGR0347_1", "DGR0348", "DGR0349", "DGR0350", "DGR0351", "DGR0352", "DGR0352_1", "DGR0352_1*", "DGR0352_1_1", "DGR0352_2",
            "DGR0352_3", "DGR0352_4export", "DGR0352_4simple", "DGR0352_5", "DGR0352_bag", "DGR0352_saveas", "DGR0352_test", "DGR0353",
            "DGR0353_1", "DGR0353_2", "DGR0354", "DGR0355", "DGR0355_1", "DGR0355_10", "DGR0355_11", "DGR0355_12", "DGR0355_13",
            "DGR0355_2", "DGR0355_3", "DGR0355_4", "DGR0355_5", "DGR0355_6", "DGR0355_7", "DGR0355_8", "DGR0355_9", "DGR0356", "DGR0356_1",
            "DGR0356_2", "DGR0356_3", "DGR0356_4", "DGR0356_5", "DGR0357", "DGR0357_1", "DGR0358", "DGR0359", "DGR0360", "DGR0361",
            "DGR0362", "DGR0363", "DGR0400L", "DGR0401L", "DGR0402L", "DGR0403L", "DGR0404L", "DGR0405L", "DGR0406L", "DGR0407L",
            "DGR0408L", "DGR0409L", "DGR_AO0001", "DGR_AO0002", "DGR_M0001", "DGR_M0002", "DGR_M0003", "DGR_M0004", "DGR_NF-kB_inhibs",
            "DGR_NFkappaB", "DGR_OX0001", "DGR_OX0002", "DGR_PP0001", "DGR_PP0002", "DGR_PP0003", "DGR_PP0004", "DGR_TH1", "DGR_TH10",
            "DGR_TH11", "DGR_TH12", "DGR_TH13", "DGR_TH14", "DGR_TH2", "DGR_TH3", "DGR_TH4", "DGR_TH5", "DGR_TH6", "DGR_TH7", "DGR_TH8",
            "DGR_TH9", "Erithroid", "Erithroiddifferentia", "fhg", "glycolysis1.xml", "Integrated Model", "Int_casp12_module",
            "Int_casp8_module", "Int_CD95L_module", "Int_CytC_module", "Int_EGF_module", "Int_exe_phase_module", "Int_Mitoch_module",
            "Int_NF-kB_module", "Int_p53_module", "Int_PARP_module", "Int_Smac_module", "Int_TNF_module", "Int_TRAIL_module", "ip3.xml",
            "jj", "k", "Karaaslan4", "Karaaslan_Ivan", "Karaaslan_Ivan1", "Karaaslan_pust", "Karaaslan_stab", "Karaslaan",
            "Karaslaan sod intake", "Karaslaan2", "Karaslaan3", "kitano1", "Kitano_1", "model55_", "New Diagram", "New Diagram 7",
            "New Diagram1", "New Diagram2", "newtestsbmlimport", "NF-kB inhibs_pending", "Nuc001", "Nuc002", "Nuc003", "Nuc003_1",
            "Nuc004", "Nuc005", "Nuc005_1", "Nuc006", "Nuc007", "Nuc008", "Nuc008_1", "Nuc008_2", "Nuc008_3", "Nuc008_4", "Nuc009",
            "Nuc010", "Nuc011", "Nuc012", "Nuc013", "Nuc014", "Nuc015", "Nuc015_1", "Nuc016", "Nuc017", "Nuc018", "Nuc019", "Nuc020",
            "Nuc021", "Nuc022", "Nuc023", "Nuc024", "Nuc025", "Nuc026", "Nuc027", "Nuc028", "Nuc029", "Nuc030", "Nuc031", "Nuc031_1",
            "Nuc031_2", "Nuc031_3", "Nuc032", "Nuc033", "Nuc034", "Nuc035", "Nuc036", "Nuc037", "Nuc037_1", "Nuc038", "Nuc039", "Nuc040",
            "Nuc041", "Nuc042", "Nuc043", "Nuc044", "Nuc045"));

    public PathwayToUniprot(String name)
    {
        super(name);
    }

    public static void main(String[] args)
    {
        junit.textui.TestRunner.run(PathwayToUniprot.class);
    }

    public static junit.framework.Test suite()
    {
        TestSuite suite = new TestSuite(PathwayToUniprot.class.getName());
        suite.addTest(new PathwayToUniprot("buildTables"));
        return suite;
    }

    public void buildTables()
    {
        for( String moduleName : moduleNames )
        {
            try
            {
                Module module = (Module)repository.get(moduleName);
                table = new ArrayList<>();
                dbid2title = new HashMap<>();
                name2uniprot = new HashMap<>();
                if( connection != null )
                    try
                    {
                        connection.close();
                    }
                    catch( SQLException e )
                    {
                    }
                connection = null;
                if( moduleName.equals("KEGG") )
                {
                    name2uniprot = readKEGG2Uniprot("kegg2uniprot.txt");
                    keggpw = readKEGG("hsa_pathway.list");
                    dbid2title = readKEGGTitles("map_title.tab");
                }
                buildTable(module);
            }
            catch( Exception e )
            {
                System.err.println("Can not create mapping for module " + moduleName);
            }
        }
    }

    private void buildTable(Module module)
    {
        try
        {
            DataCollection<Diagram> pathwayDC = module.getDiagrams();
            DataElementPath pathwaysPath = pathwayDC.getCompletePath();
            List<String> pathways = pathwayDC.getNameList();
            for(String name: pathways)
            {
                if( module.getName().equals("KEGG") )
                {
                    String pathwayNum = "path:hsa" + name.substring(3, 8);
                    Set<String> accs = keggpw.get(pathwayNum);
                    if( accs == null )
                        continue;
                    for( String acc : accs )
                    {
                        Set<String> accsens = name2uniprot.get(acc);
                        if( accsens == null )
                            continue;
                        for( String acce : accsens )
                        {
                            table.add(acce + "\t" + dbid2title.get(pathwayNum) + "\t"
                                    + getServerLink(pathwaysPath.getChildPath(name)));
                        }
                    }
                }
                else
                {
                    if( module.getName().equals("Biopath") && !amazonNameList.contains(name) )
                    {
                        continue;
                    }
                    Diagram diagram = pathwaysPath.getChildPath(name).optDataElement(Diagram.class);
                    if( diagram == null )
                        continue;
                    diagram.recursiveStream().select( Node.class ).map( Node::getKernel ).select( Referrer.class )
                        .flatCollection( kernel -> getUniprotIds( kernel, module ) )
                        .distinct().map( acc -> acc + "\t" + diagram.getTitle() + "\t" + getServerLink(diagram.getCompletePath()) )
                        .forEach( table::add );
                }

                if( table.size() >= 1000 )
                {
                    printTable(module);
                    table.clear();
                }
            }
            printTable(module);
        }
        catch( Exception e )
        {
            System.out.println(e.getMessage());
        }
    }

    private Set<String> getUniprotIds(Referrer kernel, Module module)
    {
        String completeName = kernel.getName();
        if( !name2uniprot.containsKey(completeName) )
        {
            Set<String> accs = new HashSet<>();
            if( module.getName().equals("Reactome") && ! ( kernel instanceof Reaction ) )
            {
                String entClass = (String)kernel.getAttributes().getValue("Class");
                if( entClass != null && entClass.equals("EntityWithAccessionedSequence") )
                {
                    initConnection(module);
                    try
                    {
                        preparedStatement.setString(1, kernel.getName());
                        ResultSet rs = preparedStatement.executeQuery();
                        while( rs.next() )
                        {
                            accs.add(rs.getString(1));
                        }
                    }
                    catch( SQLException e2 )
                    {

                    }
                }


            }
            else if( module.getName().equals("Transpath") && ! ( kernel instanceof Reaction ) )
            {
                initConnection(module);
                try
                {
                    preparedStatement.setString(1, kernel.getName());
                    ResultSet rs = preparedStatement.executeQuery();
                    while( rs.next() )
                    {
                        accs.add(rs.getString(1));
                    }
                }
                catch( SQLException e2 )
                {

                }
            }
            else if( module.getName().equals("Biopath") && ! ( kernel instanceof Reaction ) )
            {
                DatabaseReference[] refs = kernel.getDatabaseReferences();
                if( refs != null )
                    for( DatabaseReference ref : refs )
                    {
                        String id = ref.getId();
                        if( id == null || id.equals("null") || id.equals("CHEBI") )
                            continue;
                        String dbId = ref.getDatabaseName();

                        String dbName = null;
                        if( dbid2title.containsKey(dbId) )
                        {
                            dbName = dbid2title.get(dbId);
                        }
                        else
                        {
                            try
                            {
                                Base de = module.getCompletePath().getChildPath("Dictionaries", "database info", dbId).getDataElement(Base.class);
                                dbName = de.getTitle();
                                int ind = dbName.indexOf('/');
                                if( ind != -1 )
                                    dbName = dbName.substring(0, ind);
                            }
                            catch( Exception e1 )
                            {
                                dbName = dbId;
                            }
                            if( dbName.equals("Entrez Gene") )
                                dbName = "EntrezGene";

                            dbid2title.put(dbId, dbName);
                        }
                        if( dbName.equalsIgnoreCase("transpath") )
                        {
                            initConnection(module);
                            try
                            {
                                preparedStatement.setString(1, id);
                                ResultSet rs = preparedStatement.executeQuery();
                                while( rs.next() )
                                {
                                    accs.add(rs.getString(1));
                                }
                            }
                            catch( SQLException e2 )
                            {

                            }
                        }
                        else if( dbName.equalsIgnoreCase("uniprot") )
                        {
                            accs.add(dbId);
                        }
                        else if( !dbName.equals("pubmed") && !dbName.equals("GO") )
                        {
                            Element elem = new Element("stub/%" + dbName + "%//" + id);
                            Element[] refElems = bioHub.getReference(elem, dbOptions, null, 1, BioHub.DIRECTION_DOWN);
                            if( refElems != null && refElems.length > 0 && refElems.length < 10 )
                            {
                                for( Element el : refElems )
                                {
                                    accs.add(el.getAccession());
                                }
                            }
                            else if( refElems != null && refElems.length >= 10 )
                            {
                                System.out.println(id + "\t" + dbName + "too many");
                            }
                            else
                            {
                                Set<String> tpaccs = name2uniprot.get(id);
                                if( tpaccs != null && ( dbName.equals("Entrez") || dbName.equals("Affymetrix") ) )
                                    accs.addAll(tpaccs);
                                else
                                    System.out.println(id + "\t" + dbName);
                            }
                        }
                    }
            }
            Set<String> accslist = new HashSet<>();
            accslist.addAll(accs);
            name2uniprot.put(completeName, accslist);
        }
        return name2uniprot.get(completeName);
    }


    private void printTable(Module module) throws IOException
    {
        try (BufferedWriter out = new BufferedWriter( new OutputStreamWriter( new FileOutputStream( repositoryResultPath + "/UniprotId_to_"
                + module.getName() + "Pathway.txt", true ), StandardCharsets.UTF_8 ) ))
        {
            for( String str : table )
            {
                out.write(str + "\n");
            }
        }
    }

    private String getServerLink(DataElementPath dePath)
    {
        return serverPath + dePath;
    }

    private static Map<String, Set<String>> readKEGG2Uniprot(String name) throws IOException
    {
        try(BufferedReader br = ApplicationUtils.utfReader( repositoryPath + "/" + name) )
        {
            Map<String, Set<String>> name2uniprot = StreamEx.ofLines( br ).map( s -> TextUtil2.split( s, '\t' ) )
                    .groupingBy( v -> v[0], Collectors.mapping( v -> v[1], Collectors.toSet() ) );
            return Maps.filterValues( name2uniprot, v -> v.size() > 20 ); // remove too big groups
        }
    }

    private static Map<String, Set<String>> readKEGG(String name) throws IOException
    {
        try(BufferedReader br = ApplicationUtils.utfReader( repositoryPath + "/" + name) )
        {
            return StreamEx.ofLines( br ).map( s -> TextUtil2.split( s.trim(), '\t' ) )
                    .groupingBy( v -> v[1], Collectors.mapping( v -> v[0], Collectors.toSet() ) );
        }
    }

    private static Map<String, String> readKEGGTitles(String name) throws IOException
    {
        try(BufferedReader br = ApplicationUtils.utfReader( repositoryPath + "/" + name) )
        {
            return StreamEx.ofLines(br).map( s -> TextUtil2.split( s.trim(), '\t' ) )
                    .toMap( v -> "path:hsa" + v[0], v -> v[1] );
        }
    }


    @Override
    public void setUp() throws Exception
    {
        repository = CollectionFactory.createRepository("../data");
        String ensName = "databases/Ensembl";
        repository.get(ensName); //init ensembl module
        table = new ArrayList<>();
        CollectionRecord collection = new CollectionRecord("databases/Ensembl", true);
        dbOptions = new TargetOptions(collection);
        bioHub = BioHubRegistry.getBioHub(dbOptions);
        dbid2title = new HashMap<>();
        name2uniprot = new HashMap<>();
        
        File repositoryPathDir = new File(repositoryResultPath);
        if( !repositoryPathDir.exists() )
        {
            assertTrue("Can not create repository folder: " + repositoryPathDir.getParent(), repositoryPathDir.getParentFile().mkdir());
            assertTrue("Can not create result repository folder: " + repositoryResultPath, repositoryPathDir.mkdir());
        }
    }

    private void initConnection(Module module)
    {
        try
        {
            if( connection == null )
            {
                if( module.getName().equals("Reactome") )
                {
                    connection = Connectors.getConnection( "reactome" );
                    preparedStatement = connection
                            .prepareStatement("SELECT DISTINCT re.identifier FROM ReferenceEntity re JOIN EntityWithAccessionedSequence eas ON re.DB_ID=eas.referenceEntity "
                                    + "JOIN DatabaseObject dbo ON (eas.DB_ID = dbo.DB_ID) JOIN StableIdentifier si ON(dbo.stableIdentifier=si.DB_ID) "
                                    + "WHERE re.referenceDatabase=2 AND si.identifier=?");
                }
                else if( module.getName().equals("Transpath") || module.getName().equals("Biopath") )
                {
                    connection = Connectors.getConnection( "transpath" );
                    preparedStatement = connection
                            .prepareStatement("SELECT DISTINCT uniprot_id FROM transpath2uniprot WHERE transpath_id=?");
                }

            }
        }
        catch( Exception e )
        {
        }
    }

    @Override
    public void tearDown()
    {
        if( connection != null )
            try
            {
                connection.close();
            }
            catch( SQLException e )
            {
            }
    }
}
