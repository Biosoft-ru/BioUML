package ru.biosoft.access;

import java.io.File;
import java.io.FileFilter;
import java.util.StringTokenizer;

/**
 * Implements simple filter for files.
 * @see FileFilter
 */
public class SimpleFileFilter implements FileFilter
{
    protected String[] suffixes;

    public SimpleFileFilter(String suffixesString)
    {
        if( suffixesString == null )
            suffixesString = "*";

        if( !suffixesString.equals("*") )
        {
            StringTokenizer tokens = new StringTokenizer(suffixesString, " ,;");
            suffixes = new String[tokens.countTokens()];

            for( int i = 0; i < suffixes.length; i++ )
                suffixes[i] = tokens.nextToken();
        }
        else
        {
            suffixes = new String[1];
            suffixes[0] = "*";
        }
    }

    @Override
    public boolean accept(File pathname)
    {
        if( suffixes == null || suffixes.length == 0 )
        {
            if( pathname.isDirectory() )
                return false;
            String name = pathname.getName();
            return ( name.indexOf(".") == -1 );
        }

        if( suffixes[0].equals("*") )
            return true;

        String name = pathname.getName();

        for( int i = 0; i < suffixes.length; i++ )
        {
            if( name.endsWith(suffixes[i]) )
                return true;
        }

        return false;
    }
}