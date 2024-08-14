package ru.biosoft.access.generic._test;

import junit.framework.TestCase;
import junit.framework.TestSuite;
import ru.biosoft.access.core.CollectionFactory;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.generic.GenericDataCollection;

public class GenericDataCollectionTest extends TestCase
{
    public static final String repositoryPath = "../data";
    public static final String repositoryPath2 = "../data_resources";

    /** Standart JUnit constructor */
    public GenericDataCollectionTest(String name)
    {
        super(name);
        try
        {
            CollectionFactory.createRepository(repositoryPath);
            CollectionFactory.createRepository(repositoryPath2);
        }
        catch( Exception e )
        {
        }
    }

    /** Make suite if tests. */
    public static junit.framework.Test suite()
    {
        TestSuite suite = new TestSuite(GenericDataCollectionTest.class.getName());

        //suite.addTest(new GenericDataCollectionTest("testPutElement"));
        //suite.addTest(new GenericDataCollectionTest("testGetElement"));
        suite.addTest(new GenericDataCollectionTest("testPutDiagram"));
        //suite.addTest(new GenericDataCollectionTest("testPutTable"));
        return suite;
    }

    //////////////////////////////////////////////////////////////////
    // Test cases
    //


    public void testPutElement() throws Exception
    {
        DataElement de = CollectionFactory.getDataElement("databases/Biopath/Data/gene/GEN000005");
        assertNotNull("can not find element to add", de);

        //ru.biosoft.access.core.DataElement de2 = CollectionFactory.getDataElement("databases/Biopath/Data/protein/PRT000040");
        //assertNotNull("can not find element to add", de2);

        GenericDataCollection genericDC = (GenericDataCollection)CollectionFactory.getDataElement("data/user_repository/generic");
        assertNotNull("can not find generic collection", genericDC);

        genericDC.put(de);
        //genericDC.put(de2);

        assertNotNull("element is null", genericDC.get(de.getName()));
        //assertNotNull("element is null", genericDC.get(de2.getName()));
    }

    public void testGetElement() throws Exception
    {
        GenericDataCollection genericDC = (GenericDataCollection)CollectionFactory.getDataElement("data/user_repository/generic");
        assertNotNull("can not find generic collection", genericDC);

        DataElement de = genericDC.get("GEN000005");
        assertNotNull("element is null", de);
    }

    public void testPutDiagram() throws Exception
    {
        DataElement de = CollectionFactory.getDataElement("databases/Biopath/Diagrams/DGR0004");
        assertNotNull("can not find diagram to add", de);

        GenericDataCollection genericDC = (GenericDataCollection)CollectionFactory.getDataElement("data/user_repository/generic");
        assertNotNull("can not find generic collection", genericDC);

        genericDC.put(de);

        DataElement returnDE = genericDC.get(de.getName());
        assertNotNull("element is null", returnDE);
    }
    
    public void testPutTable() throws Exception
    {
        DataElement de = CollectionFactory.getDataElement("data/microarray/0.1uM_RITA_2.4.6_Hyp&Stud&Wilk");
        assertNotNull("can not find table to add", de);

        GenericDataCollection genericDC = (GenericDataCollection)CollectionFactory.getDataElement("data/user_repository/generic");
        assertNotNull("can not find generic collection", genericDC);

        genericDC.put(de);

        DataElement returnDE = genericDC.get(de.getName());
        assertNotNull("element is null", returnDE);
    }
}