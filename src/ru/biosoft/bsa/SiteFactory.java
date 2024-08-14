package ru.biosoft.bsa;

public class SiteFactory
{
     public static final String OLD_STR_BASIS_NOT_KNOWN      =  "not-known"      ;
     public static final String OLD_STR_BASIS_EXPERIMENTAL   =  "experimental"   ;
     public static final String OLD_STR_BASIS_COMPUTATIONAL  =  "computational"  ;
    
    public static final String STR_BASIS_NOT_KNOWN      =  "user defined";
    public static final String STR_BASIS_EXPERIMENTAL   =  "annotated"   ;
    public static final String STR_BASIS_COMPUTATIONAL  =  "predicted"   ;

    final static public String toString(int i)
    {
        switch (i)
        {
            case Basis.BASIS_USER      : return STR_BASIS_NOT_KNOWN    ;
            case Basis.BASIS_ANNOTATED : return STR_BASIS_EXPERIMENTAL ;
            case Basis.BASIS_PREDICTED : return STR_BASIS_COMPUTATIONAL;
            default                    : return null;
        }
    }

    final static public int toInteger(String str)
    {
        if(    str.equals(STR_BASIS_NOT_KNOWN)
            || str.equals(OLD_STR_BASIS_NOT_KNOWN) )
            return Basis.BASIS_USER  ;
        else if(   str.equals(STR_BASIS_EXPERIMENTAL)
                || str.equals(OLD_STR_BASIS_EXPERIMENTAL) )
            return Basis.BASIS_ANNOTATED ;
        else if(   str.equals(STR_BASIS_COMPUTATIONAL )
                || str.equals(OLD_STR_BASIS_COMPUTATIONAL) )
            return Basis.BASIS_PREDICTED  ;

        return -1;
    }
}
