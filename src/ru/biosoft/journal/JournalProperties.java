package ru.biosoft.journal;

import java.util.logging.Level;
import java.util.logging.Logger;

import com.developmentontheedge.beans.DynamicProperty;
import com.developmentontheedge.beans.Preferences;
import com.developmentontheedge.application.Application;

/**
 * Application journal properties
 */
public class JournalProperties
{
    protected static final Logger log = Logger.getLogger(JournalProperties.class.getName());
    public static final String CURRENT_JOURNAL_PROPERTY_NAME = "currentJournal";

    /**
     * Get current property name
     */
    public static String getCurrentJournal()
    {
        Preferences preferences = Application.getPreferences();
        Object journalName = preferences.getValue(CURRENT_JOURNAL_PROPERTY_NAME);
        if( journalName == null )
        {
            String[] journalNames = JournalRegistry.getJournalNames();
            if( journalNames.length > 0 )
            {
                journalName = journalNames[0];
                try
                {
                    preferences.add(new DynamicProperty(CURRENT_JOURNAL_PROPERTY_NAME, "Current journal", "Current journal", String.class,
                            journalName));
                }
                catch( Exception e )
                {
                    log.log(Level.SEVERE, "can not create journal property", e);
                }
            }
        }
        if( journalName != null )
        {
            return journalName.toString();
        }
        return null;
    }

    /**
     * Set current property name
     */
    public static void setCurrentJournal(String currentJournalName)
    {
        Preferences preferences = Application.getPreferences();
        DynamicProperty property = preferences.getProperty(CURRENT_JOURNAL_PROPERTY_NAME);
        if( property == null )
        {
            try
            {
                preferences.add(new DynamicProperty(CURRENT_JOURNAL_PROPERTY_NAME, "Current journal", "Current journal", String.class,
                        currentJournalName));
            }
            catch( Exception e )
            {
                log.log(Level.SEVERE, "can not create journal property", e);
            }
        }
        else
        {
            property.setValue(currentJournalName);
        }
    }
}
