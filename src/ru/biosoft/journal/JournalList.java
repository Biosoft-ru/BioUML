package ru.biosoft.journal;

import java.util.List;

/**
 * JournalCollection.
 * Is used for 'journalList' extension
 */
public interface JournalList
{
    /**
     * Get names of available journals
     */
    public List<String> getNameList();
    
    /**
     * Get journal by name
     */
    public Journal getJournal(String name);
}
