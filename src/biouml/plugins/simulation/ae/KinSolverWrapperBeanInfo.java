package biouml.plugins.simulation.ae;

import ru.biosoft.util.bean.BeanInfoEx2;
import ru.biosoft.workbench.editors.GenericComboBoxEditor;

public class KinSolverWrapperBeanInfo extends BeanInfoEx2<KinSolverWrapper>
{
    public KinSolverWrapperBeanInfo()
    {
        super(KinSolverWrapper.class);
    }

    @Override
    protected void initProperties() throws Exception
    {
        add("ftolerance");
        add("tolerance");
        add("maxIter");
        add("maxSetups");
        add("strategy", StrategyEditor.class);
        add("etaFlag", EtaFlagsEditor.class);
    }

    public static class StrategyEditor extends GenericComboBoxEditor
    {
        @Override
        public Object[] getAvailableValues()
        {
            return ( (KinSolverWrapper)this.bean ).getAvailableStrategies();
        }
    }

    public static class EtaFlagsEditor extends GenericComboBoxEditor
    {
        @Override
        public Object[] getAvailableValues()
        {
            return ( (KinSolverWrapper)this.bean ).getAvailableEtaFlags();
        }
    }
}
