package ru.biosoft.proteome.table;

import java.awt.Component;

import javax.swing.JLabel;

import com.developmentontheedge.beans.BeanInfoEx;
import com.developmentontheedge.beans.editors.CustomEditorSupport;

import ru.biosoft.graphics.CompositeViewMessageBundle;

public class Structure3DBeanInfo extends BeanInfoEx
{
    public Structure3DBeanInfo()
    {
        super(Structure3D.class, CompositeViewMessageBundle.class.getName());
        setBeanEditor(Structure3DViewer.class);
    }

    public static class Structure3DViewer extends CustomEditorSupport
    {
        @Override
        public Component getCustomRenderer(Component parent, boolean isSelected, boolean hasFocus)
        {
            Object value = getValue();
            if( value instanceof Structure3D )
            {
                StringBuilder title = new StringBuilder();
                for( int i = 0; i < ( (Structure3D)value ).getSize(); i++ )
                {
                    if( i > 0 )
                        title.append(',');
                    title.append( ( (Structure3D)value ).getLink(i).getFirst());
                }
                return new JLabel(title.toString());
            }
            return new JLabel();
        }

        @Override
        public Component getCustomEditor(Component parent, boolean isSelected)
        {
            return getCustomRenderer(parent, isSelected, false);
        }
    }
}
