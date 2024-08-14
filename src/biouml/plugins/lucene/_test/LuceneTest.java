package biouml.plugins.lucene._test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.Collection;

import com.developmentontheedge.application.ApplicationUtils;
import com.developmentontheedge.beans.DynamicPropertySet;

import biouml.model.Module;
import biouml.plugins.lucene.LuceneQuerySystem;
import biouml.plugins.lucene.LuceneSearchView;
import biouml.standard.type.Gene;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import ru.biosoft.access._test.AbstractBioUMLTest;
import ru.biosoft.access.core.CollectionFactory;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.Environment;
import ru.biosoft.access.security.BiosoftClassLoading;
import ru.biosoft.util.DPSUtils;

public class LuceneTest extends AbstractBioUMLTest
{
    private static final String repositoryPath = "../data/test/biouml/plugins/lucene/data";

    private DataCollection root;
    private DataCollection module;

    private LuceneQuerySystem luceneFacade;

    private DynamicPropertySet[] dps;
    private DataCollection geneDC;

    public LuceneTest(String name)
    {
        super ( name );
    }

    public static Test suite ( )
    {
        TestSuite suite = new TestSuite ( LuceneTest.class.getName ( ) );

        suite.addTest ( new LuceneTest ( "testLucene" ) );

        //suite.addTest(new LuceneTest("testSearchView"));
        //suite.addTest(new LuceneTest("testIndexEditorDialog"));

        return suite;
    }
    
    public void testLucene() throws Exception
    {
        testRepository();
        testLuceneProperties();
        testGetLuceneFacade();
        testCreateIndex();
        testModifyIndex();
        testSearch();
        testGetSuggestions();
        testSaveLoad();
        testListener();
        testDeleteIndex();
    }

    protected void testRepository ( ) throws Exception
    {
        File file1 = new File ( repositoryPath + "/module/Data/gene.dat.orig" );
        File file2 = new File ( repositoryPath + "/module/Data/gene.dat" );
        file2.createNewFile ( );
        file2.deleteOnExit ( );
        ApplicationUtils.copyFile ( file2, file1 );

        CollectionFactory.unregisterAllRoot();
        root = CollectionFactory.createRepository( repositoryPath );
        assertNotNull ( "Can not load repository", root );
    }

    protected void testLuceneProperties ( ) throws Exception
    {
        module = ( DataCollection ) root.get ( "module" );
        assertNotNull ( "Can not find module", module );

        String luceneDir = module.getInfo ( ).getProperty (
                LuceneQuerySystem.LUCENE_INDEX_DIRECTORY );
        assertNotNull ( "Lucene directory name is missing", luceneDir );

        geneDC = CollectionFactory.getDataCollection ( "test/module/Data/gene" );
        assertNotNull ( "Can not find gene data collection", geneDC );

        String luceneIndexes = geneDC.getInfo ( ).getProperty (
                LuceneQuerySystem.LUCENE_INDEXES );
        assertNotNull ( "Lucene indexes is missing", luceneIndexes );
    }

    protected void testGetLuceneFacade ( ) throws Exception
    {
        luceneFacade = ( LuceneQuerySystem ) module.getInfo ( )
                .getQuerySystem ( );
        assertNotNull ( "Cannot create lucene facade", luceneFacade );
    }

    protected void testCreateIndex ( ) throws Exception
    {
        luceneFacade.createIndex ( null, null );
    }

    protected void testModifyIndex ( ) throws Exception
    {
        luceneFacade.deleteFromIndex ( CollectionFactory.getRelativeName (
                geneDC, module ), null );

        dps = luceneFacade.search ( CollectionFactory.getRelativeName ( geneDC,
                module ), "meristem", new String[] { "name", "title",
                "comment", "description", "completeName", "species", "source",
                "regulation", "chromosome" }, null, true );
        if ( dps != null )
            assertFalse ( "Cannot delete from index", dps.length > 0 );

        luceneFacade.addToIndex ( CollectionFactory.getRelativeName ( geneDC,
                module ), true, null, null );

        Gene gene = null;
        geneDC.put ( gene = new Gene ( geneDC, "AbraCadabra" ) );
        System.out.println ( "Add to index gene AbraCadabra" );
        luceneFacade.addToIndex ( CollectionFactory.getRelativeName ( gene,
                module ), true, null, null );

        dps = luceneFacade.search ( CollectionFactory.getRelativeName ( geneDC,
                module ), "AbraCadabra", null, true );
        assertNotNull ( "Cannot add to the index", dps );
        assertTrue ( "Cannot add to the index", dps.length > 0 );

        luceneFacade.deleteFromIndex ( CollectionFactory.getRelativeName (
                gene, module ), null );
        geneDC.remove ( "AbraCadabra" );

        dps = luceneFacade.search ( CollectionFactory.getRelativeName ( geneDC,
                module ), "AbraCadabra", null, true );
        if ( dps != null )
            assertFalse ( "Cannot delete from index", dps.length > 0 );
    }

    protected void testSearch ( ) throws Exception
    {
        dps = luceneFacade.search ( CollectionFactory.getRelativeName ( geneDC,
                module), "meristem", null, null, true);
        assertNotNull("Internal search error during search with altview", dps);
        assertTrue ( "Can not find anything during search", dps.length > 0 );

        dps = luceneFacade.search ( CollectionFactory.getRelativeName ( geneDC,
                module), "meristem", null, null, false);
        assertNotNull("Internal search error during search", dps);
        assertTrue("Can not find anything during search in fields",
                dps.length > 0 );

        dps = luceneFacade.search ( CollectionFactory.getRelativeName ( geneDC,
                module ), "meristem", new String[] { "name", "title",
                "comment", "description", "completeName", "species", "source",
                "regulation", "chromosome" }, null, true );
        assertNotNull("Internal search error during search in fields", dps);
        assertTrue("Can not find anything during search in fields",
                dps.length > 0 );

        dps = luceneFacade.search ( CollectionFactory.getRelativeName ( geneDC,
                module ), "meristem", new String[] { "name", "title",
                "comment", "description", "completeName", "species", "source",
                "regulation", "chromosome" }, null, false );
        assertNotNull("Internal search error during search in fields", dps);
        assertTrue("Can not find anything during search in fields",
                dps.length > 0 );

        dps = luceneFacade.search(CollectionFactory.getRelativeName(geneDC, module), "meri*", null, null, true);
        assertNotNull("Internal search error during search with *", dps);
        assertTrue("Can not find anything during search with *", dps.length > 0);
    }

    protected void testGetSuggestions() throws Exception
    {
        Collection<String> hms = luceneFacade.getSuggestions("Ant", CollectionFactory.getRelativeName(geneDC, module));
        assertTrue("Can not found any 'Ant' suggestions", hms.size() > 0);

        Collection<String> hms2 = luceneFacade.getSuggestions("human", CollectionFactory.getRelativeName(geneDC, module));
        assertTrue("Found some 'human' suggestions", hms2.size() == 0);

    }

    protected void testSaveLoad ( ) throws Exception
    {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        DPSUtils.saveDPSArray(dps, os);

        dps = DPSUtils.loadDPSArray( new ByteArrayInputStream(os.toByteArray()));
        assertNotNull("Cannot serialize preferences", dps);
        assertTrue("Cannot load preferences", dps.length>0);
    }

    protected void testListener ( ) throws Exception
    {
        Gene gene = null;
        geneDC.put ( gene = new Gene ( geneDC, "Hellowean" ) );
        DynamicPropertySet[] dps = luceneFacade.search( CollectionFactory.getRelativeName( geneDC, module ), "Hellowean",
                new String[] {
                "name", "title" }, null, true );
        assertNotNull ( "Update index error", dps );
        assertTrue ( "Updated index is invalid", dps.length > 0 );

        System.out.println ( "Get" + geneDC.get ( "Hellowean" ));

        gene.setComment ( "knownerror" );
        geneDC.put ( gene );
        System.out.println ( ( ( Gene ) geneDC.get ( "Hellowean" ) )
                .getComment ( ) );
                
        dps = luceneFacade.search ( CollectionFactory.getRelativeName ( geneDC,
                module ), "knownerror", new String[] { "comment" },  null, true );
        assertNotNull ( "Changed index error", dps );
        assertTrue ( "Changed index is invalid", dps.length > 0 );

        geneDC.remove ( "Hellowean" );
        dps = luceneFacade.search ( CollectionFactory.getRelativeName ( geneDC,
                module ), "Hellowean", null, true );
        assertFalse ( "Cannot delete entity from index", dps == null ? false
                : dps.length > 0 );

        // luceneDataCollectionListener.removeFromAllCollections(module);
    }

    protected void testDeleteIndex ( ) throws Exception
    {
        luceneFacade.deleteIndex ( null );
        DynamicPropertySet[] client = luceneFacade.search( CollectionFactory.getRelativeName( geneDC, module ), "meristem",
                LuceneQuerySystem.DEFAULT_FORMATTER, true );
        if ( client != null )
            if ( client.length == 0 )
                client = null;
        assertNull ( "Wrong search results", client );
    }

    protected void testSearchView ( ) throws Exception
    {
        LuceneSearchView view = new LuceneSearchView ( "Search View", luceneFacade,
                ( Module ) module, LuceneQuerySystem.DEFAULT_FORMATTER );
        view.show ( );
        while ( true )
            Thread.sleep ( 1000 );
    }

    protected void testIndexEditorDialog ( ) throws Exception
    {
        IndexEditorTest indexEditor = new IndexEditorTest ( luceneFacade, ( Module ) module );
        indexEditor.pack ( );
        indexEditor.centerWindow ( );
        while ( true )
            Thread.sleep ( 1000 );
    }

    @Override
    protected void setUp() throws Exception
    {
        // TODO Auto-generated method stub
        super.setUp();
    }

}
