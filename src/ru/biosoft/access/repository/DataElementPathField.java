package ru.biosoft.access.repository;

import java.awt.Color;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.beans.IntrospectionException;

import javax.swing.JTextField;

import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.DataElementPath;

import com.developmentontheedge.beans.PropertyDescriptorEx;

public class DataElementPathField extends JTextField
{
    DataElementPathEditor editor = null;
    Class<? extends DataElement> wantedType = null;
    boolean isInput = false;

    public DataElementPathField(String propertyName, Class<? extends DataElement> wantedType, String startMessage,
            DataElementPath defaultPath, final boolean isinput)
    {
        super(25);
        setEditable(false);
        setBackground(Color.WHITE);
        setText(startMessage);

        this.wantedType = wantedType;
        this.isInput = isinput;

        editor = new DataElementPathEditor();
        PropertyDescriptorEx descriptor = null;
        try
        {
            if( isInput )
            {
                descriptor = DataElementPathEditor.registerInput(new PropertyDescriptorEx(propertyName), wantedType);
            }
            else
            {
                descriptor = DataElementPathEditor.registerOutput(new PropertyDescriptorEx(propertyName), wantedType);
            }
            editor.setDescriptor(descriptor);
        }
        catch( IntrospectionException e )
        {
        }

        editor.setValue(defaultPath);
        if( isSelectionValid(defaultPath, isinput) )
        {
            setText(defaultPath.getName());
        }


        MouseListener mouseListener = new MouseAdapter()
        {
            /**
             * Invoked when the mouse has been clicked on a component.
             */
            @Override
            public void mousePressed(MouseEvent arg0)
            {
                DataElementPathDialog dialog = editor.getDialog();
                if( !isInput )
                {
                    dialog.setPromptOverwrite(true);
                }
                if( dialog.doModal() )
                {
                    DataElementPath epath = dialog.getValue();
                    if( isSelectionValid(epath, isinput) )
                    {
                        editor.setValue(epath);
                        setText(epath.getName());
                    }
                }
            }
        };
        addMouseListener(mouseListener);

    }
    public DataElementPathField(int size)
    {
        super(size);
    }

    public boolean isSelectionValid(DataElementPath path, boolean isInput)
    {
        if( path != null && ( !path.getName().equals("") ) && ( !isInput || path.exists() ) )
        {
            return true;
        }
        return false;
    }
    public DataElementPath getPath()
    {
        Object path = editor.getValue();
        if( path != null )
            return (DataElementPath)path;
        return null;
    }
    
    public void setPath (DataElementPath path)
    {
        editor.setValue(path);
        setText(path.getName());
    }
}