package ru.biosoft.plugins.graph;

import java.util.ArrayList;
import java.util.List;

import com.developmentontheedge.beans.BeanInfoEx;
import com.developmentontheedge.beans.editors.StringTagEditorSupport;

public class LayouterOptionsBeanInfo extends BeanInfoEx
{
    public LayouterOptionsBeanInfo()
    {
        super( LayouterOptions.class, true );
    }

    @Override
    public void initProperties() throws Exception
    {
        add("layouter", LayouterEditor.class);
    }

    public static class LayouterEditor extends StringTagEditorSupport
    {
        private static List<LayouterDescriptor> layoutDescr;
        static
        {
            layoutDescr = ru.biosoft.plugins.graph.GraphPlugin.loadLayouters();
        }

        @Override
        public String[] getTags()
        {
            ArrayList<String> typesList = new ArrayList<>();
            for( LayouterDescriptor l : layoutDescr )
            {
                if( l.isPublic() )
                {
                    typesList.add(l.getTitle());
                }
            }
            return typesList.toArray(new String[typesList.size()]);
        }

        @Override
        public String getAsText()
        {
            Object value = getValue();
            if( value != null )
            {
                Class<?> vt = value.getClass();
                for( LayouterDescriptor ll : layoutDescr )
                    if( vt.equals(ll.getType()) )
                        return ll.getTitle();
            }

            return "";
        }

        @Override
        public void setAsText(String text)
        {
            for( LayouterDescriptor ll : layoutDescr )
            {
                if( text.equals(ll.getTitle()) )
                {
                    try
                    {
                        setValue(ll.createLayouter());
                    }
                    catch( Exception e )
                    {
                    }
                }
            }
        }
    }

}
