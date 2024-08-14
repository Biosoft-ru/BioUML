package com.developmentontheedge.application;

public class DocumentManager
{
    public DocumentManager(DocumentFactory documentFactory)
    {
        this.documentFactory = documentFactory;
    }

    // //////////////////////////////////////////////////////////////////////////
    private DocumentFactory documentFactory;

    public DocumentFactory getDocumentFactory()
    {
        return documentFactory;
    }

    protected ApplicationDocument activeDocument;
    public ApplicationDocument getActiveDocument()
    {
        return activeDocument;
    }
    public void setActiveDocument(ApplicationDocument document)
    {
        activeDocument = document;
    }

    protected ApplicationDocument activeRepositoryDocument;
    public ApplicationDocument getActiveRepositoryDocument()
    {
        return activeRepositoryDocument;
    }
    public void setActiveRepositoryDocument(ApplicationDocument repositoryDocument)
    {
        activeRepositoryDocument = repositoryDocument;
    }
}
