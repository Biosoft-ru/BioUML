package ru.biosoft.plugins.jri;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.util.Collections;
import java.util.List;

import org.apache.commons.lang.SystemUtils;

import ru.biosoft.exception.ExceptionRegistry;
import ru.biosoft.util.WinRegistry;

/**
 * Utility methods for R
 */
public class RUtility
{
    private static String rLocation = null;
    private static volatile boolean rLocationInit = false;

    /**
     * Get install location of R
     */
    public static String getRLocation()
    {
        if( !rLocationInit )
        {
            if( SystemUtils.IS_OS_WINDOWS )
            {
                try
                {
                    List<String> subKeys = WinRegistry.readStringSubKeys( WinRegistry.HKEY_LOCAL_MACHINE, "Software\\R-core\\R" );
                    if(!subKeys.isEmpty())
                    {
                        Collections.sort( subKeys );
                        String version = subKeys.get( subKeys.size()-1 );
                        String path = WinRegistry.readString( WinRegistry.HKEY_LOCAL_MACHINE, "Software\\R-core\\R\\"+version, "InstallPath" );
                        if(path != null)
                        {
                            String osarch = System.getProperty( "os.arch" );
                            boolean is64 = osarch != null && osarch.contains( "64" );
                            path += is64 ? "\\bin\\x64\\R.exe" : "\\bin\\R.exe";
                        }
                        rLocation = path;
                    } else
                    {
                        rLocation = null;
                    }
                }
                catch( IllegalArgumentException | IllegalAccessException | InvocationTargetException | IllegalStateException e )
                {
                    ExceptionRegistry.log(e);
                    rLocation = null;
                }
            }
            else
            {
                rLocation = "/usr/local/lib/R/bin/R";
                if( new File(rLocation).exists() )
                {
                    return rLocation;
                }
                rLocation = "/usr/lib/R/bin/R";
                if( new File(rLocation).exists() )
                {
                    return rLocation;
                }
                rLocation = "/usr/local/bin/R";
                if( new File(rLocation).exists() )
                {
                    return rLocation;
                }
                rLocation = "/sw/bin/R";
                if( new File(rLocation).exists() )
                {
                    return rLocation;
                }
                rLocation = "/usr/common/bin/R";
                if( new File(rLocation).exists() )
                {
                    return rLocation;
                }
                rLocation = "R";
            }
            rLocationInit = true;
        }
        return rLocation;
    }

    /**
     * removes dots in the middle of path, like: C:/someFolder1/./someFolder2/ => C:/someFolder1/SomeFolder2
     * Is needed because R does not understand paths of first type
     */
    public static String removeUnneccessaryDots(String path)
    {
        return path.replace("\\.\\", "\\");
    }


    public static String escapeRString(String src)
    {
        return src.replaceAll("([\\\\\\\'\\\"])", "\\\\$1");
    }
}
