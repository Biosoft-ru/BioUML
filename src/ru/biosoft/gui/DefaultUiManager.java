package ru.biosoft.gui;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import ru.biosoft.access.repository.RepositoryTabs;
import com.developmentontheedge.application.Application;
import com.developmentontheedge.application.ApplicationFrame;

public class DefaultUiManager implements UiManager
{
    private final DocumentViewAccessProvider accessProvider = new DefaultDocumentViewAccessProvider();

    @Override
    public RepositoryTabs getRepositoryTabs()
    {
        return (RepositoryTabs)Application.getApplicationFrame().getPanelManager().getPanel( ApplicationFrame.REPOSITORY_PANE_NAME );
    }

    @Override
    public void explore(Object object)
    {
        ( (ExplorerPane)Application.getApplicationFrame().getPanelManager().getPanel( ApplicationFrame.EXPLORER_PANE_NAME ) ).explore(
                object, null );
    }

    @Override
    public void showViewPart(ViewPart viewPart)
    {
        if(viewPart == null)
            return;
        EditorsTabbedPane editorsPane = (EditorsTabbedPane)Application.getApplicationFrame().getPanelManager()
                .getPanel(ApplicationFrame.EDITOR_PANE_NAME);
        editorsPane.selectTab( viewPart );
    }

    @Override
    public void addDocument(Document document)
    {
        DocumentsPane.getDocumentsPane().addDocument( document );
    }

    @Override
    public Collection<Document> getDocuments()
    {
        DocumentsPane pane = DocumentsPane.getDocumentsPane();
        int count = pane.getDocumentCount();
        List<Document> documents = new ArrayList<>();
        for(int i=0; i<count; i++)
        {
            Document doc = pane.getDocument(i);
            if(doc != null)
                documents.add(doc);
        }
        return documents;
    }

    @Override
    public void removeDocument(Document document)
    {
        DocumentsPane.getDocumentsPane().removeDocument( document );
    }

    @Override
    public Document getCurrentDocument()
    {
        return DocumentsPane.getDocumentsPane().getCurrentDocument();
    }

    @Override
    public void replaceDocument(Document oldDocument, Document newDocument)
    {
        DocumentsPane documentsPane = DocumentsPane.getDocumentsPane();
        int count = documentsPane.getDocumentCount();
        for(int i=0; i<count; i++)
        {
            if(documentsPane.getDocument( i ) == oldDocument)
            {
                documentsPane.removeDocument( oldDocument );
                documentsPane.insertDocument( newDocument, i );
                return;
            }
        }
        documentsPane.removeDocument( oldDocument );
        documentsPane.addDocument( newDocument );
    }

    @Override
    public DocumentViewAccessProvider getDocumentViewAccessProvider()
    {
        ApplicationFrame frame = Application.getApplicationFrame();
        if(frame instanceof DocumentViewAccessProvider)
            return (DocumentViewAccessProvider)frame;
        return accessProvider;
    }
}
