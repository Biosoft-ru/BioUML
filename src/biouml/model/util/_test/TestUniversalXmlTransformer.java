package biouml.model.util._test;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

import biouml.model.Module;
import biouml.model.util.UniversalXmlTransformer;
import biouml.plugins.biopax.model.BioSource;
import biouml.standard.type.BaseSupport;
import biouml.standard.type.Cell;
import biouml.standard.type.Compartment;
import biouml.standard.type.Complex;
import biouml.standard.type.Gene;
import biouml.standard.type.Protein;
import biouml.standard.type.RNA;
import biouml.standard.type.Reaction;
import biouml.standard.type.Relation;
import biouml.standard.type.SpecieReference;
import biouml.standard.type.Substance;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import one.util.streamex.IntStreamEx;
import ru.biosoft.access.Entry;
import ru.biosoft.access.core.CollectionFactory;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataCollectionConfigConstants;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.MutableDataElementSupport;
import ru.biosoft.access.core.Transformer;
import ru.biosoft.access.core.VectorDataCollection;
import ru.biosoft.access.support.BeanInfoEntryTransformer;
import ru.biosoft.util.serialization.Utils;

public class TestUniversalXmlTransformer extends TestCase
{
    static String repositoryPath = "./data/unittest";
    static Module module;
    static Transformer<Entry, BaseSupport> transformer;

    /** Standart JUnit constructor */
    public TestUniversalXmlTransformer(String name)
    {
        super(name);
    }

    /**
     * Run test in TestRunner.
     * @param args[0] Type of test runner.
     */
    public static void main(String[] args)
    {
        junit.swingui.TestRunner.run(TestUniversalXmlTransformer.class);
    }

    /** Make suite if tests. */
    public static junit.framework.Test suite()
    {
        TestSuite suite = new TestSuite(TestUniversalXmlTransformer.class
                .getName());
        
        suite.addTest(new TestUniversalXmlTransformer("testByCell"));
        suite.addTest(new TestUniversalXmlTransformer("testByCompartment"));
        suite.addTest(new TestUniversalXmlTransformer("testByConcept"));
        suite.addTest(new TestUniversalXmlTransformer("testByGene"));
        suite.addTest(new TestUniversalXmlTransformer("testByProtein"));
        suite.addTest(new TestUniversalXmlTransformer("testByReaction"));
        suite.addTest(new TestUniversalXmlTransformer("testByRelation"));
        suite.addTest(new TestUniversalXmlTransformer("testByRna"));
        suite.addTest(new TestUniversalXmlTransformer("testBySubstance"));
        suite.addTest(new TestUniversalXmlTransformer("testSpecieArray"));
        suite.addTest(new TestUniversalXmlTransformer("testSpecieReference"));
        suite.addTest(new TestUniversalXmlTransformer("testBioSource"));
        suite.addTest(new TestUniversalXmlTransformer("testCell"));
        suite.addTest(new TestUniversalXmlTransformer("testComplex"));

        return suite;
    }

    @Override
    protected void setUp() throws Exception
    {
        super.setUp();

        module = (Module) CollectionFactory.createRepository(repositoryPath);

        transformer = new UniversalXmlTransformer();
        transformer.init(null, null);
    }

    //////////////////////////////////////////////////////////////////
    // Test cases
    //
    public void testXmlTransformer() throws Exception
    {
        //LocalRepository lr = (LocalRepository)CollectionFactory.createRepository(repositoryPath);
        DataCollection<?> parent = (DataCollection<?>) ( (DataCollection<?>)module.get( "Data" ) ).get( "protein" );

        DataElement bs = parent.get(parent.getNameList().get(1));

        Transformer<Entry, BaseSupport> transformer = new UniversalXmlTransformer();
        transformer.init(null, null);

        Entry result = transformer.transformOutput((BaseSupport)bs);

        System.out.println();
        System.out.println(result.getData());
    }

    public void testByCell() throws Exception
    {
        testBySerializedDeserialized("cell");
    }

    public void testByCompartment() throws Exception
    {
        testBySerializedDeserialized("compartment");
    }

    public void testByConcept() throws Exception
    {
        testBySerializedDeserialized("concept");
    }

    public void testByGene() throws Exception
    {
        testBySerializedDeserialized("gene");
    }

    public void testByProtein() throws Exception
    {

        testBySerializedDeserialized("protein");
    }

    public void testByReaction() throws Exception
    {
        testBySerializedDeserialized("reaction");
    }

    public void testByRelation() throws Exception
    {
        testBySerializedDeserialized("relation");
    }

    public void testByRna() throws Exception
    {
        testBySerializedDeserialized("rna");
    }

    public void testBySubstance() throws Exception
    {
        testBySerializedDeserialized("substance");
    }
    
    @SuppressWarnings ( "unchecked" )
    private <T extends DataElement> T getFirstFromRepo(Class<T> clazz) throws Exception
    {
        DataCollection<?> parent = (DataCollection<?>) ((DataCollection<?>) module.get("Data")).get(clazz.getSimpleName().toLowerCase());
        if (parent.getNameList().size() > 0)
        {
            String name = parent.getNameList().get(0);
            T de = (T)parent.get(name);
            return de;
        }
        
        return null;
    }

    private void testBySerializedDeserialized(String dcName) throws Exception
    {
        DataCollection<?> parent = (DataCollection<?>) ((DataCollection<?>) module.get("Data")).get(dcName);
        for (DataElement de : parent)
        {
            Entry serializedDe = transformer.transformOutput((BaseSupport)de);
//            PrintWriter pw = new PrintWriter(new File("c://temp//ser//"+ dcName + ".txt"));
//            pw.write(serializedDe.getData());
//            pw.flush()
            DataElement resurrectedDe = transformer.transformInput(serializedDe);
            
            Set<AnnotatedElement> excludedFields = new HashSet<>();
            Field originField =  Utils.getField(resurrectedDe.getClass(), "ru.biosoft.access.core.MutableDataElementSupport", "origin");
            excludedFields.add(originField);
            assertEquals(true, Utils.areEqual(de, resurrectedDe, excludedFields));
            
            transformer.transformOutput((BaseSupport)resurrectedDe);
            //assertEquals(serializedDe.getData(), serializedAgainDe.getData());
        }
    }

    public void testSpecieReference() throws Exception
    {
        SpecieReference sp = new SpecieReference(null, "name");
        testSerializedDeserializedCorrect(sp);
    }
    
    public void testSpecieArray() throws Exception
    {
        Reaction reaction = new Reaction(null, "bang");
        
        SpecieReference[] arr = IntStreamEx.range( 3 ).mapToObj( i -> new SpecieReference( reaction, "SpecieReference" + i ) )
                .toArray( SpecieReference[]::new );
        
        reaction.setSpecieReferences(arr);
//        reaction.setLiteratureReferences(new String[]{"lit1","lit2"});
        
        testSerializedDeserializedCorrect(reaction);
    }
    
    private void testSerializedDeserializedCorrect(MutableDataElementSupport de) throws Exception
    {
        Entry serializedDe = transformer.transformOutput((BaseSupport)de);
        BaseSupport resurrectedDe = transformer.transformInput(serializedDe);
        
        Set<AnnotatedElement> excludedFields = new HashSet<>();
        Field originField =  Utils.getField(resurrectedDe.getClass(), "ru.biosoft.access.core.MutableDataElementSupport", "origin");
        excludedFields.add(originField);
        
        assertEquals(true, Utils.areEqual(de, resurrectedDe, excludedFields));
        transformer.transformOutput(resurrectedDe);
    }
    
    private void performSerializedDeserialized(DataElement de) throws Exception
    {
        Entry serializedDe = transformer.transformOutput((BaseSupport)de);
        BaseSupport resurrectedDe = transformer.transformInput(serializedDe);
        transformer.transformOutput(resurrectedDe);
    }
    
    private <T extends DataElement> void performSerializedDeserializedViaBeanInfoTrans(
            BeanInfoEntryTransformer<T> beanInfoEntryTransformer, T de) throws Exception
    {
        Entry serializedDe = beanInfoEntryTransformer.transformOutput(de);
        T resurrectedDe = beanInfoEntryTransformer.transformInput(serializedDe);
        beanInfoEntryTransformer.transformOutput(resurrectedDe);
    }
    
    public void testBioSource() throws Exception
    {
        BioSource bioSource = new BioSource(null, "BioSource1");
        testSerializedDeserializedCorrect(bioSource);
    }
    
    public void testCellMany() throws Exception
    {
        final int entityCount = 1000;
        
        Cell cell = getFirstFromRepo(Cell.class);
        System.out.println(getCurrentMethodName()+":UniversalXmlTransformer:started time measurement");
        
        long timeBegin = System.currentTimeMillis();
        
        for (int i = 0; i < entityCount; i++)
        {
            performSerializedDeserialized(cell);
        }
        
        long timeEnd = System.currentTimeMillis();
        long secs = (timeEnd - timeBegin);
        
        System.out.println(getCurrentMethodName() + ":UniversalXmlTransformer:time(secs):" + secs);
        
        cell = getFirstFromRepo(Cell.class);
        BeanInfoEntryTransformer<Cell> beanInfoEntryTransformer = createBeanInfoEntryTransformer(Cell.class);
        System.out.println(getCurrentMethodName()+":BeanInfoTransformer:started time measurement");
        
        timeBegin = System.currentTimeMillis();
        
        for (int i = 0; i < entityCount; i++)
        {
            performSerializedDeserializedViaBeanInfoTrans(beanInfoEntryTransformer, cell);
        }
        
        timeEnd = System.currentTimeMillis();
        secs = (timeEnd - timeBegin);
        
        System.out.println(getCurrentMethodName() + ":BeanInfoTransformer:time(secs):" + secs);
    }
    
    private <T extends DataElement> BeanInfoEntryTransformer<T> createBeanInfoEntryTransformer(Class<T> clazz)
    {
        BeanInfoEntryTransformer<T> beanInfoEntryTransformer = new BeanInfoEntryTransformer<>();
        
        Properties props = new Properties ( );
        props.setProperty ( DataCollectionConfigConstants.NAME_PROPERTY, "DynamicModuleExample" );
        props.setProperty ( DataCollectionConfigConstants.DATA_ELEMENT_CLASS_PROPERTY, clazz.getName() );
        beanInfoEntryTransformer.init(new VectorDataCollection<T>(null, props), new VectorDataCollection<T>(null, props));
        return beanInfoEntryTransformer;
    }
    
    public void testGeneMany() throws Exception
    {
        final int entityCount = 1000;
        
        System.out.println(getCurrentMethodName()+":UniversalXmlTransformer:started time measurement");
        
        long timeBegin = System.currentTimeMillis();
        
        Gene reaction = getFirstFromRepo(Gene.class);
        for (int i = 0; i < entityCount; i++)
        {
            performSerializedDeserialized(reaction);
        }
        
        long timeEnd = System.currentTimeMillis();
        long secs = (timeEnd - timeBegin);
        
        System.out.println(getCurrentMethodName() + ":UniversalXmlTransformer:time(secs):" + secs);
        
        System.out.println(getCurrentMethodName()+":BeanInfoTransformer:started time measurement");
        
        BeanInfoEntryTransformer<Gene> beanInfoEntryTransformer = createBeanInfoEntryTransformer(Gene.class);
        reaction = getFirstFromRepo(Gene.class);
        
        timeBegin = System.currentTimeMillis();
        
        for (int i = 0; i < entityCount; i++)
        {
            performSerializedDeserializedViaBeanInfoTrans(beanInfoEntryTransformer, reaction);
        }
        
        timeEnd = System.currentTimeMillis();
        secs = (timeEnd - timeBegin);
        
        System.out.println(getCurrentMethodName() + ":BeanInfoTransformer:time(secs):" + secs);
    }
    
    private <T extends DataElement> void testMany(Class<T> clazz, int entityCount) throws Exception
    {
        System.out.println(getCallerMethodName()+":UniversalXmlTransformer:started time measurement");
        
        long timeBegin = System.currentTimeMillis();
        
        T dataElement = getFirstFromRepo(clazz);
        for (int i = 0; i < entityCount; i++)
        {
            performSerializedDeserialized(dataElement);
        }
        
        long timeEnd = System.currentTimeMillis();
        long secs = (timeEnd - timeBegin);
        
        System.out.println(getCallerMethodName() + ":UniversalXmlTransformer:time(secs):" + secs);
        
        System.out.println(getCallerMethodName()+":BeanInfoTransformer:started time measurement");
        
        BeanInfoEntryTransformer<T> beanInfoEntryTransformer = createBeanInfoEntryTransformer(clazz);
        dataElement = getFirstFromRepo(clazz);
        
        timeBegin = System.currentTimeMillis();
        
        for (int i = 0; i < entityCount; i++)
        {
            performSerializedDeserializedViaBeanInfoTrans(beanInfoEntryTransformer, dataElement);
        }
        
        timeEnd = System.currentTimeMillis();
        secs = (timeEnd - timeBegin);
        
        System.out.println(getCallerMethodName() + ":BeanInfoTransformer:time(secs):" + secs);
    }
    
    public void testRnaMany() throws Exception
    {
        testMany(RNA.class, 1000);
    }
    
    public void testGeneMany2() throws Exception
    {
        testMany(Gene.class, 1000);
    }
    
    public void testRelationMany() throws Exception
    {
        testMany(Relation.class, 1000);
    }
    
    public void testProteinMany() throws Exception
    {
        testMany(Protein.class, 1000);
    }
    
    public void testLiteratureMany() throws Exception
    {
    }
    
    public void testCompartmentMany() throws Exception
    {
        testMany(Compartment.class, 1000);
    }
    
    public void testSubstanceMany() throws Exception
    {
        final int entityCount = 1000;
        
        System.out.println(getCurrentMethodName()+":UniversalXmlTransformer:started time measurement");
        
        Substance substance = getFirstFromRepo(Substance.class);
        
        long timeBegin = System.currentTimeMillis();
        
        for (int i = 0; i < entityCount; i++)
        {
            performSerializedDeserialized(substance);
        }
        
        long timeEnd = System.currentTimeMillis();
        long secs = (timeEnd - timeBegin);
        
        System.out.println(getCurrentMethodName() + ":UniversalXmlTransformer:time(secs):" + secs);
        
        System.out.println(getCurrentMethodName()+":BeanInfoTransformer:started time measurement");
        
        BeanInfoEntryTransformer<Substance> beanInfoEntryTransformer = createBeanInfoEntryTransformer(Substance.class);
        substance = getFirstFromRepo(Substance.class);
        
        timeBegin = System.currentTimeMillis();
        
        for (int i = 0; i < entityCount; i++)
        {
            performSerializedDeserializedViaBeanInfoTrans(beanInfoEntryTransformer, substance);
        }
        
        timeEnd = System.currentTimeMillis();
        secs = (timeEnd - timeBegin);
        
        System.out.println(getCurrentMethodName() + ":BeanInfoTransformer:time(secs):" + secs);
    }
    
    private String getCurrentMethodName()
    {
        StackTraceElement [] callStack = new Throwable().getStackTrace();
        return callStack[1].getMethodName();
    }
    
    private String getCallerMethodName()
    {
        StackTraceElement [] callStack = new Throwable().getStackTrace();
        return callStack[2].getMethodName();
    }
    
    private Cell createCell()
    {
//        DatabaseReference[] dbRefs = new DatabaseReference[3];
//        for (int i = 0; i < dbRefs.length; i++)
//        {
//            dbRefs[i] = new DatabaseReference();
//            dbRefs[i].setVersion(""+i);
//            dbRefs[i].setInfo("info"+i);
//        }
        
        Cell cell = new Cell(null, "Cell");
//        cell.setDatabaseReferences(dbRefs);
        
        return cell;
    }
    
    public void testCell() throws Exception
    {
        Cell cell = createCell();
        testSerializedDeserializedCorrect(cell);
    }
    
    public void testComplex() throws Exception
    {
        Complex cmpl = new Complex(null, "Complex");
        cmpl.setDate("2008.02.01");
        testSerializedDeserializedCorrect(cmpl);
    }

    public void testXmlEncoder() throws Exception
    {
        Reaction de = new Reaction(null, "my reaction");
        de.setComment("comment");
        de.setDescription("description");
        //de.setSpecieReferences(new SpecieReference[] {new SpecieReference(null, "sr1")});

        Transformer<Entry, BaseSupport> transformer = new UniversalXmlTransformer();
        transformer.init(null, null);

        Entry result = transformer.transformOutput(de);

        System.out.println();
        System.out.println(result.getData());

        Reaction out = (Reaction) transformer.transformInput(result);

        System.out.println();
        System.out.println(de.getName() + " = " + out.getName());
        System.out.println(de.getOrigin() + " = " + out.getOrigin());
        System.out.println(de.getComment() + " = " + out.getComment());
        System.out.println(de.getDescription() + " = " + out.getDescription());
    }
}