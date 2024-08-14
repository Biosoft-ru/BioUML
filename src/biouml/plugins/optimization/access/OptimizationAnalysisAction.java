package biouml.plugins.optimization.access;

import biouml.plugins.optimization.OptimizationMethodRegistry;
import biouml.plugins.optimization.OptimizationUtils;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.repository.AbstractElementAction;
import ru.biosoft.analysis.optimization.OptimizationMethod;
import ru.biosoft.analysiscore.AnalysisMethodInfo;

public class OptimizationAnalysisAction extends AbstractElementAction
{
    @Override
    protected void performAction(DataElement de) throws Exception
    {
        OptimizationMethod method = OptimizationMethodRegistry.getOptimizationMethod(de.getName());
        method = method.clone(method.getOrigin(), de.getName());
        DataCollection<?> dc = OptimizationUtils.DOCUMENTS.getDataCollection();
        NewOptimizationDialog dialog = new NewOptimizationDialog(dc, method);
        dialog.doModal();
    }

    @Override
    protected boolean isApplicable(DataElement de)
    {
        return de instanceof AnalysisMethodInfo && OptimizationMethod.class.isAssignableFrom(((AnalysisMethodInfo)de).getAnalysisClass());
    }
}
