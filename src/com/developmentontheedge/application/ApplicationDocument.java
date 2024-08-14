package com.developmentontheedge.application;

/**
 * Interface for control documents.
 * Used by classes implements {@link DocumentFactory} and by some actions.
 * @see DocumentFactory
 */
public interface ApplicationDocument
{
    /**
     * Close document.
     * Document should release all resource, save its state and other...
     * After calling this method no one method of ApplicationDocument never called.
     */
    public void close();

    /**
     * Returns display name of the document.
     * @return Display name of the document.
     */
    public String getDisplayName();

    /**
     * Set settings for this document.
     * This function needed because document hasn't access to {@link com.ru.biosoftgui.ApplicationFrame}.
     * @param settings Settings for assigning to document.
     * @see ru.biosoftgui.DocumentFactory
     */
//    public void   setSettings(Settings settings);
}