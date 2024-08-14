package ru.biosoft.util;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Desktop;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;

import javax.swing.JDialog;
import javax.swing.JFrame;

import ru.biosoft.exception.ExceptionRegistry;
import com.developmentontheedge.beans.swing.PropertyInspector;
import com.developmentontheedge.beans.web.HtmlPane;
import com.developmentontheedge.application.dialog.OkCancelDialog;

public class PropertiesDialog extends OkCancelDialog
{
    private static final long serialVersionUID = 1L;
    private Object properties;

    public PropertiesDialog(JDialog dialog, String title, Object properties)
    {
        super( dialog, title );
        init( properties );
    }

    public PropertiesDialog(JFrame frame, String title, Object properties)
    {
        super( frame, title );
        init( properties );
    }

    private void init(Object properties)
    {
        this.properties = properties;
        String description = null;
        try
        {
            description = (String)BeanUtil.getBeanPropertyValue( properties, "descriptionHTML" );
        }
        catch( Exception e )
        {
            // Ignore
        }
        if( description != null )
        {
            @SuppressWarnings ( "serial" )
            HtmlPane descriptionPane = new HtmlPane()
            {
                @Override
                protected void followHyperlink(Object data)
                {
                    try
                    {
                        Desktop.getDesktop().browse( ( (URL)data ).toURI() );
                    }
                    catch( IOException | URISyntaxException e )
                    {
                        ExceptionRegistry.log(e);
                    }
                }
            };
            descriptionPane.setInitialText( description );
            add( descriptionPane, BorderLayout.NORTH );
        }
        add( getDialogContent() );
    }

    public Object getProperties()
    {
        return properties;
    }

    public Component getDialogContent()
    {
        PropertyInspector propertyInspector = new PropertyInspector();
        propertyInspector.explore( properties );
        return propertyInspector;
    }
}