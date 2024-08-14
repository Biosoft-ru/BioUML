package ru.biosoft.gui;

import java.util.Collection;

import ru.biosoft.access.repository.RepositoryTabs;

public interface UiManager
{
    /**
     * @return RepositoryTabs component
     */
    RepositoryTabs getRepositoryTabs();

    /**
     * Displays information about provided object in PropertiesView/PropertiesEditor/etc. tabs
     * @param object to explore
     */
    void explore(Object object);
    
    /**
     * Bring given viewpart to the top or select it in tab container or open it if it was closed
     * @param viewPart to show
     */
    void showViewPart(ViewPart viewPart);
    
    /**
     * Adds document and opens it. If document was already added and opened, then brings it to top
     * @param document
     */
    void addDocument(Document document);
    
    /**
     * @return collection of all opened documents
     */
    Collection<Document> getDocuments();
    
    /**
     * Remove document if it was opened
     * @param document
     */
    void removeDocument(Document document);
    
    /**
     * @return currently active document (if applicable) or null
     */
    Document getCurrentDocument();
    
    /**
     * Removes oldDocument if it was opened and adds newDocument if it wasn't opened
     * trying to place new document at the same location in UI as the oldDocument
     * @param oldDocument
     * @param newDocument
     */
    void replaceDocument(Document oldDocument, Document newDocument);
    
    /**
     * @return DocumentViewAccessProvider for current application
     */
    DocumentViewAccessProvider getDocumentViewAccessProvider();
}
