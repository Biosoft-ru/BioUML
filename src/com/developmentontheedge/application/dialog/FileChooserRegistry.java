package com.developmentontheedge.application.dialog;

import java.awt.Component;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.util.HashMap;

import javax.swing.JFileChooser;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileView;

import java.util.logging.Level;
import java.util.logging.Logger;

public class FileChooserRegistry
{

    public final static String OTHER_FILES     = "Other files entry";
    public final static String SEQUENCE_FILES  = "Sequence files entry";
    public final static String MATRIX_FILES    = "Matrix files entry";
    public final static String PROFILE_FILES   = "Profile files entry";

    public final static int SAVE_MODE       = 0;
    public final static int OPEN_MODE       = 1;

    private static Logger cat = Logger.getLogger( FileChooserRegistry.class.getName() );

    private static HashMap<String, RegistryEntry> registryMap = null;
    public static void init()
    {
        registryMap = new HashMap<>();
        // OTHER_FILES
        RegistryEntry entry = new RegistryEntry(
                                               null,
                                               null
                                               );
        registryMap.put( OTHER_FILES, entry );
        // MATRIX_FILES
        entry = new RegistryEntry(
                                 new FileFilter()
                                 {
                                     /**
                                      * Whether the given file is accepted by this filter.
                                      */
                                     @Override
                                    public boolean accept(File f)
                                     {
                                         if( f.getAbsolutePath().endsWith( ".mat" ) || f.isDirectory() )
                                         {
                                             return true;
                                         }
                                         return false;
                                     }
                                     /**
                                      * The description of this filter. For example: "JPG and GIF Images"
                                      * @see FileView#getName
                                      */
                                     @Override
                                    public String getDescription()
                                     {
                                         return "Matrices files (*.mat)";
                                     }
                                 },
                                 null
                                 );
        registryMap.put( MATRIX_FILES, entry );
        // SEQUENCE_FILES
        entry = new RegistryEntry(
                                 new FileFilter()
                                 {
                                     /**
                                      * Whether the given file is accepted by this filter.
                                      */
                                     @Override
                                    public boolean accept(File f)
                                     {
                                         if( f.getAbsolutePath().endsWith( ".seq" ) || f.isDirectory() )
                                         {
                                             return true;
                                         }
                                         return false;
                                     }
                                     /**
                                      * The description of this filter. For example: "JPG and GIF Images"
                                      * @see FileView#getName
                                      */
                                     @Override
                                    public String getDescription()
                                     {
                                         return "Sequences files (*.seq)";
                                     }
                                 },
                                 null
                                 );
        registryMap.put( SEQUENCE_FILES, entry );
        // PROFILE_FILES
        entry = new RegistryEntry(
                                 new FileFilter()
                                 {
                                     /**
                                      * Whether the given file is accepted by this filter.
                                      */
                                     @Override
                                    public boolean accept(File f)
                                     {
                                         if( f.getAbsolutePath().endsWith( ".prf" ) || f.isDirectory() )
                                         {
                                             return true;
                                         }
                                         return false;
                                     }
                                     /**
                                      * The description of this filter. For example: "JPG and GIF Images"
                                      * @see FileView#getName
                                      */
                                     @Override
                                    public String getDescription()
                                     {
                                         return "Profile files (*.prf)";
                                     }
                                 },
                                 null
                                 );
        registryMap.put( PROFILE_FILES, entry );
    }

    private static JFileChooser chooser;

    public static File getSelectedFile()
    {
        return chooser == null ? null : chooser.getSelectedFile();
    }



    public static boolean showOpenDialog( final Component parent, final int mode, final String dialogType ,final int saveOpenMode)
    {
        if(registryMap == null)
        {
            init();
        }
        chooser = new JFileChooser();
        RegistryEntry entry = registryMap.get( dialogType );
        if(entry != null)
        {
            if(entry.getFilter() != null)  chooser.setFileFilter(       entry.getFilter() );
            if(entry.getHomeDir() != null) chooser.setCurrentDirectory( entry.getHomeDir() );
        }
        else
        {
            cat.log( Level.SEVERE, "File chooser registry entry not found:" + dialogType );
        }
        if(mode == JFileChooser.DIRECTORIES_ONLY)
        {
            chooser.setSelectedFile( new File( chooser.getCurrentDirectory(), "choose folder" ) );
            chooser.addPropertyChangeListener( new PropertyChangeListener()
                                               {
                                                   /**
                                                    * This method gets called when a bound property is changed.
                                                    * @param evt A PropertyChangeEvent object describing the event source
                                                    *      and the property that has changed.
                                                    */
                                                   @Override
                                                public void propertyChange(PropertyChangeEvent evt)
                                                   {
                                                       if(evt.getPropertyName().equals("directoryChanged") )
                                                       {
                                                           chooser.setSelectedFile( new File( chooser.getCurrentDirectory(), "choose folder" ) );
                                                       }
                                                   }

                                               }
                                             );
        }
        else
        {
        }
        chooser.setFileSelectionMode( mode );
        boolean good =  ( saveOpenMode == OPEN_MODE ?  chooser.showOpenDialog(parent) : chooser.showSaveDialog(parent)) == JFileChooser.APPROVE_OPTION ;
        if( good )
        {
            entry.setHomeDir( chooser.getCurrentDirectory() );
        }
        return good;
    }

    private static class RegistryEntry
    {
        public RegistryEntry( FileFilter filter, File homeDir )
        {
            this.filter    = filter;
            this.homeDir   = homeDir;
        }
        private FileFilter filter;
        public FileFilter getFilter()
        {
            return filter;
        }
        private File homeDir;
        public File getHomeDir()
        {
            return homeDir;
        }
        public void setHomeDir(File homeDir)
        {
            this.homeDir = homeDir;
        }
    }
}
