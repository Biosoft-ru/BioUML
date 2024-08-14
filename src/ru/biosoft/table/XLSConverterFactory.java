package ru.biosoft.table;

import java.io.File;

public class XLSConverterFactory
{
    public static XLSandXLSXConverters getXLSConverter(File file, boolean tryNewConverter)
    {
        if( tryNewConverter )
        {
            XLSandXLSXConverters result = XLSandXLSXToTabConvertersNew.getConverter( file );
            if( result != null )
                return result;
        }
        return getXLSConverter( file );
    }

    public static XLSandXLSXConverters getXLSConverter(File file)
    {
        try
        {
            return ( new XLSToTabConverter(file) );
        }
        catch( Exception ex1 )
        {
            try
            {
                return ( new XLSXToTabConverter(file) );
            }
            catch( Exception ex2 )
            {
                return null;
            }
        }
    }
}
