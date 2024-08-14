package biouml.plugins.lucene;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.JFrame;

import biouml.model.Module;

import com.developmentontheedge.application.Application;

/**
 *
 */
public class RebuildIndexAction extends AbstractAction
{
    
    public static final String KEY = "Rebuild index";
    public static final String LUCENE_FACADE = "luceneFacade";
    public static final String DATA_COLLECTION = "dataColection";
      
    MessageBundle messageBundle = new MessageBundle();
    
    protected JFrame frame = null;
    
    public RebuildIndexAction()
    {
        super(KEY);
        frame = Application.getApplicationFrame();
    }
    
    @Override
    public void actionPerformed(ActionEvent e)
    {
        try
        {
            RebuildIndexDialog rebuildDialog = new RebuildIndexDialog(frame, messageBundle.getResourceString("LUCENE_REBUILD_INDEX_TITLE"), (String[])null, (LuceneQuerySystem)getValue(LUCENE_FACADE), (Module)getValue(DATA_COLLECTION));
            rebuildDialog.doModal();
        }
        catch (Exception e1)
        {
            e1.printStackTrace();
        }
        putValue(LUCENE_FACADE, null);
        putValue(DATA_COLLECTION, null);
    }
    
}
