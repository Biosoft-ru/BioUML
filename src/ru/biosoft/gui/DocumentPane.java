package ru.biosoft.gui;

import java.awt.BorderLayout;
import java.util.HashMap;

import javax.swing.ImageIcon;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import ru.biosoft.access.core.ClassIcon;
import ru.biosoft.access.repository.IconFactory;


public class DocumentPane extends JPanel implements ChangeListener
{
    private final JTabbedPane tabbedPane = new JTabbedPane(JTabbedPane.TOP);
    private final HashMap<String, Document> tabs = new HashMap<>();

    public DocumentPane()
    {
        setLayout(new BorderLayout());
        add(tabbedPane, BorderLayout.CENTER);
        tabbedPane.addChangeListener(this);
    }

    public void addChangeListener(ChangeListener l)
    {
        tabbedPane.addChangeListener(l);
    }

    public void removeChangeListener(ChangeListener l)
    {
        tabbedPane.removeChangeListener(l);
    }

    protected String getSelectedTabName()
    {
        int idx = tabbedPane.getSelectedIndex();
        return idx < 0 ? null : tabbedPane.getTitleAt(idx);
    }

    public int getDocumentCount()
    {
        return tabbedPane.getTabCount();
    }


    public void addDocument(Document document)
    {
        insertDocument(document, tabbedPane.getTabCount());
    }


    public void insertDocument(Document document, int index)
    {
        if (tabs.keySet().contains(document.getDisplayName()))
        {
            tabbedPane.setSelectedComponent(document.getViewPane());
        }
        else
        {
            String iconId = IconFactory.getClassIconId( document.getClass() );
            ImageIcon icon = IconFactory.getIconById( iconId );
//            ClassIcon icon = document.getClass().getAnnotation( ClassIcon.class );
//            icon.value();
            tabbedPane.insertTab(document.getDisplayName(), icon, document.getViewPane(), null, index);
            tabbedPane.setSelectedComponent(document.getViewPane());
            tabs.put(document.getDisplayName(), document);
            update(document);
        }
    }

    public Document getCurrentDocument()
    {
        return tabs.get(getSelectedTabName());
    }

    public Document getDocument(int n)
    {
        return tabs.get(tabbedPane.getTitleAt(n));
    }

    public void setActiveDocument(Document doc)
    {
        addDocument(doc);
    }

    public void removeCurrentDocument()
    {
        Document document = getCurrentDocument();
        document.close();
        tabs.remove(getSelectedTabName());
        tabbedPane.removeTabAt(tabbedPane.getSelectedIndex());

        document = getCurrentDocument();
        update(document);
    }

    public void removeDocument(int n)
    {
        tabbedPane.setSelectedIndex(n);
        removeCurrentDocument();
    }

    public void replaceCurrentDocument(Document document)
    {
        int currentIndex = tabbedPane.getSelectedIndex();
        removeDocument(currentIndex);
        insertDocument(document, currentIndex);
    }

    /**
     * Invoked when the target of the listener has changed its state.
     * @param e  a ChangeEvent object
     */
    @Override
    public void stateChanged(ChangeEvent e)
    {
        update(tabs.get(getSelectedTabName()));
    }

    private void update(Document document)
    {
        DocumentManager.setActiveDocument(document, this);
    }
}

