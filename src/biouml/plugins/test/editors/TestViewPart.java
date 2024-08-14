package biouml.plugins.test.editors;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import java.util.logging.Level;
import java.util.logging.Logger;

import biouml.model.Diagram;
import biouml.plugins.test.AcceptanceTestSuite;
import biouml.plugins.test.TestDocument;
import biouml.plugins.test.TestModel;
import biouml.plugins.test.tests.ExperimentValueTest;
import biouml.plugins.test.tests.IntervalTest;
import biouml.plugins.test.tests.SteadyStateTest;
import biouml.plugins.test.tests.Test;
import biouml.standard.simulation.SimulationResult;

import com.developmentontheedge.beans.swing.PropertyInspectorEx;
import com.developmentontheedge.application.action.ActionInitializer;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.exception.ExceptionRegistry;
import ru.biosoft.gui.Document;
import ru.biosoft.gui.ViewPartSupport;

/**
 * Main view part for editing test document
 */
public class TestViewPart extends ViewPartSupport implements ListSelectionListener
{
    protected static final Logger log = Logger.getLogger(TestViewPart.class.getName());

    public static final String TEST_FOLDER = "test";

    public static final String CREATE_SUITE = "Create new test suite";
    public static final String CREATE_TEST = "Create test for suite";
    public static final String REMOVE_TEST = "Remove test or suite";
    public static final String RUN_TESTS = "Run tests";

    protected Action createAction = new CreateAction(CREATE_SUITE);
    protected Action createTestAction = new CreateTestAction(CREATE_TEST);
    protected Action removeTestAction = new RemoveTestAction(REMOVE_TEST);
    protected Action runTestsAction = new RunTestsAction(RUN_TESTS);

    protected AcceptanceTestSuite currentTestSuite = null;
    protected Test currentTest = null;

    protected JPanel mainPanel;

    public TestViewPart()
    {
        mainPanel = new JPanel();
        mainPanel.setLayout(new BorderLayout());
        add(mainPanel, BorderLayout.CENTER);

        createTestAction.setEnabled(false);
    }

    @Override
    public void explore(Object model, Document document)
    {
        this.model = model;
        this.document = document;
        ( (TestDocument)document ).addSelectionListener(this);
    }

    @Override
    public boolean canExplore(Object model)
    {
        if( model instanceof TestModel )
        {
            return true;
        }
        return false;
    }

    @Override
    public void valueChanged(ListSelectionEvent e)
    {
        currentTestSuite = ( (TestDocument)document ).getSelectedTestSuite();
        createTestAction.setEnabled(currentTestSuite != null);

        currentTest = ( (TestDocument)document ).getSelectedTest();

        Test test = ( (TestDocument)document ).getSelectedTest();
        if( test != null )
        {
            updatePropertyInspector(test);
        }
        else
        {
            updatePropertyInspector(currentTestSuite);
        }
    }

    protected void updatePropertyInspector(Object object)
    {
        if( object != null )
        {
            mainPanel.removeAll();
            PropertyInspectorEx inspector = new PropertyInspectorEx();
            inspector.explore(object);
            JScrollPane scrollPane = new JScrollPane(inspector);
            mainPanel.add(scrollPane, BorderLayout.CENTER);
        }
        repaint();
        validate();
    }

    ///////////////////////////////////////////////////////////////////////////
    // Actions
    //
    @Override
    public Action[] getActions()
    {
        ActionInitializer initializer = new ActionInitializer(MessageBundle.class);
        initializer.initAction(createAction, CREATE_SUITE);
        initializer.initAction(createTestAction, CREATE_TEST);
        initializer.initAction(removeTestAction, REMOVE_TEST);
        initializer.initAction(runTestsAction, RUN_TESTS);

        Action[] action = new Action[] {createAction, createTestAction, removeTestAction, runTestsAction};

        return action;
    }

    class CreateAction extends AbstractAction
    {
        public CreateAction(String name)
        {
            super(name);
        }

        @Override
        public void actionPerformed(ActionEvent e)
        {
            DataCollection targetDC = ( (DataElement)model ).getOrigin();
            NewTestSuiteDialog dialog = new NewTestSuiteDialog(targetDC);
            if( dialog.doModal() )
            {
                DataElementPath path = dialog.getPath();
                AcceptanceTestSuite testSuite = null;
                try
                {
                    if( path.exists() )
                    {
                        testSuite = path.getDataElement(AcceptanceTestSuite.class);
                    }
                    else
                    {
                        testSuite = new AcceptanceTestSuite(path.optParentCollection(), path.getName());
                        path.save(testSuite);
                    }
                    ( (TestModel)model ).addAcceptanceTestSuite(testSuite);
                }
                catch( Exception ex )
                {
                    log.log(Level.SEVERE, "Unable to create test suite: "+ExceptionRegistry.log(ex));
                }
            }
        }
    }

    class CreateTestAction extends AbstractAction
    {
        public static final String STEADY_STATE = "Steady state";
        public static final String INTERVAL = "Interval";
        public static final String EXPERIMENT_VALUE = "Experiment value";

        public CreateTestAction(String name)
        {
            super(name);
        }

        @Override
        public void actionPerformed(ActionEvent e)
        {
            if( currentTestSuite != null )
            {
                String[] testTypes = new String[] {STEADY_STATE, INTERVAL, EXPERIMENT_VALUE};
                String type = (String)JOptionPane.showInputDialog(TestViewPart.this, "Please select test type", "Tests",
                        JOptionPane.INFORMATION_MESSAGE, null, testTypes, STEADY_STATE);

                Test test = null;
                if( type.equals(STEADY_STATE) )
                {
                    test = new SteadyStateTest();
                }
                else if( type.equals(INTERVAL) )
                {
                    test = new IntervalTest();
                }
                else
                {
                    test = new ExperimentValueTest();
                }
                currentTestSuite.addTest(test);
                document.update();
            }
        }
    }

    class RemoveTestAction extends AbstractAction
    {
        public RemoveTestAction(String name)
        {
            super(name);
        }

        @Override
        public void actionPerformed(ActionEvent e)
        {
            if( currentTest != null )
            {
                currentTestSuite.removeTest(currentTest);
                document.update();
            }
            else if( currentTestSuite != null )
            {
                ( (TestModel)model ).removeAcceptanceTestSuite(currentTestSuite);
                document.update();
            }
        }
    }

    class RunTestsAction extends AbstractAction
    {
        public RunTestsAction(String name)
        {
            super(name);
        }

        @Override
        public void actionPerformed(ActionEvent e)
        {
            DataElementPath modelPath = ( (TestModel)model ).getModelPath();
            Diagram diagram = (Diagram)modelPath.optDataElement();
            for( AcceptanceTestSuite testSuite : ( (TestModel)model ).getAcceptanceTests() )
            {
                testSuite.setStatus(null);
                testSuite.setDuration(0);
            }
            document.update();
            for( AcceptanceTestSuite testSuite : ( (TestModel)model ).getAcceptanceTests() )
            {
                SimulationResult result = testSuite.test(diagram);
                if( result != null )
                {
                    ( (TestModel)model ).addSimulationResult(testSuite, result);
                }
                document.update();
            }
        }
    }
}
