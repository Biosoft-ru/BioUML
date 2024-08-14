package biouml.plugins.gtrd.master.utils;

import com.google.common.collect.Interner;
import com.google.common.collect.Interners;

//Set of strings used to save memory occupied by a lot of strings with the same content.
//Similar to java.lang.String.intern() but faster.
public class StringPool
{
    private static final Interner<String> interner = Interners.newWeakInterner();
    public static String get(String x)
    {
        return interner.intern( x );
    }
}
