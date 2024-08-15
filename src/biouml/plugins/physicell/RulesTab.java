package biouml.plugins.physicell;

import java.util.Arrays;
import java.util.List;

import javax.swing.JTable;

import com.developmentontheedge.beans.swing.PropertyInspector;
import com.developmentontheedge.beans.swing.table.AbstractRowModel;
import com.developmentontheedge.beans.swing.table.RowModel;
import ru.biosoft.gui.TabularPropertiesEditor;

public class RulesTab extends TabularPropertiesEditor
{
    private RulesProperties rules = new RulesProperties();
    private Object template;

    public void explore(RulesProperties rules)
    {
        this.rules = rules;
        update();
    }

    public void addRule()
    {
        rules.addRule();
        update();
    }

    public void removeSelectedRule()
    {
        int index = getTable().getSelectedRow();
        if( index < 0 )
            return;
        rules.removeRule( index );
        update();
    }

    public void update()
    {
        explore( getRowModel(), getTemplate(), PropertyInspector.SHOW_USUAL );
        getTable().setAutoResizeMode( JTable.AUTO_RESIZE_ALL_COLUMNS );
    }

    protected RowModel getRowModel()
    {
        return new ListRowModel( Arrays.asList( rules.getRules() ), RuleProperties.class );
    }

    protected Object createTemplate()
    {
        return new RuleProperties();
    }

    protected Object getTemplate()
    {
        if( template == null )
            template = createTemplate();
        return template;
    }

    static class ListRowModel extends AbstractRowModel
    {
        private List<?> roles;
        private Class<?> c;

        public ListRowModel(List<?> roles, Class<?> c)
        {
            this.roles = roles;
            this.c = c;
        }

        @Override
        public int size()
        {
            return roles.size();
        }

        @Override
        public Object getBean(int index)
        {
            return roles.get( index );
        }

        @Override
        public Class<?> getBeanClass()
        {
            return c;
        }
    };
}