package biouml.plugins.test;

import java.beans.PropertyDescriptor;
import java.util.logging.Level;
import java.util.Properties;
import java.util.ResourceBundle;

import java.util.logging.Logger;

import com.developmentontheedge.beans.BeanInfoEx;
import com.developmentontheedge.beans.PropertyDescriptorEx;

import biouml.plugins.test.tests.Test;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElementSupport;
import ru.biosoft.access.core.VectorDataCollection;
import ru.biosoft.plugins.javascript.JSCommand;

public class TableTestModelWrapper extends VectorDataCollection
{
    protected static final Logger log = Logger.getLogger(TableTestModelWrapper.class.getName());

    protected TestModel testModel;

    protected MessageBundle messages = (MessageBundle)ResourceBundle.getBundle(MessageBundle.class.getName());

    public TableTestModelWrapper(TestModel testModel, Properties properties)
    {
        super(null, properties);
        this.testModel = testModel;

        updateModel();
    }

    /**
     * Recreate table content for model
     */
    public void updateModel()
    {
        elements.clear();
        if( vectorNameList != null )
            vectorNameList.clear();

        try
        {
            AcceptanceTestSuite[] testSuits = testModel.getAcceptanceTests();
            if( testSuits != null )
            {
                for( AcceptanceTestSuite testSuit : testSuits )
                {
                    String name = testSuit.getName();
                    doPut(new TestRow(name, this, testSuit, null, testModel), true);
                    int i = 0;
                    for( Test test : testSuit.getTests() )
                    {
                        name = testSuit.getName() + "_" + ( i++ );
                        doPut(new TestRow(name, this, testSuit, test, testModel), true);
                    }
                }
            }
        }
        catch( Exception e )
        {
            log.log(Level.SEVERE, "Cannot create tests table", e);
        }
    }

    /**
     * TODO: extend from RowDataElement
     */
    public static class TestRow extends DataElementSupport
    {
        protected AcceptanceTestSuite testSuite;
        protected Test test;
        protected TestModel model;

        public TestRow(String name, DataCollection origin, AcceptanceTestSuite testSuite, Test test, TestModel model)
        {
            super(name, origin);
            this.testSuite = testSuite;
            this.test = test;
            this.model = model;
        }

        public AcceptanceTestSuite getTestSuite()
        {
            return testSuite;
        }

        public Test getTest()
        {
            return test;
        }

        public String getTestName()
        {
            if( test != null )
                return "";
            else
                return testSuite.getName();
        }

        public String getState()
        {
            return testSuite.getStateName();
        }

        public String getInfo()
        {
            if( test != null )
            {
                return test.getInfo();
            }
            return "";
        }

        public long getTime()
        {
            return testSuite.getTimeLimit();
        }

        public String getStatus()
        {
            if( test != null && test.getStatus() != null )
            {
                return test.getStatus().toString();
            }
            else if( testSuite.getStatus() != null )
            {
                return testSuite.getStatus().toString();
            }
            return null;
        }

        public long getDuration()
        {
            return testSuite.getDuration();
        }

        public String getError()
        {
            if( test != null )
            {
                return test.getError();
            }
            return null;
        }

        public JSCommand getPlot()
        {
            if( test != null )
            {
                return new JSCommand(new JSCommand.CommandGenerator()
                {
                    @Override
                    public boolean isEmpty()
                    {
                        return ( model.getSimulationResult(test) == null );
                    }

                    @Override
                    public String generateCommand()
                    {
                        return test.generateJavaScript(model);
                    }
                });
            }
            return null;
        }

        public void setPlot(JSCommand command)
        {
        }

        public boolean isTestSuite()
        {
            return ( test == null );
        }
    }

    public static class TestRowBeanInfo extends BeanInfoEx
    {
        public TestRowBeanInfo()
        {
            super(TestRow.class, "biouml.plugins.test.MessageBundle");
            beanDescriptor.setDisplayName(getResourceString("CN_ROW_TEST"));
            beanDescriptor.setShortDescription(getResourceString("CD_ROW_TEST"));
        }

        @Override
        public void initProperties() throws Exception
        {
            PropertyDescriptor pd = new PropertyDescriptorEx("name", beanClass, "getTestName", null);
            add(pd, getResourceString("PN_ROW_TEST_NAME"), getResourceString("PD_ROW_TEST_NAME"));

            pd = new PropertyDescriptorEx("state", beanClass, "getState", null);
            add(pd, getResourceString("PN_ROW_TEST_STATE"), getResourceString("PD_ROW_TEST_STATE"));

            pd = new PropertyDescriptorEx("info", beanClass, "getInfo", null);
            add(pd, getResourceString("PN_ROW_TEST_INFO"), getResourceString("PD_ROW_TEST_INFO"));

            pd = new PropertyDescriptorEx("time", beanClass, "getTime", null);
            add(pd, getResourceString("PN_ROW_TEST_TIME"), getResourceString("PD_ROW_TEST_TIME"));

            pd = new PropertyDescriptorEx("duration", beanClass, "getDuration", null);
            add(pd, getResourceString("PN_ROW_TEST_DURATION"), getResourceString("PD_ROW_TEST_DURATION"));

            pd = new PropertyDescriptorEx("error", beanClass, "getError", null);
            add(pd, getResourceString("PN_ROW_TEST_ERROR"), getResourceString("PD_ROW_TEST_ERROR"));

            pd = new PropertyDescriptorEx("status", beanClass, "getStatus", null);
            add(pd, getResourceString("PN_ROW_TEST_STATUS"), getResourceString("PD_ROW_TEST_STATUS"));

            pd = new PropertyDescriptorEx("plot", beanClass, "getPlot", "setPlot");
            add(pd, getResourceString("PN_ROW_TEST_PLOT"), getResourceString("PD_ROW_TEST_PLOT"));
        }
    }
}
