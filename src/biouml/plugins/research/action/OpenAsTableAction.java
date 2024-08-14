package biouml.plugins.research.action;

import java.util.Iterator;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.VectorDataCollection;
import ru.biosoft.access.repository.AbstractElementAction;
import ru.biosoft.gui.GUI;
import ru.biosoft.journal.Journal;
import ru.biosoft.table.document.TableDocument;
import ru.biosoft.tasks.TaskInfo;
import biouml.model.Module;
import biouml.plugins.research.JournalInfoWrapper;
import biouml.plugins.research.ResearchModuleType;

public class OpenAsTableAction extends AbstractElementAction
{
    @Override
    protected void performAction(DataElement de) throws Exception
    {
        Module module = Module.optModule(de);

        VectorDataCollection<JournalInfoWrapper> tasks = new VectorDataCollection<>( "Actions", JournalInfoWrapper.class, null );
        if( ( module != null ) && ( module.getType() instanceof ResearchModuleType ) )
        {
            Journal journal = ( (ResearchModuleType)module.getType() ).getResearchJournal(module);
            Iterator<TaskInfo> iter = journal.iterator();
            while( iter.hasNext() )
            {
                JournalInfoWrapper ji = new JournalInfoWrapper(iter.next());
                tasks.put(ji);
            }
            GUI.getManager().addDocument(new TableDocument(tasks));
        }
    }

    @Override
    protected boolean isApplicable(DataElement de)
    {
        return (de instanceof DataCollection) && TaskInfo.class.isAssignableFrom(((DataCollection<?>)de).getDataElementType());
    }
}
