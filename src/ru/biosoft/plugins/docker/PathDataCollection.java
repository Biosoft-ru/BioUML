package ru.biosoft.plugins.docker;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataCollectionConfigConstants;
import ru.biosoft.access.core.VectorDataCollection;

import ru.biosoft.analysiscore.AnalysisMethodRegistry;

public class PathDataCollection/*<T extends DataElement>*/ extends VectorDataCollection/*<T>*/
{
    Set<String> paths;
    Properties properties;

    public PathDataCollection(String name, DataCollection<?> parent, Properties properties, Set<String> paths )
    {
        super(name, parent, properties);
        this.paths = paths;
    }

    public void addPath( String path )
    {
        paths.add( path );  
    }   

    public void fill()
    {
        ArrayList<String> nameList = new ArrayList<>();
        for( String path : paths )
        {
            String elem = path.split( "/" )[ 0 ];
            if( !nameList.contains( elem ) )
            {
                nameList.add( elem ); 
            }
        }
        
        for( String name : nameList )
        {
            Set<String> subpaths = new HashSet<>();
            for( String path : paths )
            {
                if( path.startsWith( name + "/" ) )
                {                  
                    subpaths.add( path.substring( ( name + "/" ).length() ) );
                }     
            }

            final String iconPrefix = "ru.biosoft.plugins.docker:ru/biosoft/plugins/docker/resources";
          
            String completePathStr = getCompletePath().toString();
            String []completePath = getCompletePath().getPathComponents();
            Properties props = null;
            boolean isLeaf = false;  
            //  analyses/Jupyter/developmentontheedge/biouml-scipyr-notebook:jh.1.0.1/bioumlkernel 
           
            if( completePathStr.startsWith( "analyses/Jupyter" ) )
            {
                if( completePath.length == 3 )
                {
                    props = new Properties() {{ 
                        put( DataCollectionConfigConstants.NODE_IMAGE, iconPrefix + "/logo-docker.png" );
                    }};
                }
                else if( completePath.length == 4 )
                {
                    put( new JupyterKernelDataElement( name, this ) );
                    isLeaf = true;
                }
            }   
            else if( completePathStr.startsWith( "analyses/Docker" ) )
            {
                if( completePath.length == 3 )
                {
                    props = new Properties() {{ 
                        put( DataCollectionConfigConstants.NODE_IMAGE, iconPrefix + "/logo-docker.png" );
                    }};
                }
                else if( completePath.length == 5 )
                {
                    CwlScriptDataElement cwlMethod = new CwlScriptDataElement( name, name, name, this );
                    AnalysisMethodRegistry.addMethod( name, cwlMethod );
                    put( cwlMethod );
                    isLeaf = true;
                }
            }   

            if( isLeaf )
            {                
            }   
            else 
            { 
                PathDataCollection pathDC = new PathDataCollection( name, this, props, subpaths );
                pathDC.fill();
                put( pathDC );
            }              
        }
    }
}