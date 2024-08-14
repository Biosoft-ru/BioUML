
package biouml.plugins.research.workflow.items;

import java.awt.Dimension;

import org.mozilla.javascript.EvaluatorException;

import ru.biosoft.plugins.javascript.document.Dim;
import ru.biosoft.plugins.javascript.document.JSDocumentContextFactory;
import ru.biosoft.plugins.javascript.document.JSPanel;

import com.developmentontheedge.application.Application;
import com.developmentontheedge.application.dialog.OkCancelDialog;

public class TextScriptEditorDialog extends OkCancelDialog
{
    protected JSPanel jsPanel = null;
    
    public TextScriptEditorDialog()
    {
        this("");
        
    }
    
    public TextScriptEditorDialog(String script)
    {
        super(Application.getApplicationFrame(), "JavaScript editor", true);
        Dim dim = new Dim();
        dim.attachTo(new JSDocumentContextFactory());
        try
        {
            dim.compileScript("JScript editor script", script);
        }
        catch(EvaluatorException ignore)
        {
            
        }
        jsPanel = new JSPanel(dim.sourceInfo("JScript editor script"), dim, script);
        jsPanel.setSize(new Dimension(600, 300));
        jsPanel.setPreferredSize(new Dimension(600, 300));
        
        setContent(jsPanel);
        pack();
    }
    
    public String getScript()
    {
        return jsPanel.getText(false);
    }
    
}
