package biouml.plugins.gtrd.master.utils;

import java.lang.reflect.Field;
import java.util.ArrayList;

public class SizeOfUtils
{
    public static long align(long size)
    {
        int rem = (int) ( size % 8 );
        if( rem == 0 )
            return size;
        return size + 8 - rem;
    }

    public static long sizeOfArrayList(ArrayList<?> list, long elementSize)
    {
        long res = 16 + 4 + 8;//object header, size, array pointer
        res = align( res );
        int capacity = arrayListCapacity( list );
        res += sizeOfArray(capacity, 8);//array with pointers
        res += list.size() * elementSize;
        return res;
    }

    
    public static long sizeOfArrayList(ArrayList<? extends SizeOf> list)
    {
        long res = 16 + 4 + 8;//object header, size, array pointer
        res = align( res );
        int capacity = arrayListCapacity( list );
        res += sizeOfArray(capacity , 8);//array with pointers
        for(SizeOf e : list)
            res += e.sizeOf();
        return res;
    }
    
    private static int arrayListCapacity(ArrayList<?> l)
    {
        try
        {
            Field dataField = ArrayList.class.getDeclaredField( "elementData" );
            dataField.setAccessible( true );
            return ( (Object[])dataField.get( l ) ).length;
        }
        catch( Exception e )
        {
            throw new RuntimeException( e );
        }
    }

    public static long sizeOfString(String str)
    {
        long res =  16 //header
               + 4//hash
               + 8//char array pointer
               ;
        res = align(res);
        res += sizeOfArray(str.length(), 2);
        return res;
    }
    
    public static long sizeOfArray(int length, long elementSize)
    {
        long res = 16// header
               +4 //length
               +length*elementSize;
        return align(res);
    }
}
