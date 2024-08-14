package biouml.workbench._test;

import java.awt.Dimension;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.StringWriter;

import javax.swing.JEditorPane;
import javax.swing.JFrame;

import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;

import com.developmentontheedge.application.ApplicationUtils;
import com.developmentontheedge.beans.web.HtmlBeanGenerator;

import junit.framework.TestCase;
import junit.framework.TestSuite;
import ru.biosoft.access.core.CollectionFactory;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.DataElementPath;

public class HtmlViewPaneTest extends TestCase
{
    public static final String repositoryPath = "../data";

    /** Standart JUnit constructor */
    public HtmlViewPaneTest(String name)
    {
        super(name);
    }

    /** Make suite if tests. */
    public static junit.framework.Test suite()
    {
        TestSuite suite = new TestSuite(HtmlViewPaneTest.class.getName());

        suite.addTest(new HtmlViewPaneTest("testSimpleHtmlView"));
        //suite.addTest(new HtmlViewPaneTest("testSpeed"));
        return suite;
    }

    //////////////////////////////////////////////////////////////////
    // Test cases
    //


    public void testSimpleHtmlView() throws Exception
    {
        JFrame frame = new JFrame("Html rendering test");
        frame.setPreferredSize(new Dimension(600, 600));
        frame.show();

        CollectionFactory.createRepository(repositoryPath);
        //DataElement de = CollectionFactory.getDataElement("databases/Biopath/Data/gene/GEN000005");
        DataElement de = CollectionFactory.getDataElement("databases/Biopath/Data/reaction/RCT001494");

        JEditorPane panel = new JEditorPane("text/html", "");

        Velocity.init();
        VelocityContext context = new VelocityContext();
        context.put("de", de);

        StringWriter sw = new StringWriter();
        Template template = Velocity.getTemplate("beaninfotemplate.vm");
        template.merge(context, sw);

        panel.setText(sw.toString());

        frame.setContentPane(panel);
        frame.pack();

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

    public void testSpeed() throws Exception
    {
        CollectionFactory.createRepository(repositoryPath);
        DataElement de1 = CollectionFactory.getDataElement("databases/Biopath/Data/gene/GEN000005");
        DataElement de2 = CollectionFactory.getDataElement("databases/Biopath/Data/reaction/RCT001494");
        DataElement de3 = CollectionFactory.getDataElement("databases/Biopath/Data/protein/PRT000007");
        DataElement de4 = CollectionFactory.getDataElement("databases/Biopath/Data/relation/RLT001464");

        deSpeedTest(de1);
        deSpeedTest(de2);
        deSpeedTest(de3);
        deSpeedTest(de4);
    }

    protected void deSpeedTest(DataElement de) throws Exception
    {
        System.out.println(DataElementPath.create(de));
        HtmlBeanGenerator beanGenerator = new HtmlBeanGenerator();
        Velocity.init();
        for( int repeat = 1; repeat < 10000; repeat *= 10 )
        {
            long start = System.currentTimeMillis();
            for( int i = 0; i < repeat; i++ )
            {
                beanGenerator.generate(de, new StringWriter());
            }
            long beTime = System.currentTimeMillis() - start;

            start = System.currentTimeMillis();
            String templateString = ApplicationUtils.readAsString( new File("beaninfotemplate.vm") );
            for( int i = 0; i < repeat; i++ )
            {
                VelocityContext context = new VelocityContext();
                context.put("de", de);

                Velocity.evaluate(context, new StringWriter(), "velocity", templateString);
            }
            long velocityTime2 = System.currentTimeMillis() - start;

            start = System.currentTimeMillis();
            Template template = Velocity.getTemplate("beaninfotemplate.vm");
            for( int i = 0; i < repeat; i++ )
            {
                VelocityContext context = new VelocityContext();
                context.put("de", de);
                template.merge(context, new StringWriter());
            }
            long velocityTime = System.currentTimeMillis() - start;

            System.out.println("    count: " + repeat + "\t BE: " + beTime + "\t Velocity: " + velocityTime + "\t Velocity2: "
                    + velocityTime2);
        }
    }
}