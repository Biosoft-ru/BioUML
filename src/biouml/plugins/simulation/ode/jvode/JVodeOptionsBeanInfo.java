package biouml.plugins.simulation.ode.jvode;

import com.developmentontheedge.beans.BeanInfoEx;
import com.developmentontheedge.beans.editors.TagEditorSupport;

import biouml.plugins.simulation.OdeSimulatorOptions;

public class JVodeOptionsBeanInfo extends BeanInfoEx
{
    public JVodeOptionsBeanInfo()
    {
        super( JVodeOptions.class, true );
    }

    @Override
    public void initProperties() throws Exception
    {
        add("atol");
        add("rtol");
        addWithTags("statisticsMode", OdeSimulatorOptions.STATISTICS_MODS);
        add("method", MethodEditor.class);
        add("iterations", IterationsEditor.class);
        addHidden("jacobianApproximation", JacobianApproximationEditor.class, "isFunctional");
        add("stepsLimit");
        add("hMin");
        add("hMaxInv");
        addHidden("mu", "isNotBandJacobian");
        addHidden("ml", "isNotBandJacobian");
        add("detectIncorrectNumbers");
    }

    public static class MethodEditor extends TagEditorSupport
    {
        public MethodEditor()
        {
            super(new String[] {"Adams-Moulton", "Backward Differential"}, 0);
        }
    }

    public static class IterationsEditor extends TagEditorSupport
    {
        public IterationsEditor()
        {
            super(new String[] {"Newton", "Functional"}, 0);
        }
    }

    public static class JacobianApproximationEditor extends TagEditorSupport
    {
        public JacobianApproximationEditor()
        {
            super(new String[] {"Dense jacobian", "Band jacobian", "Diagonal jacobian"}, 0);
        }
    }
}
