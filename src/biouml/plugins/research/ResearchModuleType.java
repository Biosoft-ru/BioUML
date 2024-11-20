package biouml.plugins.research;

import java.util.logging.Level;
import java.util.Arrays;
import java.util.List;

import java.util.logging.Logger;
import java.util.stream.Collectors;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.DataElementSupport;
import ru.biosoft.access.core.DerivedDataCollection;
import ru.biosoft.access.security.SecurityManager;
import ru.biosoft.access.DataCollectionUtils;
import ru.biosoft.access.Repository;
import ru.biosoft.journal.Journal;
import biouml.model.DiagramType;
import biouml.model.Module;
import biouml.model.ModuleType;
import biouml.model.xml.XmlDiagramType;
import biouml.standard.StandardModuleType;

import com.developmentontheedge.application.Application;

/**
 * Module type for research
 */
public class ResearchModuleType extends DataElementSupport implements ModuleType
{
    protected static final Logger log = Logger.getLogger(ResearchModuleType.class.getName());

    public static final String VERSION = "0.8.7";
    public static final String JOURNAL_COLLECTION = "Journal";
    
    public ResearchModuleType()
    {
        this(Application.getGlobalValue("ApplicationName")+" research");
    }

    protected ResearchModuleType(String name)
    {
        super(name, null);
    }

    protected Journal researchJournal = null;
    /**
     * Get journal for research
     */
    public Journal getResearchJournal(Module module)
    {
        if( researchJournal == null )
        {
            try
            {
                DataCollection<?> journalCollection = (DataCollection<?>)module.get(JOURNAL_COLLECTION);
                journalCollection.setNotificationEnabled(false);
                SecurityManager.runPrivileged(() -> {
                    DataCollection<?> primaryJournalCollection = DataCollectionUtils.fetchPrimaryCollectionPrivileged(journalCollection);
                    primaryJournalCollection.setNotificationEnabled(false);
                    if ( primaryJournalCollection instanceof DerivedDataCollection )
                    {
                        DataCollection<?> primaryJournalCollection2 = ((DerivedDataCollection) primaryJournalCollection).getPrimaryCollection();
                        primaryJournalCollection2.setNotificationEnabled(false);
                    }
                    return null;
                });
                researchJournal = new DataCollectionJournal(journalCollection);
            }
            catch( Exception e )
            {
                log.log(Level.SEVERE, "Can not load journal", e);
            }
        }
        return researchJournal;
    }

    @Override
    public boolean canCreateEmptyModule()
    {
        return true;
    }

    @Override
    public String getCategory(Class<? extends DataElement> aClass)
    {
        return null;
    }

    @Override
    public Class<? extends DiagramType>[] getDiagramTypes()
    {
        return StandardModuleType.getGeneralPurposeTypes().toArray( Class[]::new );
    }

    @Override
    public String getVersion()
    {
        return VERSION;
    }

    @Override
    public String[] getXmlDiagramTypes()
    {
        DataCollection<?> xmlDiagrams = XmlDiagramType.getTypesCollection();
        List<String> names = xmlDiagrams.names().collect( Collectors.toList() );
        names.retainAll(Arrays.asList("endonet.xml", "lipidomic.xml", "kegg_recon.xml"));
        return names.toArray(new String[names.size()]);
    }

    @Override
    public boolean isCategorySupported()
    {
        return false;
    }

    @Override
    public Module createModule(Repository parent, String name) throws Exception
    {
        // TODO Auto-generated method stub
        return null;
    }
}
