package ru.biosoft.gui;

import javax.swing.event.ChangeEvent;

import ru.biosoft.graphics.editor.SelectionManager;
import ru.biosoft.graphics.editor.ViewPane;
import ru.biosoft.graphics.editor.ViewPaneAdapter;
import ru.biosoft.graphics.editor.ViewPaneListener;

/**
 * Default implementation of DocumentViewAccessProvider.
 * 
 * @pending refactoring the concept of DocumentViewAccessProvider.
 */
public class DefaultDocumentViewAccessProvider implements DocumentViewAccessProvider
{
    protected ViewPaneAdapter stubListener = new ViewPaneAdapter();

    @Override
    public ViewPaneListener getDocumentViewListener()
    {
        return stubListener;
    }

    @Override
    public void updateSelection(ViewPane viewPane)
    {
        SelectionManager sm = viewPane.getSelectionManager();
        if (sm.getSelectedViewCount() != 1)
            return;
        final Object model = sm.getSelectedView(0).getModel();
        GUI.getManager().explore(model);
    }

    @Override
    public void enableDocumentActions(boolean b)    {}

    @Override
    public void stateChanged(ChangeEvent e)         {}

    @Override
    public void closeCurrentDocument()
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean saveDocumentCurrentApplicationConfirmDialog(
            Document document, String displayName)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public void saveDocument()
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean askSaveConfirmation(Document doc)
    {
        return doc.isChanged();
    }
}
