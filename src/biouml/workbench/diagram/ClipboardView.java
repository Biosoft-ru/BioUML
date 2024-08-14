package biouml.workbench.diagram;

import javax.swing.Action;

import ru.biosoft.graphics.editor.ViewPane;
import ru.biosoft.gui.Document;
import ru.biosoft.gui.ViewPartSupport;
import biouml.model.Diagram;

/**
 * Spike solution for clipboard.
 * 
 * @pending - explore issues - undo/redo, document, store updated kernel into
 *          the database. Possibly clipboard also can be considered as document
 *          like repositoryDocument. In any case we need to add
 */
@SuppressWarnings ( "serial" )
public class ClipboardView extends ViewPartSupport
{
    static ClipboardPane clipboard;

    public ClipboardView()
    {
        clipboard = ClipboardPane.getClipboard();
        add(clipboard);
    }

    @Override
    public boolean canExplore(Object model)
    {
        return model instanceof Diagram;
    }

    @Override
    public void explore(Object model, Document document)
    {
        if( this.document != null )
        {
            this.document.getViewPane().removeViewPaneListener(clipboard);
        }

        super.explore(model, document);

        Diagram diagram = null;
        if( document instanceof DiagramDocument )
        {
            diagram = ( (DiagramDocument)document ).getDiagram();
        }
        ViewPane viewPane = null;
        if( document != null )
        {
            viewPane = document.getViewPane();
        }
        clipboard.explore(diagram, viewPane);

        clipboard.repaint();
        if( document != null )
        {
            document.getViewPane().addViewPaneListener(clipboard);
        }
    }
    ////////////////////////////////////////////////////////////////////////////
    // Actions
    //

    @Override
    public Action[] getActions()
    {
        if( clipboard != null )
            return clipboard.getActions();
        return new Action[0];
    }

}
