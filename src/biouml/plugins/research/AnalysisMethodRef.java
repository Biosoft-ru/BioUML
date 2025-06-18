package biouml.plugins.research;

import java.awt.Point;
import biouml.model.Compartment;
import biouml.model.Diagram;
import biouml.model.DiagramElementGroup;
import biouml.model.InitialElementProperties;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.analysiscore.AnalysisMethodRegistry;
import ru.biosoft.graphics.editor.ViewEditorPane;
import ru.biosoft.util.TextUtil2;
import com.developmentontheedge.beans.annot.PropertyName;

public class AnalysisMethodRef implements InitialElementProperties
{
    private String analysisMethod = AnalysisMethodRegistry.getAnalysisNamesWithGroup().findFirst().get();
    private boolean selectManually = false;
    private DataElementPath analysisElement = null;

    public boolean isSelectFromList()
    {
        return !selectManually;
    }
    
    @PropertyName("Analysis method")
    public DataElementPath getAnalysisElement()
    {
        return analysisElement;
    }
    public void setAnalysisElement(DataElementPath analysisElement)
    {
        this.analysisElement = analysisElement;
    }
    
    @PropertyName("Select manually")
    public boolean isSelectManually()
    {
        return selectManually;
    }
    public void setSelectManually(boolean selectManually)
    {
        this.selectManually = selectManually;
    }

    @PropertyName("Analysis method")
    public String getAnalysisMethod()
    {
        return analysisMethod;
    }

    public void setAnalysisMethod(String analysisMethod)
    {
        this.analysisMethod = analysisMethod;
    }

    @Override
    public DiagramElementGroup createElements(Compartment parent, Point location, ViewEditorPane viewPane) throws Exception
    {
        BaseResearchSemanticController semanticController = (BaseResearchSemanticController)Diagram.getDiagram(parent).getType().getSemanticController();
        
        if (selectManually)
            return new DiagramElementGroup(
                    semanticController.addAnalysis( parent, analysisElement.getName(), location, viewPane ) );
        
        return new DiagramElementGroup(
                semanticController.addAnalysis( parent, TextUtil2.split( getAnalysisMethod(), '/' )[1], location, viewPane ) );
    }
}