package ru.biosoft.access.support._test;

import java.util.Arrays;

import junit.framework.Test;
import junit.framework.TestSuite;
import ru.biosoft.access._test.AbstractBioUMLTest;
import ru.biosoft.access.core.CollectionFactory;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.support.DataCollectionMultyChoiceDialog;
import ru.biosoft.access.support.DataCollectionMultyChoicePane;
import ru.biosoft.access.support.NewDataElementDialog;

/** Batch unit test for ... . */
public class MultyChoicePaneTest extends AbstractBioUMLTest
{
    private static DataCollection dc;

    /** Standart JUnit constructor */
    public MultyChoicePaneTest(String name)
    {
        super(name);
    }

    /** Make suite if tests. */
    public static Test suite()
    {
        TestSuite suite = new TestSuite(MultyChoicePaneTest.class.getName());

        suite.addTest(new MultyChoicePaneTest("testInitData"));
        suite.addTest(new MultyChoicePaneTest("testShowPane"));
        suite.addTest(new MultyChoicePaneTest("testShowDialog"));
        suite.addTest(new MultyChoicePaneTest("testNewDataElementDialog"));
        
        return suite;
    }

    //////////////////////////////////////////////////////////////////
    // Test cases
    //

    public void testInitData() throws Exception
    {
        // create repository
        DataCollection repository = CollectionFactory.createRepository("../data/test/ru/biosoft/access/support/data");
        assertNotNull("Can't initialize repository", repository);

        dc = CollectionFactory.getDataCollection( "databases/GeneNet/Data/gene" );
        assertNotNull("Can't initialize data collection", dc);
    }

    public void testShowPane()
    {
        DataCollectionMultyChoicePane choicePane = new DataCollectionMultyChoicePane( dc, new String[0], true );

        javax.swing.JFrame frame = new javax.swing.JFrame("Test");
        frame.getContentPane().add(choicePane);
        frame.pack();
        frame.setVisible(true);
    }

    public void testShowDialog()
    {
        DataCollectionMultyChoiceDialog dialog = new DataCollectionMultyChoiceDialog( null, "Test", dc, new String[0], true );

        if( dialog.doModal() )
            System.out.println("Selected values: " + Arrays.asList(dialog.getSelectedValues()));
    }

    public void testNewDataElementDialog()
    {
        NewDataElementDialog dialog = new NewDataElementDialog(null, "Test", dc);

        if( dialog.doModal() )
            System.out.println("New data element: " + dialog.getNewDataElement());
    }
}