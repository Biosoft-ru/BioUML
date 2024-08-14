package biouml.plugins.research;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.CheckForNull;

import biouml.model.Module;
import ru.biosoft.access.CollectionFactoryUtils;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.journal.Journal;
import ru.biosoft.journal.JournalList;

/**
 * Journal list extension for research modules
 */
public class ResearchJournalList implements JournalList
{
    /**
     * Prefix for research journals
     */
    public static final String RESEARCH_PREFIX = "Research: ";

    @Override
    public @CheckForNull Journal getJournal(String name)
    {
        if(name == null || !name.startsWith(RESEARCH_PREFIX)) return null;
        name = name.substring(RESEARCH_PREFIX.length());
        try
        {
            Module module = CollectionFactoryUtils.getUserProjectsPath().getChildPath( name ).getDataElement( Module.class );
            return ((ResearchModuleType)module.getType()).getResearchJournal(module);
        }
        catch( Exception e )
        {
            return null;
        }
    }

    @Override
    public List<String> getNameList()
    {
        List<String> names = new ArrayList<>();
        DataCollection<?> modules = CollectionFactoryUtils.getUserProjectsPath().optDataCollection();
        if( modules != null )
        {
            for(DataElement de: modules)
            {
                if( ( de instanceof Module ) && ( ( (Module)de ).getType() instanceof ResearchModuleType ) )
                {
                    String journalName = RESEARCH_PREFIX + de.getName();
                    names.add(journalName);
                }
            }
        }
        return names;
    }
}
