package biouml.plugins.research.action;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;

import com.developmentontheedge.application.Application;

@SuppressWarnings ( "serial" )
public class ImportResearchAction extends AbstractAction
{
    public static final String KEY = "Load Research";
    
    public ImportResearchAction()
    {
        super(KEY);
    }
    
    @Override
    public void actionPerformed(ActionEvent arg0)
    {
        ImportResearchDialog dialog = new ImportResearchDialog(Application.getApplicationFrame());
        dialog.pack();
        dialog.setVisible(true);
    }

}
