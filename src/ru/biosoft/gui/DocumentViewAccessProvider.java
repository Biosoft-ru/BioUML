package ru.biosoft.gui;

import javax.swing.event.ChangeEvent;
import ru.biosoft.graphics.editor.ViewPane;
import ru.biosoft.graphics.editor.ViewPaneListener;
import ru.biosoft.gui.Document;

public interface DocumentViewAccessProvider
{
    public ViewPaneListener getDocumentViewListener();
    public void updateSelection(ViewPane viewPane);
    public void enableDocumentActions(boolean b);
    public void stateChanged(ChangeEvent e);
    public void closeCurrentDocument();
    public boolean saveDocumentCurrentApplicationConfirmDialog(Document document, String displayName);
    public void saveDocument();
    public boolean askSaveConfirmation(Document doc);
}
