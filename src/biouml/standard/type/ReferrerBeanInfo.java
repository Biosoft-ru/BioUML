package biouml.standard.type;

import one.util.streamex.StreamEx;

import java.util.logging.Level;
import java.util.logging.Logger;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.VectorDataCollection;
import ru.biosoft.access.support.DataCollectionMultyChoiceDialog;
import ru.biosoft.access.support.DataCollectionMultyChoicePropertyEditor;
import biouml.model.Module;
import biouml.standard.diagram.DatabaseReferencesPropertyEditor;
import biouml.standard.type.access.TitleIndex;

public class ReferrerBeanInfo<T extends Referrer> extends GenericEntityBeanInfo<T>
{
    protected static final Logger log = Logger.getLogger(ReferrerBeanInfo.class.getName());

    protected ReferrerBeanInfo(Class<? extends T> r)
    {
        super(r);
    }
    
    protected ReferrerBeanInfo(Class beanClass, String key)
    {
        super(beanClass, key);
    }

    @Override
    public void initProperties() throws Exception
    {
        super.initProperties();

        property("description").htmlDisplayName("DE").add(4);
        property("databaseReferences").editor(DatabaseReferencesPropertyEditor.class).htmlDisplayName("DR").add();
        property("literatureReferences").htmlDisplayName("RF").editor(LiteratureReferencesEditor.class).add();
    }
    ////////////////////////////////////////////////////////////////////////////
    // Literature references editor issues
    //

    public static class LiteratureReferencesEditor extends DataCollectionMultyChoicePropertyEditor
    {
        public static final String REFERENCE = "reference";

        public LiteratureReferencesEditor()
        {
            title = "LiteratureReferencesEditor";
        }

        private DataCollection dc;
        @Override
        public DataCollection getDataCollection() throws Exception
        {
            DataElement de = (DataElement)getBean();
            Module module = Module.optModule(de);
            if( module != null )
            {
                dc = module.getCategory(Publication.class);
                if( dc == null )
                {
                    log.info("Module \"" + module.getName() + "\" doesn't contain collection for literature references.");
                }
                else
                {
                    if( dc.getInfo().getQuerySystem() != null && dc.getInfo().getQuerySystem().getIndex(REFERENCE) != null )
                    {
                        index = (TitleIndex)dc.getInfo().getQuerySystem().getIndex(REFERENCE);
                    }
                }
            }
            return dc;
        }

        @Override
        protected void initEditButtonListener()
        {
            editButton.addActionListener(e -> {
                try
                {
                    DataCollectionMultyChoiceDialog dialog;
                    DataCollection dc = getDataCollection();
                    if( dc == null )
                    {
                        dc = new VectorDataCollection<>("tmp_literatures", Publication.class, null);
                    }
                    dialog = new DataCollectionMultyChoiceDialog(parent, title, dc, REFERENCE, getReferences(), true);
                    if( dialog.doModal() )
                    {
                        setReferences(dialog.getSelectedValues());
                    }
                }
                catch( Throwable t )
                {
                    log.log(Level.SEVERE, t.getMessage(), t);
                }
            });
        }

        private TitleIndex index;
        public String[] getReferences()
        {
            if( index != null )
            {
                return StreamEx.of(getValue()).map(index::get).toArray(String[]::new);
            }
            return ( (String[])getValue() ).clone();
        }

        public void setReferences(String[] references)
        {
            if( index != null )
            {
                setValue(StreamEx.of(references).map(index::getIdByTitle).toArray(String[]::new));
            }
            setValue(references);
        }
    }
}
