
package com.developmentontheedge.application.dialog;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyListener;
import java.io.File;
import java.util.Vector;

import javax.swing.JFileChooser;

public class BrowseTextField extends TextButtonField
{
    protected File selectedFile = null;

    private Vector<ActionListener> listeners;

    public BrowseTextField(Component parent, String dialogType )
    {
        this(parent, JFileChooser.FILES_ONLY, dialogType ,FileChooserRegistry.OPEN_MODE );
    }
    
    public BrowseTextField(Component parent, String dialogType ,final int saveOpenMode)
    {
        this(parent, JFileChooser.FILES_ONLY, dialogType ,saveOpenMode);
    }
    
    public BrowseTextField( Component parent, final int mode, final String dialogType ,final int saveOpenMode)
    {
        super("...");
        button.setPreferredSize(new Dimension(20,20));
        textField.setPreferredSize(new Dimension(150,20));
        final Component prnt = parent;
        button.addActionListener(new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                if( FileChooserRegistry.showOpenDialog(prnt, mode, dialogType, saveOpenMode) )
                {
                    selectedFile = FileChooserRegistry.getSelectedFile();
                    //System.out.println("selectedFile="+selectedFile);
                    //System.out.println("selectedFile..."+selectedFile.getName());
                    //System.out.println("fc.getName()="+fc.getName());
                    File file = new File(selectedFile.getAbsolutePath());

                    //if( !file.exists()  ) //|| !file.isDirectory()  //removed by lesa  //JFileChooser
                    //{
                    //    file = file.getParentFile();
                    //}
                    textField.setText(file.getPath());
                    if( listeners != null )
                    {
                        for( ActionListener listener : listeners )
                        {
                            listener.actionPerformed(new ActionEvent(this, 0, "light my fire"));
                        }
                    }
                }
            }
        });
    }


    public File getFile() throws Exception
    {
        String name = getFileName();
        File file = new File( name );
        /*
        if (!file.exists())
        {
            throw new Exception("File "+name+" not found");
        }
        */
        return file;
    }

    public void  setFileName(String name)
    {
        textField.setText(name);
    }

    public String  getFileName() throws Exception
    {
        String name = textField.getText();
        /*
        if (name==null || name.length()==0)
            throw new Exception("File name is empty");
        */
        return name;
    }


    public void addListener(Object listener)
    {
        if(listeners==null)
            listeners = new Vector<>();
        
        if( listener != null)
        {
            if(listener instanceof KeyListener)
            {
                textField.addKeyListener( (KeyListener)listener );
            }
            else  if (listener instanceof ActionListener)
            {
                listeners.add((ActionListener)listener);
            }
        }
    }
}
