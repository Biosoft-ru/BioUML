package ru.biosoft.treetable._test;

import java.awt.Container;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.JFrame;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;

import junit.framework.TestCase;
import junit.framework.TestSuite;
import ru.biosoft.access.core.CollectionFactory;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.plugins.javascript.JScriptContext;
import ru.biosoft.plugins.javascript.host.JavaScriptData;
import ru.biosoft.treetable.TreeTableDocument;
import ru.biosoft.treetable.TreeTableElement;

public class TreeTableViewTest extends TestCase
{
    static final String dataDirectory = "../data/";
    static final String resourcesDirectory = "../data_resources/";

    /** Standart JUnit constructor */
    public TreeTableViewTest(String name)
    {
        super(name);
    }

    public static void main(String[] args)
    {
        junit.swingui.TestRunner.run(TreeTableViewTest.class);
    }

    /** Make suite if tests. */
    public static junit.framework.Test suite()
    {
        TestSuite suite = new TestSuite(TreeTableViewTest.class.getName());
        suite.addTest(new TreeTableViewTest("testTreeTable"));
        return suite;
    }

    //////////////////////////////////////////////////////////////////
    // Test cases
    //

    public void testTreeTable() throws Exception
    {
        CollectionFactory.createRepository(dataDirectory);
        CollectionFactory.createRepository(resourcesDirectory);

        JScriptContext.getContext();
        ScriptableObject scope = JScriptContext.getScope();
        Scriptable scriptable = Context.toObject(new JavaScriptData(), scope);
        scope.put("data", scope, scriptable);

        TreeTableElement model = new TreeTableElement("test", null);
        model.setTreePath(DataElementPath.create("databases/GTRD/classification/huTF"));
        model.setTableScript("data.get(\"data/Collaboration/test/Data/test\")");
        TreeTableDocument document = new TreeTableDocument(model);

        JFrame frame = new JFrame("TreeTable test");
        frame.show();
        Container content = frame.getContentPane();

        content.add(document.getViewPane());
        frame.setSize(600, 600);
        frame.addWindowListener(new WindowAdapter()
        {
            @Override
            public void windowClosed(WindowEvent e)
            {
                System.exit(0);
            }
        });
        while( true )
        {
            Thread.sleep(100);
        }
    }
}