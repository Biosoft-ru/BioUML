package ru.biosoft.analysis._test;

import java.awt.Dimension;

import javax.swing.JPanel;

import com.developmentontheedge.beans.BeanInfoEx;
import com.developmentontheedge.beans.PropertyDescriptorEx;
import com.developmentontheedge.beans.swing.PropertyInspector;
import com.developmentontheedge.application.Application;
import com.developmentontheedge.application.dialog.OkCancelDialog;

import junit.framework.TestCase;
import junit.framework.TestSuite;

public class BeanExplorerTest extends TestCase
{
    public BeanExplorerTest(String name)
    {
        super(name);
    }

    public static junit.framework.Test suite()
    {
        TestSuite suite = new TestSuite(BeanExplorerTest.class.getName());
        suite.addTest(new BeanExplorerTest("test"));
        return suite;
    }
 
    /**
     * @throws Exception
     */
    public void test() throws Exception
    {
        PropertyInspector pi = new PropertyInspector();
        B b = new B();
        pi.explore(b);
        TestPane pane = new TestPane(pi);
        pane.doModal();
        
    }
    
    public static class TestPane extends OkCancelDialog
    {
        public TestPane(PropertyInspector pi)
        {
            super(Application.getApplicationFrame(), "Warning");
            setPreferredSize(new Dimension(150, 400));
            mainPanel = new JPanel();
            mainPanel.add(pi);
            add(mainPanel);
        }
    }

    public static class B
    {

        public B()
        {
            this.field = new Object[2];
            field[0] = new double[] {1,2};
            field[1] = "s";
        }

        private Object[] field;
        public Object[] getField()
        {
            return this.field;
        }
        public void setField(Object[] field)
        {
            this.field = field;
        }
    }

    public static class BBeanInfo extends BeanInfoEx
    {
        public BBeanInfo(Class<?> type, String name)
        {
            super(type, name);
            beanDescriptor.setDisplayName(getResourceString("CN_CLASS"));
            beanDescriptor.setShortDescription(getResourceString("CD_CLASS"));
        }

        @Override
        public void initProperties() throws Exception
        {
            PropertyDescriptorEx pde;

            pde = new PropertyDescriptorEx("field", beanClass, "getField", "setField");
            add(pde, getResourceString("PN_EXPERIMENT"), getResourceString("PD_EXPERIMENT"));
        }
    }

}
