package biouml.plugins.research.web;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.VectorDataCollection;
import ru.biosoft.journal.Journal;
import ru.biosoft.server.servlets.webservices.BiosoftWebRequest;
import ru.biosoft.table.access.TableResolver;
import ru.biosoft.tasks.TaskInfo;
import biouml.model.Diagram;
import biouml.model.Module;
import biouml.plugins.research.JournalInfoWrapper;
import biouml.plugins.research.ResearchModuleType;

public class JournalTableResolver extends TableResolver
{
    public JournalTableResolver(BiosoftWebRequest arguments)
    {
    }
    
    @Override
    public int accept(DataElement de) throws Exception
    {
        if( de instanceof DataCollection )
        {
            Class<?> dataElementType = ( (DataCollection<?>)de ).getDataElementType();
            if( dataElementType == TaskInfo.class )
                return 1;
        }
        return 0;
    }
    
    @Override
    public DataCollection<?> getTable(DataElement de) throws Exception
    {
        if( de instanceof Diagram
                || ( de instanceof DataCollection && ( (DataCollection<?>)de ).getDataElementType() == TaskInfo.class ) )
        {
            VectorDataCollection<JournalInfoWrapper> actions = new VectorDataCollection<>( "Actions",
                    JournalInfoWrapper.class, null);

            Module module = Module.optModule(de);
            if( module != null && module.getType() instanceof ResearchModuleType )
            {
                Journal journal = ( (ResearchModuleType)module.getType() ).getResearchJournal(module);
                for(TaskInfo task: journal )
                {
                    actions.put(new JournalInfoWrapper(task));
                }
            }
            return actions;
        }
        return null;
    }

    @Override
    public String getRowId(DataElement de, String rowName)
    {
        if( de instanceof Diagram )
        {
            Module module = Module.optModule(de);
            if( ( module != null ) && ( module.getType() instanceof ResearchModuleType ) )
            {
                Journal journal = ( (ResearchModuleType)module.getType() ).getResearchJournal(module);
                return journal.getJournalPath().getChildPath(rowName).toString();
            }
        }
        return null;
    }
}