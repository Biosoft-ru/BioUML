package biouml.plugins.state;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;

import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.analysis.document.AnalysisDocument;
import ru.biosoft.analysiscore.AnalysisMethodInfo;
import ru.biosoft.analysiscore.AnalysisMethodRegistry;
import ru.biosoft.gui.Document;
import ru.biosoft.gui.GUI;
import biouml.model.util.DiagramXmlConstants;
import biouml.standard.state.State;

import com.developmentontheedge.beans.DynamicProperty;
import com.developmentontheedge.application.action.ApplicationAction;

@SuppressWarnings ( "serial" )
public class ApplyStateAction extends AbstractAction
{
    public static final String KEY = "Apply state";
    
    public ApplyStateAction()
    {
        super(KEY);
    }
    
    @Override
    public void actionPerformed(ActionEvent e)
    {
        DataElementPath path = (DataElementPath)getValue(ApplicationAction.PARAMETER);
      
        ApplyState method = AnalysisMethodRegistry.getAnalysisMethod( ApplyState.class );
        AnalysisMethodInfo methodInfo = AnalysisMethodRegistry.getMethodInfo( method.getName() );
        ApplyStateParameters params = method.getParameters();
        params.setStatePath(path);
        params.setNewDiagram( false );
        try
        {
            State state = (State)path.getDataElement();
            DynamicProperty dp = state.getAttributes().getProperty(DiagramXmlConstants.DIAGRAM_REF_ATTR);
            if (dp != null)
            {
                params.setInputDiagramPath(DataElementPath.create(dp.getValue().toString()));
                params.setOutputDiagramPath(null);
            }
        }
        catch (Exception ex)
        {
            
        }
        
        AnalysisDocument analysisDocument = new AnalysisDocument(methodInfo, params);
        for (Document document : GUI.getManager().getDocuments())
        {
            if(!(document instanceof AnalysisDocument))
                continue;
            AnalysisDocument doc = (AnalysisDocument)document;
            if (doc.getDisplayName().equals(methodInfo.getName()))
            {
                GUI.getManager().replaceDocument( doc, analysisDocument );
                return;
            }
        }
        GUI.getManager().addDocument(analysisDocument);
    }

}
