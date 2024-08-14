package biouml.plugins.simulation.ode._test;

import ru.biosoft.bsa._test.ViewTestCase;
import biouml.plugins.simulation.ode.ImexSD;
import biouml.plugins.simulation.ode.VanDerPol;
import biouml.plugins.simulation.*;

import javax.swing.*;
import java.util.HashMap;

/**
 *
 * @author sanya
 * @since Jan 29, 2005
 * $Id: VanderpolTest.jav,v 1.3 2005/02/09 12:59:07 puz Exp $
 */
public class VanderpolTest extends ViewTestCase
{
    public VanderpolTest(String name)
    {

        super(name);
    }

    public void test() throws Exception
    {
        ImexSD simulator = new ImexSD();
        ImexSD.Options opt = new ImexSD.Options (1.0E-6, ImexSD.Options.STATISTICS_OFF);

        double[] atol = new double[1];
        atol[0] = 1.0E-6;

        double[] rtol = new double[1];
        rtol[0] = 1.0E-6;

        ResultListener[] listeners = { new ResultWriter(null) };
        Model model = new VanDerPol();
        Span span = new Span(0.0, 11.0);

		simulator.setOptions(opt);

        SimulationControl simulationControl = new SimulationControl();

        simulationControl.addResultListener(listeners[0]);

        simulationControl.setModel(model);
        simulationControl.setSimulator(simulator);
        simulationControl.setSpan(span);
        simulationControl.setAtol(atol);
        simulationControl.setRtol(rtol);
        simulationControl.setDiagramName("test model");

        HashMap legends = new HashMap();
        legends.put("x0", new Boolean(true));
        legends.put("x1", new Boolean(true));
        simulationControl.setLegends(legends);

        SimulationPane pane = new SimulationPane(simulationControl);
        JFrame frame = new JFrame();
        frame.getContentPane().add(pane);
        frame.setSize(frame.getPreferredSize());
        frame.validate();
        frame.show();
        frame.setEnabled(true);
        frame.setVisible(true);
    }

}
