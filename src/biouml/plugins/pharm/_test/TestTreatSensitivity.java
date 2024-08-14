package biouml.plugins.pharm._test;

import java.io.BufferedWriter;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.developmentontheedge.application.ApplicationUtils;

import biouml.model.Diagram;
import biouml.model.SubDiagram;
import biouml.model.dynamics.EModel;
import biouml.model.dynamics.Variable;
import biouml.plugins.agentmodeling.AgentModelSimulationEngine;
import biouml.plugins.agentmodeling.AgentSimulationEngineWrapper;
import biouml.plugins.simulation.Model;
import biouml.plugins.simulation.SimulationEngine;
import biouml.plugins.simulation.ode.jvode.JVodeSolver;
import biouml.standard.diagram.DiagramUtility;
import biouml.standard.diagram.Util;
import biouml.standard.simulation.ResultListener;
import junit.framework.TestSuite;
import one.util.streamex.StreamEx;
import ru.biosoft.access._test.AbstractBioUMLTest;
import ru.biosoft.access.core.CollectionFactory;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElement;

public class TestTreatSensitivity extends AbstractBioUMLTest implements ResultListener
{
    public TestTreatSensitivity(String name)
    {
        super(name);
    }

    public static junit.framework.Test suite()
    {
        TestSuite suite = new TestSuite(TestTreatSensitivity.class.getName());
        suite.addTest(new TestTreatSensitivity("test"));
        return suite;
    }


    private Diagram getDiagram() throws Exception
    {
        CollectionFactory.createRepository("../data_resources");
        DataCollection collection = CollectionFactory.getDataCollection("data/Collaboration (git)/Cardiovascular system/Complex model/");
        DataElement de = collection.get("Complex model new Enalapril");
        return (Diagram)de;
    }

    private String resultFolder = "/Treatment sensitivity/Enalapril/";

    private String resultPSup = resultFolder + "PS_up.txt";
    private String resultPDup = resultFolder + "PD_up.txt";
    private String resultHRup = resultFolder + "HR_up.txt";

    private String resultPSdown = resultFolder + "PS_down.txt";
    private String resultPDdown = resultFolder + "PD_down.txt";
    private String resultHRdown = resultFolder + "HR_down.txt";

    private String[] selected = new String[] {"Heart/P_S", "Heart/P_D", "Heart/Heart_Rate"};

    public void test() throws Exception
    {
        Diagram d = getDiagram();

        try
        {
            getTestFile(resultFolder).mkdirs();

            SimulationEngine engine = DiagramUtility.getPreferredEngine(d);
            engine.setDiagram(d);
            engine.setCompletionTime(4E4);
            engine.setTimeIncrement(2E4);

            for( AgentSimulationEngineWrapper innerEngine : ( (AgentModelSimulationEngine)engine ).getEngines() )
            {
                JVodeSolver solver = (JVodeSolver)innerEngine.getSolver();
                solver.getOptions().setDetectIncorrectNumbers(true);
                //            	solver.getOptions().setAtol(1E-6);
                //            	solver.getOptions().setRtol(1E-6);
            }

            Model model = engine.createModel();

            Map<String, Integer> mapping = engine.getVarPathIndexMapping(); //mapping for new model

            model.init();

            double[] initialValues = model.getCurrentValues();
            iPS = mapping.get(selected[0]);
            iPD = mapping.get(selected[1]);
            iHR = mapping.get(selected[2]);

            Set<String> parameters = new HashSet<>();

            d.getRole(EModel.class).detectVariableTypes();

            for( SubDiagram subDiagram : Util.getSubDiagrams(d) )
            {
                Diagram innerDiagram = subDiagram.getDiagram();
                EModel innerEmodel = innerDiagram.getRole(EModel.class);
                String prefix = subDiagram.getName();
                parameters.addAll(innerEmodel.getVariables().stream().filter(v -> v.getType().equals(Variable.TYPE_PARAMETER))
                        .map( v -> prefix + "/" + v.getName() ).collect( Collectors.toSet() ) );
            }

            calcSensitivity(model, initialValues, engine, parameters, 1, resultPSup, resultPDup, resultHRup);
            calcSensitivity(model, initialValues, engine, parameters, 1, resultPSdown, resultPDdown, resultHRdown);
        }
        catch( Exception ex )
        {
            ex.printStackTrace();
        }
    }

    public void calcSensitivity(Model model, double[] initial, SimulationEngine engine, Set<String> parameters, double change,
            String psResult, String pdresult, String hrResult) throws Exception
    {
        Map<String, Integer> mapping = engine.getVarPathIndexMapping();
        int i = 0;
        for( String paramPath : parameters )
        {
            i++;
            int index = mapping.get(paramPath);
            double initialValue = initial[index];
            double changed = initialValue * change;
            initial[index] = changed;
            model.setCurrentValues(initial);

            System.out.println("Testing " + paramPath);
            double timeStart = System.currentTimeMillis();

            engine.simulate(model, new ResultListener[] {this});

            String err = engine.getSimulator().getProfile().getErrorMessage();
            if( err != null )
            {
                System.out.println("Failed");
                System.out.println(System.currentTimeMillis() - timeStart);
                initial[index] = initialValue;
                continue;
            }

            try (BufferedWriter psWriter = ApplicationUtils.utfAppender(getTestFile(resultPSup));
                    BufferedWriter pdWriter = ApplicationUtils.utfAppender(getTestFile(resultPDup));
                    BufferedWriter hrWriter = ApplicationUtils.utfAppender(getTestFile(resultHRup)))
            {
                psWriter.append("\n" + StreamEx.of(paramPath, beforePS, afterPS).joining("\t"));
                pdWriter.append("\n" + StreamEx.of(paramPath, beforePD, afterPD).joining("\t"));
                hrWriter.append("\n" + StreamEx.of(paramPath, beforeHR, afterHR).joining("\t"));
            }

            System.out.println(i + " / " + parameters.size() + " parameters done.");

            initial[index] = initialValue;
            
        }
    }


    Model model;
    @Override
    public void start(Object model)
    {
        this.model = (Model)model;
    }

    double initialPS;
    double initialPD;
    double initialHR;

    double beforePS;
    double beforePD;
    double beforeHR;

    double afterPS;
    double afterPD;
    double afterHR;

    int iPS;
    int iPD;
    int iHR;

    @Override
    public void add(double t, double[] y) throws Exception
    {
        double[] values = model.getCurrentValues();

        if( t > 0 && t < 3E4 )
        {
            beforePS = values[iPS];
            beforePD = values[iPD];
            beforeHR = values[iHR];
        }
        else if( t > 3E4 )
        {
            afterPS = values[iPS];
            afterPD = values[iPD];
            afterHR = values[iHR];
        }
        else if( t == 0 )
        {
            initialPS = values[iPS];
            initialPD = values[iPD];
            initialHR = values[iHR];
        }
    }
}
