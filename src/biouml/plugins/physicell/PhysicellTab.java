package biouml.plugins.physicell;

import java.util.List;

import javax.swing.JTable;

import com.developmentontheedge.beans.swing.PropertyInspector;
import com.developmentontheedge.beans.swing.table.AbstractRowModel;
import com.developmentontheedge.beans.swing.table.RowModel;

import ru.biosoft.gui.TabularPropertiesEditor;

public abstract class PhysicellTab extends TabularPropertiesEditor
{
    protected MulticellEModel emodel;
    private Object template;

    public PhysicellTab(MulticellEModel emodel)
    {
        this.emodel = emodel;
        update();
    }

    public void update()
    {
        explore( getRowModel(), getTemplate(), PropertyInspector.SHOW_USUAL );
        getTable().setAutoResizeMode( JTable.AUTO_RESIZE_ALL_COLUMNS );
    }

    protected abstract RowModel getRowModel();
    protected abstract Object createTemplate();

    protected Object getTemplate()
    {
        if( template == null )
            template = createTemplate();
        return template;
    }

    static class ListRowModel extends AbstractRowModel
    {
        private List<?> roles;
        private Class c;

        public ListRowModel(List<?> roles, Class c)
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