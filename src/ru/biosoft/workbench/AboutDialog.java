package ru.biosoft.workbench;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.URL;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextPane;
import javax.swing.SwingConstants;

import java.util.logging.Level;
import java.util.logging.Logger;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.Platform;
import org.osgi.framework.Bundle;

import com.developmentontheedge.application.Application;
import com.developmentontheedge.application.ApplicationUtils;

public class AboutDialog
        extends JDialog
{
    private static Logger log = Logger.getLogger( AboutDialog.class.getName() );

    private JTabbedPane tabbedPanel;

    private int width = 200;

    private int height = 150;

    private Bundle bundle;

    private String resources;

    public AboutDialog ( final JFrame parent, String title )
    {
        super ( parent, "About " + title, true );

        tabbedPanel = new JTabbedPane ( );

        IExtension extension = Application.getExtensionRegistry ( ).getExtension ( "ru.biosoft.workbench.aboutDialog",
                AboutAction.getStartupPluginName ( ) + ".aboutDialog" );

        String pluginId = extension.getNamespaceIdentifier ( );

        resources = Platform.getBundle ( pluginId ).getEntry ( "/" ).toString ( ).concat ( "resources/" );
        bundle = Platform.getBundle( pluginId );

        IConfigurationElement[] elements = extension.getConfigurationElements ( );

        initSize ( elements );
        createTabs ( elements );

        JButton okButton = new JButton ( "OK" );
        okButton.addActionListener ( new ActionListener ( )
        {
            @Override
            public void actionPerformed ( ActionEvent e )
            {
                setVisible ( false );
                dispose ( );
            }
        } );

        JPanel buttonPanel = new JPanel ( new FlowLayout ( FlowLayout.RIGHT ) );
        buttonPanel.add ( okButton );

        JPanel panel = new JPanel ( new BorderLayout ( ) );
        panel.add ( tabbedPanel, BorderLayout.CENTER );
        panel.add ( buttonPanel, BorderLayout.SOUTH );

        getContentPane ( ).add ( panel );
        getRootPane ( ).setDefaultButton ( okButton );
    }

    protected void initSize ( IConfigurationElement[] elements )
    {
        for ( IConfigurationElement el : elements )
            if ( el.getName ( ).equals ( "size" ) )
                try
                {
                    int wd = Integer.parseInt ( el.getAttribute ( "width" ) );
                    int ht = Integer.parseInt ( el.getAttribute ( "height" ) );
                    if ( wd > 0 && ht > 0 )
                    {
                        width = wd;
                        height = ht;
                    }
                }
                catch ( Throwable t )
                {
                    log.log( Level.SEVERE, "Incorrect AboutDialog size definition." );
                }
    }

    protected void createTabs ( IConfigurationElement[] elements )
    {
        for ( IConfigurationElement el : elements )
            if ( el.getName ( ).equals ( "tab" ) )
            {
                String name = el.getAttribute ( "name" );
                String html = el.getAttribute ( "html" );
                String img = el.getAttribute ( "img" );
                if ( name != null && html != null )
                    createTab ( name, html );
                else if ( name != null && img != null )
                    createImageTab ( name, img );
            }
    }

    protected URL getResource ( String name )
            throws Exception
    {
        URL url = null;

        // try to load through plugin class loader
        url = bundle.getResource ( name );
        if ( url == null ) // try to get resources as file
            url = new URL ( resources + name );

        return url;
    }

    protected void createTab ( String name, String file )
    {
        try
        {
            URL url = getResource ( file );
            JTextPane pane = new JTextPane ( );
            pane.setContentType ( "text/html" );
            pane.setPage ( url );
            pane.setForeground ( Color.blue.darker ( ) );
            pane.setBorder ( BorderFactory.createEmptyBorder ( 5, 5, 5, 5 ) );
            pane.setEditable ( false );
            pane.setCaretPosition ( 0 );
            JScrollPane scrollPane = new JScrollPane ( pane );
            scrollPane.setPreferredSize ( new Dimension ( width, height ) );
            tabbedPanel.add ( name, scrollPane );
        }
        catch ( Throwable t )
        {
            log.log( Level.SEVERE, "Can not add tab " + name + " into about dialog", t );
        }
    }

    protected void createImageTab ( String name, String file )
    {
        try
        {
            URL url = getResource ( file );
            JLabel splashLabel = new JLabel ( );
            splashLabel.setHorizontalAlignment ( SwingConstants.CENTER );
            splashLabel.setVerticalAlignment ( SwingConstants.CENTER );
            splashLabel.setIcon ( new ImageIcon ( url ) );
            tabbedPanel.add ( name, splashLabel );
        }
        catch ( Throwable t )
        {
            log.log( Level.SEVERE, "Cannot add image tab " + name + " into about dialog", t );
        }
    }

    public int doModal ( )
    {
        pack ( );
        setSize ( getPreferredSize ( ) );
        ApplicationUtils.moveToCenter ( this );
        setResizable ( false );
        setVisible ( true );
        return 0;
    }

}
