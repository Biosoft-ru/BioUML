package biouml.plugins.expression;

import biouml.model.Diagram;
import biouml.model.DiagramFilter;
import ru.biosoft.access.CollectionFactoryUtils;
import ru.biosoft.access.core.ClassIcon;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.analysiscore.AnalysisMethodSupport;
import ru.biosoft.util.BeanUtil;

/**
 * Add expression as analysis
 * @author lan
 */
@ClassIcon("resources/expression-filter.gif")
public class ExpressionFilterAnalysis extends AnalysisMethodSupport<ExpressionFilterAnalysisParameters>
{
    public ExpressionFilterAnalysis(DataCollection<?> origin, String name)
    {
        super(origin, name, new ExpressionFilterAnalysisParameters());
    }

    /**
     * Actually perform analysis
     * @throws Exception
     */
    @Override
    public Diagram justAnalyzeAndPut() throws Exception
    {
        Diagram input = parameters.getInputDiagram().getDataElement(Diagram.class);
        Diagram output = input.clone(parameters.getOutputDiagram().optParentCollection(), parameters.getOutputDiagram().getName());
        ExpressionFilterProperties filterProperties = parameters.getFilterProperties();
        String filterName = "mapping";
        if(filterProperties.getTable() != null && !filterProperties.getTable().getName().equals(""))
            filterName = filterProperties.getTable().getName();
        ExpressionFilter filter = new ExpressionFilter(filterName);
        filter.setEnabled(true);
        BeanUtil.copyBean(filterProperties, filter.getProperties());
        output.setDiagramFilter(filter);
        output.setFilterList(new DiagramFilter[] {filter});
        CollectionFactoryUtils.save(output);
        return output;
    }
}
