package biouml.workbench.graphsearch.actions;

import java.util.List;

import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.biohub.BioHub;
import ru.biosoft.access.biohub.TargetOptions;
import ru.biosoft.access.biohub.TargetOptions.CollectionRecord;
import ru.biosoft.access.subaction.BackgroundDynamicAction;
import biouml.model.Diagram;
import biouml.model.DiagramElement;
import biouml.model.util.AddElementsUtils;
import biouml.standard.diagram.Util;
import biouml.standard.type.Base;
import biouml.workbench.graphsearch.GraphSearchOptions;
import biouml.workbench.graphsearch.QueryEngine;
import biouml.workbench.graphsearch.QueryEngineRegistry;
import biouml.workbench.graphsearch.QueryOptions;
import biouml.workbench.graphsearch.SearchElement;

import com.developmentontheedge.beans.DynamicProperty;
import ru.biosoft.jobcontrol.AbstractJobControl;
import ru.biosoft.jobcontrol.JobControl;
import ru.biosoft.jobcontrol.JobControlException;

/**
 * @author lan
 *
 */
public abstract class AddFromSearchAction extends BackgroundDynamicAction
{
    private final String direction;
    
    protected AddFromSearchAction(String direction)
    {
        this.direction = direction;
    }

    @Override
    public JobControl getJobControl(final Object model, final List<DataElement> selectedItems, Object properties) throws Exception
    {
        return new AbstractJobControl(log)
        {
            @Override
            protected void doRun() throws JobControlException
            {
                try
                {
                    QueryEngine queryEngine = null;
                    QueryOptions queryOptions = new QueryOptions(1, direction.equals("down")?BioHub.DIRECTION_DOWN:BioHub.DIRECTION_UP);
                    TargetOptions targetOptions = null;
                    for(int i=0; i<selectedItems.size(); i++)
                    {
                        setPreparedness(i*100/selectedItems.size());
                        DataElement dataElement = selectedItems.get(i);
                        if(dataElement instanceof DiagramElement)
                        {
                                Base kernel = ((DiagramElement)dataElement).getKernel();
                                DynamicProperty dp = kernel.getAttributes().getProperty(Util.ORIGINAL_PATH);
                                if (dp != null)
                                {
                                    String originalPath = (String)dp.getValue();
                                    DataElementPath dep = DataElementPath.create(originalPath);
                                    DataElement de = dep.getDataElement();
                                    if (de instanceof Base)
                                        kernel = (Base)de;
                                }
                                
                                if(queryEngine == null)
                                {
                                    String[] pathComponents = DataElementPath.create(kernel).getPathComponents();
                                    if(pathComponents.length >= 2)
                                    {
                                        String defaultPath = pathComponents[0]+"/"+pathComponents[1];
                                        if(pathComponents[1].toLowerCase().startsWith("transpath"))
                                            queryOptions.setDepth(3);
                                        targetOptions = new TargetOptions(new CollectionRecord(defaultPath, true));
                                        queryEngine = QueryEngineRegistry.lookForQueryEngine(targetOptions, GraphSearchOptions.TYPE_NEIGHBOURS);
                                    }
                                }
                                if(queryEngine != null)
                                {
                                    SearchElement[] result = queryEngine.searchLinked(new SearchElement[] {new SearchElement(kernel)},
                                            queryOptions, targetOptions, this);
                                    if(result != null && result.length != 0)
                                    {
                                        AddElementsUtils.addElements((Diagram)model, result, null);
                                    }
                                }
                        }
                    }
                    setPreparedness(100);
                    resultsAreReady(new Object[]{model});
                }
                catch( Exception e )
                {
                    throw new JobControlException(e);
                }
            }
        };
    }

    @Override
    public boolean isApplicable(Object model)  //TODO: find more clever way to disable action for certain diagrams
    {
        return model instanceof Diagram && !((Diagram)model).getType().getClass().toString().endsWith("AnnotationDiagramType");
    }
}
