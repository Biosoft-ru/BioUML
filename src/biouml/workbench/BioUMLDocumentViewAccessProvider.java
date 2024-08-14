package biouml.workbench;

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.event.ChangeEvent;

import biouml.model.Node;
import ru.biosoft.access.core.CollectionFactory;
import ru.biosoft.access.core.DataCollectionConfigConstants;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.graphics.editor.SelectionManager;
import ru.biosoft.graphics.editor.ViewPane;
import ru.biosoft.graphics.editor.ViewPaneAdapter;
import ru.biosoft.graphics.editor.ViewPaneEvent;
import ru.biosoft.graphics.editor.ViewPaneListener;
import ru.biosoft.gui.Document;
import ru.biosoft.gui.DocumentManager;
import ru.biosoft.gui.DocumentViewAccessProvider;
import ru.biosoft.gui.GUI;

public class BioUMLDocumentViewAccessProvider implements DocumentViewAccessProvider
{
    protected static final Logger log = Logger.getLogger(BioUMLDocumentViewAccessProvider.class.getName());
    
    protected DiagramViewListener diagramViewListener;

    @Override
    public ViewPaneListener getDocumentViewListener()
    {
        if( diagramViewListener == null )
        {
            diagramViewListener = new DiagramViewListener();
        }
        return diagramViewListener;
    }

    @Override
    public void updateSelection(ViewPane viewPane)
    {
        SelectionManager sm = viewPane.getSelectionManager();
        if( sm.getSelectedViewCount() != 1 )
            return;
        final Object model = sm.getSelectedView(0).getModel();
        GUI.getManager().explore( model );
    }

    @Override
    public void enableDocumentActions(boolean b)
    {
        // TODO Auto-generated method stub

    }

    @Override
    public void stateChanged(ChangeEvent e)
    {
        // TODO Auto-generated method stub

    }

    @Override
    public void closeCurrentDocument()
    {
        // TODO Auto-generated method stub

    }

    @Override
    public boolean saveDocumentCurrentApplicationConfirmDialog(Document document, String displayName)
    {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public void saveDocument()
    {
        // TODO Auto-generated method stub

    }

    @Override
    public boolean askSaveConfirmation(Document doc)
    {
        return doc.isChanged();
    }

    // //////////////////////////////////////////////////////////////////////////
    class DiagramViewListener extends ViewPaneAdapter
    {
        public void openElement(ViewPane viewPane)
        {
            SelectionManager sm = viewPane.getSelectionManager();
            if( sm.getSelectedViewCount() != 1 )
                return;
            Object model = sm.getSelectedView(0).getModel();
            if( model instanceof Node )
            {
                Object path = ( (Node)model ).getAttributes().getValue(DataCollectionConfigConstants.COMPLETE_NAME_PROPERTY);
                if( path != null )
                {
                    DataElement de = CollectionFactory.getDataElement(path.toString());
                    if( de != null )
                    {
                        if( DocumentManager.getDocumentManager().openDocument(de) == null )
                        {
                            log.log(Level.SEVERE, "Cannot open: " + path);
                        }
                    }
                }
            }
        }

        @Override
        public void mousePressed(ViewPaneEvent e)
        {
            updateSelection(e.getViewPane());

            if( e.getClickCount() > 1 )
            {
                openElement(e.getViewPane());
            }
        }
    }
}
