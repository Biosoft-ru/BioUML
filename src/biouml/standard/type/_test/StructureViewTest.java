package biouml.standard.type._test;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.Image;
import java.util.List;

import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import com.developmentontheedge.application.ApplicationUtils;

import biouml.standard.type.CDKRenderer;
import biouml.standard.type.Structure;
import junit.framework.TestSuite;
import ru.biosoft.access._test.AbstractBioUMLTest;
import ru.biosoft.access.core.CollectionFactory;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.graphics.CompositeView;
import ru.biosoft.graphics.editor.ViewPane;

public class StructureViewTest extends AbstractBioUMLTest
{
    private static final String repositoryPath = "../data_resources";
    private static final DataElementPath structuresPath = DataElementPath.create( "data/Collaboration/Net2Drug/Data/Structures/Example" );

    public StructureViewTest(String name)
    {
        super(name);
    }

    public static junit.framework.Test suite()
    {
        TestSuite suite = new TestSuite(StructureViewTest.class.getName());
        suite.addTest(new StructureViewTest("test"));
        return suite;
    }

    /**
     * @throws Exception
     */
    public void test() throws Exception
    {
        CollectionFactory.createRepository(repositoryPath);
        DataCollection<Structure> structures = structuresPath.getDataCollection( Structure.class );
        List<String> names = structures.getNameList();

        JFrame frame = new JFrame();
        frame.setSize(600, 400);

        JPanel mainPanel = new JPanel(new GridLayout(0, 2));

        Graphics2D g = ApplicationUtils.getGraphics();
        int size = 50;

        for( int i = 0; ( i < size ) && ( i < names.size() ); i++ )
        {
            try
            {
                Structure structure = structures.get(names.get(i));

                //view
                CompositeView view = CDKRenderer.createStructureView(structure, new Dimension(400, 200), g);
                ViewPane pane = new ViewPane();
                pane.setView(view);
                mainPanel.add(pane);

                //image
                Image image = CDKRenderer.createStructureImage(structure, new Dimension(600, 400));
                JLabel imagePane = new JLabel(new ImageIcon(image));
                mainPanel.add(imagePane);
            }
            catch( Exception e )
            {
                System.err.println("Error structure: " + names.get(i));
                e.printStackTrace();
            }
        }

        JScrollPane scrollPane = new JScrollPane(mainPanel, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        frame.add(scrollPane, BorderLayout.CENTER);
        frame.setVisible(true);
        frame.setResizable(true);

        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        while( true )
        {
            Thread.sleep(1000);
        }
    }
}
