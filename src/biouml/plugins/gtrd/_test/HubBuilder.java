package biouml.plugins.gtrd._test;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;

import javax.swing.text.MutableAttributeSet;
import javax.swing.text.html.HTML.Attribute;
import javax.swing.text.html.HTML.Tag;
import javax.swing.text.html.HTMLEditorKit.ParserCallback;
import javax.swing.text.html.parser.ParserDelegator;

import com.developmentontheedge.application.ApplicationUtils;

import biouml.plugins.ensembl.tabletype.EnsemblGeneTableType;
import biouml.plugins.ensembl.tabletype.UniprotProteinTableType;
import junit.framework.Test;
import junit.framework.TestSuite;
import one.util.streamex.StreamEx;
import ru.biosoft.access._test.AbstractBioUMLTest;
import ru.biosoft.access.biohub.BioHubRegistry;
import ru.biosoft.access.biohub.BioHubSupport;
import ru.biosoft.access.biohub.ReferenceType;
import ru.biosoft.access.biohub.ReferenceTypeRegistry;
import ru.biosoft.access.core.CollectionFactory;
import ru.biosoft.access.sql.Connectors;
import ru.biosoft.access.sql.FastBulkInsert;
import ru.biosoft.access.sql.SqlUtil;
import ru.biosoft.exception.InternalException;
import ru.biosoft.util.HtmlUtil;
import ru.biosoft.util.TextUtil;

/**
 * @author lan
 *
 */
public class HubBuilder extends AbstractBioUMLTest
{
    private static final String NEW_DB_NAME = "gtrd_new";
    private static final String ORIGINAL_DB_NAME = "gtrd_current";

    /** Standard JUnit constructor */
    public HubBuilder(String name)
    {
        super(name);
    }

    /** Make suite if tests. */
    public static Test suite()
    {
        TestSuite suite = new TestSuite(HubBuilder.class.getName());
        suite.addTest(new HubBuilder("testBuilder"));
        return suite;
    }

    //////////////////////////////////////////////////////////////////
    // Test case
    //
    public static class GTRDParserCallback extends ParserCallback
    {
        private ClassInfo info = null;
        private String mode = "";
        private final Map<String, ClassInfo> result;
        public GTRDParserCallback(Map<String, ClassInfo> result)
        {
            this.result = result;
        }

        @Override
        public void handleText(char[] data, int pos)
        {
            if(info == null) return;
            if(mode.equals("ID"))
            {
                info.id = new String(data);
            } else if(mode.equals("title"))
            {
                info.title += new String(data);
            }
        }

        private String parseUniprot(MutableAttributeSet a)
        {
            String url = a.getAttribute( Attribute.HREF ).toString();
            String res = url.substring( url.lastIndexOf( '/' ) + 1 );
            if( res.startsWith( "?" ) )
                res = res.substring( res.indexOf( '=' ) + 1, res.indexOf( '[' ) );
            if( res.contains( "[" ) )
                res = res.substring( 0, res.indexOf( '[' ) );

            return res;
        }

        @Override
        public void handleStartTag(Tag t, MutableAttributeSet a, int pos)
        {
            if(t == Tag.TR)
            {
            }
            if(t == Tag.A && mode.equals("description") && info != null)
            {
                try
                {
                    info.description = HtmlUtil.stripHtml(a.getAttribute(Attribute.TITLE).toString());
                }
                catch( Exception e )
                {
                }
            }
            if(t == Tag.A && mode.equals("uniprot") && info != null)
            {
                info.uniprotID = parseUniprot( a );
            }
            if(t == Tag.A && mode.equals("uniprot_mu") && info != null)
            {
                info.uniprotIDMouse = parseUniprot( a );
            }
            if(t == Tag.A && mode.equals("uniprot_rat") && info != null)
            {
                info.uniprotIDRat = parseUniprot( a );
            }

            if(t == Tag.TD)
            {
                int colSpan = 1;
                try
                {
                    colSpan = Integer.parseInt(a.getAttribute(Attribute.COLSPAN).toString());
                }
                catch(Exception e)
                {
                }
                Object classNameObj = a.getAttribute(Attribute.CLASS);
                if(classNameObj != null)
                {
                    String className = classNameObj.toString();
                    if(className.endsWith("_no_td"))
                    {
                        info = new ClassInfo();
                        info.type = className.substring(0, className.indexOf("_no_td"));
                        mode = "ID";
                        return;
                    }
                    if(className.equals("uniprot"))
                    {
                        mode = "uniprot";
                    }else if(className.equals( "uniprot_mu" ))
                    {
                        mode = "uniprot_mu";
                    }else if(className.equals( "uniprot_rat" ))
                    {
                        mode = "uniprot_rat";
                    }
                }
            }
        }

        @Override
        public void handleEndTag(Tag t, int pos)
        {
            if(t == Tag.TD)
            {
                if(mode.equals("ID")) mode = "title";
                else if(mode.equals("title")) mode = "description";
                else mode = "";
            }
            if(t == Tag.TR && info != null)
            {
                info.normalize();
                if(result.containsKey(info.id)) throw new RuntimeException("Duplicate ID: "+info.id);
                result.put(info.id, info);
                info = null;
            }
        }
    }

    public static class ClassInfo
    {
        String id;
        String title="";
        String description="";
        String uniprotID;
        String uniprotIDMouse;
        String uniprotIDRat;
        String type;
        int level;

        @Override
        public String toString()
        {
            return id+"\t"+title+"\t"+description+"\t"+uniprotID + "\t" +uniprotIDMouse + "\t" + uniprotIDRat;
        }

        public void normalize()
        {
            id = id.replaceAll("[^\\d\\.]", "");
            int pos = title.indexOf(": ");
            if(pos > 0) title = title.substring(pos+2);
            level = TextUtil.split(id, '.').length;
        }

        public String getParentID(Map<String, ClassInfo> classes)
        {
            String parentID = id.contains(".")?id.substring(0, id.lastIndexOf(".")):"";
            while(parentID.endsWith(".0") && !classes.containsKey(parentID)) parentID = parentID.substring(0, parentID.length()-2);
            return parentID;
        }

        public String getPreviousSiblingID()
        {
            String[] elements = TextUtil.split(id, '.');
            int siblingNumber = Integer.parseInt(elements[elements.length-1])-1;
            if(siblingNumber <= 0) return "";
            elements[elements.length-1] = String.valueOf(siblingNumber);
            return String.join(".", elements);
        }
    }

    public void testBuilder() throws Exception
    {
        Map<String, ClassInfo> result = new LinkedHashMap<>();
        try (InputStream stream = HubBuilder.class.getResourceAsStream( "resources/huTF_classification.html" );
                Reader reader = new InputStreamReader( stream ))
        {
            ParserCallback callback = new GTRDParserCallback( result );
            new ParserDelegator().parse( reader, callback, false );
        }
        fixErrors( result );
        checkClassification(result);


        System.out.println("Create database...");
        Connection connection = getConnection();

        SqlUtil.execute( connection, "DROP TABLE IF EXISTS `hub`");
        SqlUtil.execute( connection,"CREATE TABLE `hub` LIKE " + ORIGINAL_DB_NAME + ".hub" );

        SqlUtil.execute( connection, "DROP TABLE IF EXISTS `classification`");
        SqlUtil.execute( connection, "CREATE TABLE `classification` LIKE " + ORIGINAL_DB_NAME + ".classification");

        SqlUtil.execute( connection, "DROP TABLE IF EXISTS `chip_experiments`");
        SqlUtil.execute( connection, "CREATE TABLE `chip_experiments` LIKE " + ORIGINAL_DB_NAME + ".chip_experiments" );

        System.out.println("Fill 'classification'...");
        FastBulkInsert insert = new FastBulkInsert(connection, "classification");
        for(ClassInfo classInfo: result.values())
        {
            if(classInfo.level < 6)
            {
                insert.insert(new Object[] {
                        classInfo.id,
                        classInfo.getParentID(result),
                        entitify(classInfo.title),
                        entitify(classInfo.description.isEmpty() ? classInfo.type.substring(0, 1).toUpperCase()
                                + classInfo.type.substring(1) : classInfo.description), classInfo.level});
            }
        }
        insert.flush();

        fillHub(result, connection);

        checkConstraints( connection );
    }

    protected String convertClass(String origClass, Map<String, Set<String>> old2new, Map<String, Set<String>> new2old)
    {
        if(origClass.split( "[.]" ).length != 5)
        {
            System.err.println( "Using orig name for " + origClass );
            return origClass;
        }
        if(!old2new.containsKey(origClass))
        {
            throw new InternalException( "Not linked" );
        }
        Set<String> newIds = old2new.get(origClass);
        if(newIds.contains(origClass))
        {
            return origClass;
        }
        if(newIds.size() > 1)
        {
            throw new InternalException("Ambiguous (One Old->Many New)");
        }
        origClass = newIds.iterator().next();
        Set<String> oldIds = new2old.get(origClass);
        if(oldIds.size() > 1 /*&& !origClass.equals("2.3.3.11.3") */)
        {
            throw new InternalException( "Ambiguous (One New->Many Old)" );
        }
        return origClass;
    }

    /**
     * @param result
     * @param connection
     * @throws SQLException
     * @throws Exception
     */
    protected void fillHub(Map<String, ClassInfo> result, Connection connection) throws SQLException, Exception
    {
        System.out.println("Fill 'hub'...");
        FastBulkInsert insert = new FastBulkInsert(connection, "hub");
        SqlUtil.execute(connection, "INSERT INTO hub SELECT * FROM "+ORIGINAL_DB_NAME+".hub WHERE input_type != 'ProteinGTRDType' AND input_type != 'IsoformGTRDType'");

        System.out.println("Fill isoforms matching...");
        for(ClassInfo classInfo: result.values())
        {
            if(classInfo.level == 6)
            {
                insert.insert(new Object[] {classInfo.getParentID(result),"ProteinGTRDType",classInfo.id,"IsoformGTRDType","Homo sapiens"});
                insert.insert(new Object[] {classInfo.id,"IsoformGTRDType",classInfo.uniprotID,"UniprotProteinTableType","Homo sapiens"});
                if(classInfo.uniprotIDMouse != null)
                    insert.insert(new Object[] {classInfo.id,"IsoformGTRDType",classInfo.uniprotIDMouse,"UniprotProteinTableType","Mus musculus"});
                if(classInfo.uniprotIDRat != null)
                    insert.insert(new Object[] {classInfo.id,"IsoformGTRDType",classInfo.uniprotIDRat,"UniprotProteinTableType","Rattus norvegicus"});
            }
        }

        System.out.println( "Fill TFClass level 5 to uniprot matching..." );
        for(ClassInfo classInfo: result.values())
        {
            if(classInfo.level == 5)
            {
                insert.insert( new Object[] {classInfo.id, "ProteinGTRDType", classInfo.uniprotID,"UniprotProteinTableType", "Homo sapiens"} );
                if(classInfo.uniprotIDMouse != null)
                    insert.insert( new Object[] {classInfo.id, "ProteinGTRDType", classInfo.uniprotIDMouse,"UniprotProteinTableType", "Mus musculus"} );
                if(classInfo.uniprotIDRat != null)
                    insert.insert( new Object[] {classInfo.id, "ProteinGTRDType", classInfo.uniprotIDRat,"UniprotProteinTableType", "Rattus norvegicus"} );
            }
        }

        initHubs();
        for(String species: new String[] {"Homo sapiens", "Mus musculus", "Rattus norvegicus"})
        {
            System.out.println("Matching uniprot to ensembl for " + species);
            ReferenceType sourceType = ReferenceTypeRegistry.getReferenceType(UniprotProteinTableType.class);
            Properties fromProperties = BioHubSupport.createProperties( species, sourceType );
            ReferenceType targetType = ReferenceTypeRegistry.getReferenceType(EnsemblGeneTableType.class);
            Properties toProperties = BioHubSupport.createProperties( species, targetType );

            Map<String, Set<String>> uniprotToClass = new HashMap<>();
            result.forEach( (id,ci)->{
                String uniprot = getUniprotForSpecies( ci, species );
                if(uniprot != null)
                    uniprotToClass.computeIfAbsent( uniprot, k->new HashSet<>() ).add( id );
            } );


            Map<String, String[]> references = BioHubRegistry.getReferences( uniprotToClass.keySet().toArray( new String[0] ),
                    fromProperties, toProperties, null );

            for( Entry<String, String[]> entry : references.entrySet() )
            {
                for( String className : uniprotToClass.get( entry.getKey() ) )
                {
                    for( String matchedId : entry.getValue() )
                    {
                        insert.insert( new Object[] {className, "ProteinGTRDType", matchedId, targetType.getStableName(), species} );
                    }
                }
            }
        }

        insert.flush();

        System.out.println("Map old IDs to new ones");
        Map<String, Set<String>> old2new = SqlUtil.queryMapSet(connection, "SELECT DISTINCT h1.input,h2.input FROM "+ORIGINAL_DB_NAME+".hub h1 " +
                "JOIN hub h2 ON(h1.output=h2.output) WHERE h1.input_type=\"ProteinGTRDType\" " +
                "AND h1.output_type=\"UniprotProteinTableType\" AND h2.input_type=\"ProteinGTRDType\" " +
                "AND h2.output_type=\"UniprotProteinTableType\" AND h1.specie=\"Homo sapiens\" " +
                "AND h2.specie=\"Homo sapiens\"");
        /*
        old2new.put("3.1.10.7", new HashSet<>(Arrays.asList("3.1.3.7")));
        old2new.put("2.1.2.1", new HashSet<>(Arrays.asList("2.1.2.1")));
        old2new.put("4.2.1", new HashSet<>(Arrays.asList("4.2.1")));
        old2new.put("6.1.1.1", new HashSet<>(Arrays.asList("6.1.1.1")));
        old2new.put("6.1.1", new HashSet<>(Arrays.asList("6.1.1")));
        old2new.put("6.1.5", new HashSet<>(Arrays.asList("6.1.5")));
        */
        Map<String, Set<String>> new2old = new HashMap<>();
        for(Entry<String, Set<String>> entry: old2new.entrySet())
        {
            for(String value: entry.getValue())
            {
                new2old.computeIfAbsent( value, k -> new HashSet<>() ).add( entry.getKey() );
            }
        }

        System.err.println( "Fill `hub` with SiteModelGTRDType" );
        int relinked = 0;
        int dead = 0;
        int copied = 0;
        int ambiguous = 0;
        for( Object[] row : SqlUtil.queryRows(connection, "select * from " + ORIGINAL_DB_NAME + ".hub where output_type ='SiteModelGTRDType'",
                String.class, String.class, String.class, String.class, String.class) )
        {
            try
            {
                String newId = convertClass((String)row[0], old2new, new2old);
                if(newId.equals(row[0])) copied++; else relinked++;
                row[0] = newId;
                insert.insert(row);
            }
            catch(Exception e)
            {
                System.out.println(e.getMessage()+": "+StreamEx.of( row ).joining("\t"));
                if(e.getMessage().equals("Not linked")) dead++; else ambiguous++;
            }
        }
        insert.flush();
        System.out.println("Relinked: "+relinked+"; copied: "+copied+"; dead: "+dead+"; ambiguous: "+ambiguous);
        assertTrue(dead == 0 && ambiguous == 0);

        System.out.println("Fill `chip_experiments`...");
        relinked = 0;
        dead = 0;
        copied = 0;
        ambiguous = 0;
        insert = new FastBulkInsert(connection, "chip_experiments");
        for( Object[] row : SqlUtil.queryRows(connection, "select id,antibody,tfClassId,specie,treatment,control_id,cell_id,experiment_type from " + ORIGINAL_DB_NAME + ".chip_experiments",
                String.class, String.class, String.class,
                String.class, String.class, String.class, Integer.class, String.class) )
        {
            try
            {
                String oldId = (String)row[2];
                if(oldId != null)
                {
                    String newId = convertClass(oldId, old2new, new2old);
                    if(newId.equals(oldId)) copied++; else relinked++;
                    row[2] = newId;
                }
                insert.insert(row);
            }
            catch(Exception e)
            {
                System.out.println(e.getMessage()+": "+StreamEx.of( row ).joining("\t"));
                if(e.getMessage().equals("Not linked")) dead++; else ambiguous++;
            }
        }
        insert.flush();
        System.out.println("Relinked: "+relinked+"; copied: "+copied+"; dead: "+dead+"; ambiguous: "+ambiguous);
        assertTrue(dead == 0 && ambiguous == 0);
    }

    private void checkConstraints(Connection connection)
    {
        //Find experiments that have no matching uniprot
        List<String> unmapped = SqlUtil.queryStrings( connection, "select distinct concat(tfClassId,'(',chip_experiments.specie,')') from chip_experiments left join "
        + "(select * from hub where input_type='ProteinGTRDType' and output_type='UniprotProteinTableType' ) h "
        + "on(h.input=tfClassId AND chip_experiments.specie=h.specie) where tfClassId like '%.%.%.%.%' and output is NULL");

        if(!unmapped.isEmpty())
        {
            for(String s : unmapped)
                System.err.println("No uniprot for " + s);
            fail("Some experiments has no matching uniprot");
        }

        List<String> multipleUniprotPerTFClass = SqlUtil.queryStrings( connection, "select concat(input,'(',specie,')'), count(*) c from hub where input_type='ProteinGTRDType' AND output_type='UniprotProteinTableType' group by input,specie having c > 1");
        if(!multipleUniprotPerTFClass.isEmpty())
        {
            for(String s : multipleUniprotPerTFClass)
                System.err.println( "Multiple uniprot per TFClass " + s );
            fail("Multiple uniprot per TFClass");
        }


        List<String> multipleTFClassesPerUniprot = SqlUtil.queryStrings( connection, "select output, count(*) c from hub where input_type='ProteinGTRDType' AND output_type='UniprotProteinTableType' group by output having c > 2 order by c");
        if(!multipleTFClassesPerUniprot.isEmpty())
        {
            for(String s : multipleTFClassesPerUniprot)
                System.err.println( "More then 2 TFClasses per uniprot " + s );
            fail("Multiple TFClasses per uniprot");
        }
    }

    private String getUniprotForSpecies(ClassInfo ci, String species)
    {
        if( species.equals( "Homo sapiens" ) )
            return ci.uniprotID;
        else if( species.equals( "Mus musculus" ) )
            return ci.uniprotIDMouse;
        else if( species.equals( "Rattus norvegicus" ) )
            return ci.uniprotIDRat;
        throw new AssertionError();
    }

    private void fixErrors(Map<String, ClassInfo> result)
    {
        result.get( "3.1.4.4.3.3" ).uniprotID = "P40426-3";//was P40424-3
        result.get( "3.1.4.4.3.4" ).uniprotID = "P40426-4";//was P40424-4
        result.get( "3.1.4.4.3.5" ).uniprotID = "P40426-5";//was P40424-5

        result.get( "2.3.3.37.2" ).uniprotIDMouse = "E9Q1A5";//was missing
        result.get( "3.1.4.6.2" ).uniprotIDMouse = "Q8C0Y1";//was missing (xxxxxx)

        result.get( "3.1.3.7.5" ).uniprotIDMouse = "A1JVI8";//Dux mouse was missing

        for(ClassInfo ci : result.values())
        {
            ci.uniprotID = fixUniprotTypo( ci.uniprotID );
            ci.uniprotIDMouse = fixUniprotTypo( ci.uniprotIDMouse );
            ci.uniprotIDRat = fixUniprotTypo( ci.uniprotIDRat );
        }
    }

    private String fixUniprotTypo(String id)
    {
        if(id == null)
            return null;

        if( id.equals( "xxxxxx" ) )
            return null;

        //Fix errors (typos) in html
        if( id.equals( "Q64289x" ) )
            return "Q64289";
        if( id.equals( "Q9Z248 target=" ) )
            return "Q9Z248";
        if( id.equals( "D4ADE6x" ) )
            return "D4ADE6";
        if( id.equals( "(F1LWY9" ) )
            return "F1LWY9";
        if( id.equals( "Q4KLH4x" ) )
            return "Q4KLH4";

        return id;

    }

    protected void checkClassification(Map<String, ClassInfo> result)
    {
        List<String> noUniprotLevel6 = Arrays.asList("1.2.1.0.1.3", "1.2.6.2.2.4", "2.3.3.0.21.8", "2.3.3.0.21.9",
                "3.5.3.0.3.3", "3.5.3.0.3.4", "3.5.3.0.3.5", "6.2.1.0.2.1", "6.2.1.0.2.2",
                "6.2.1.0.3.3",
                "4.1.3.0.5.13", "5.1.1.1.3.7");

        //Uniprots that maps to several TFclasses
        List<String> checkedDuplicates = Arrays.asList("Q9UHF7", "Q8IX07", "Q8WW38", "Q13330", "O94776", "Q9BTC8", "Q9P2R6", "Q96PN7",
                "Q9H0D2", "Q8IWI9", "Q9H2P0", "Q6IQ32");

        //PrintWriter pw = new PrintWriter(new File("C:\\test.txt"), "UTF-8");
        Map<String, String> title2id = new HashMap<>();
        Map<String, String> uniprot2id = new HashMap<>();
        for(ClassInfo classInfo: result.values())
        {
            String infoString = classInfo.toString();
            assertTrue(infoString, classInfo.level >= 1 && classInfo.level <=6);

            assertTrue(infoString, classInfo.level == 1 || result.containsKey(classInfo.getParentID(result)));

            //Some level6 classes has no uniprot
            assertTrue(infoString, classInfo.uniprotID != null || classInfo.level != 6 || noUniprotLevel6.contains(classInfo.id));

            if(classInfo.level == 5)
            {
                assertNotNull( classInfo.uniprotID );
                assertTrue(infoString, checkUniprotProtein(classInfo.uniprotID));
                if(classInfo.uniprotIDMouse != null)
                    assertTrue(infoString, checkUniprotProtein(classInfo.uniprotIDMouse));
                if(classInfo.uniprotIDRat != null)
                    assertTrue(infoString, checkUniprotProtein(classInfo.uniprotIDRat));
            }
            else if(classInfo.level == 6)
            {
                if(classInfo.uniprotID != null)
                    assertTrue(infoString, checkUniprotIsoform(classInfo.uniprotID));
                if(classInfo.uniprotIDMouse != null)
                    assertTrue(infoString, checkUniprotIsoform(classInfo.uniprotIDMouse));
                if(classInfo.uniprotIDRat != null)
                    assertTrue(infoString, checkUniprotIsoform(classInfo.uniprotIDRat));
            }
            else if(classInfo.uniprotID != null || classInfo.uniprotIDMouse != null || classInfo.uniprotIDRat != null)
                fail( "Has uniprot for level < 5: " + infoString );

            assertTrue(infoString, classInfo.title.length() >= 2 || classInfo.id.equals("6.1.4.1"));
            if(title2id.containsKey(classInfo.title) && !title2id.get(classInfo.title).equals(classInfo.getParentID(result)))
            {
                String uni1 = classInfo.uniprotID;
                String uni2 = result.get(title2id.get(classInfo.title)).uniprotID;
                if(uni1 == null || uni2 == null || !uni1.equals(uni2))
                    assertTrue("Dublicate title: "+classInfo.title+" ("+classInfo.id+", "+title2id.get(classInfo.title)+")", false);
            }
            if(!classInfo.title.equals("unclassified"))
                title2id.put(classInfo.title, classInfo.id);

            //TODO: check duplicates for rat and mouse
            if(classInfo.uniprotID != null)
            {
                if( uniprot2id.containsKey(classInfo.uniprotID)
                        && ( classInfo.level < 6 || ( !uniprot2id.get(classInfo.uniprotID).equals(classInfo.getParentID(result)) && !classInfo
                                .getParentID(result).equals(result.get(uniprot2id.get(classInfo.uniprotID)).getParentID(result)) ) ) )
                {
                    assertTrue("Dublicate uniprot: "+classInfo.uniprotID+" ("+classInfo.id+", "+uniprot2id.get(classInfo.uniprotID)+")", false);
                    //System.out.println("Dublicate uniprot: "+classInfo.uniprotID+" ("+classInfo.id+", "+uniprot2id.get(classInfo.uniprotID)+")");
                }
                if(!checkedDuplicates.contains(classInfo.uniprotID.substring(0, 6)))
                    uniprot2id.put(classInfo.uniprotID, classInfo.id);
            }
            //pw.println(infoString);
        }
        //pw.close();
    }

    private boolean checkUniprotIsoform(String uniprotID)
    {
        return uniprotID.matches("([A-NR-Z][0-9][A-Z][A-Z0-9][A-Z0-9][0-9]|[OPQ][0-9][A-Z0-9][A-Z0-9][A-Z0-9][0-9])(|\\-\\d{1,2})");
    }

    private boolean checkUniprotProtein(String uniprotID)
    {
        return uniprotID.matches("([A-NR-Z][0-9][A-Z][A-Z0-9][A-Z0-9][0-9]|[OPQ][0-9][A-Z0-9][A-Z0-9][A-Z0-9][0-9])");
    }

    /**
     * Replaces non-ascii symbols with HTML entities
     * @return
     */
    private String entitify(String string)
    {
        StringBuilder result = new StringBuilder();
        for(int i=0; i<string.length(); i++)
        {
            int codePoint = string.codePointAt(i);
            if(codePoint > 127) result.append("&#"+codePoint+";");
            else result.append(string.charAt(i));
        }
        return result.toString();
    }

    private void executeSQL(Connection conn, String sql) throws Exception
    {
        for( String sqlStr : StreamEx.split( sql, ';' ).map( String::trim ).remove( String::isEmpty ) )
        {
            try(Statement st = conn.createStatement())
            {
                st.execute(sqlStr);
            }
        }
    }

    private String getSql(String fileName) throws IOException
    {
        InputStream inputStream = HubBuilder.class.getResourceAsStream(fileName);
        return ApplicationUtils.readAsString(inputStream);
    }

    private Connection getConnection()
    {
        return Connectors.getConnection( NEW_DB_NAME );
    }

    private void initHubs() throws Exception
    {
        CollectionFactory.createRepository("../data");
    }
}
