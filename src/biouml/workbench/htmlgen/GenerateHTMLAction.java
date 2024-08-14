package biouml.workbench.htmlgen;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;

import ru.biosoft.access.core.DataElementPath;
import biouml.model.Diagram;
import biouml.model.Module;
import biouml.workbench.BioUMLApplication;

import com.developmentontheedge.application.Application;
import com.developmentontheedge.application.action.ApplicationAction;

public class GenerateHTMLAction extends AbstractAction
{
    public static final String KEY = "Generate HTML";
    public static final String DATABASE = "Database";
    public static final String DIAGRAM = "Diagram";

    public GenerateHTMLAction()
    {
        super( KEY );
    }

    @Override
    public void actionPerformed( ActionEvent e )
    {
        Module module = (Module)getValue(DATABASE);
        Diagram diagram = null;

        DataElementPath diagramCompletePath = (DataElementPath)getValue(ApplicationAction.PARAMETER);
        if (diagramCompletePath != null)
        {
            diagram = diagramCompletePath.optDataElement(Diagram.class);
            if(diagram != null)
            {
                module = Module.optModule(diagram);
            }
        }

        String title = BioUMLApplication.getMessageBundle().getResourceString( "GENERATE_HTML_DIALOG_TITLE" );
        GenerateHTMLDialog generateHTMLDialog = new GenerateHTMLDialog( Application.getApplicationFrame(), title, module, diagram);
        generateHTMLDialog.doModal();

        putValue(DATABASE, null);
        putValue(ApplicationAction.PARAMETER, null);
    }
}
