package ru.biosoft.access._test;

import java.beans.BeanInfo;
import java.beans.Introspector;
import java.beans.MethodDescriptor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

import junit.framework.TestCase;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataCollectionEvent;
import ru.biosoft.access.core.DataCollectionListener;
import ru.biosoft.access.core.DataCollectionVetoException;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.util.ExProperties;

/**
 * Base test suite for all DataCollections.
 * Contains universal test cases for all DataCollections.
 * @author www.DevelopmentOnTheEdge.com
 * @version 1.0
 */
abstract public class DataCollectionTest extends TestCase
{
    final static int MAX_SIZE = 10000;
    protected ExProperties properties = new ExProperties();
    /** DataCollection for testing */
    protected DataCollection<DataElement> dataCollection = null;
    /** Events that set in event listener */
    protected DataCollectionEvent eventAdded      = null;
    protected DataCollectionEvent eventRemoved    = null;
    protected DataCollectionEvent eventChanged    = null;
    protected DataCollectionEvent eventWillAdd    = null;
    protected DataCollectionEvent eventWillChange = null;
    protected DataCollectionEvent eventWillRemove = null;
   
    protected DataElement willRemoveDE = null;

    /** Event listener */
    protected DataCollectionListener listener = null;
    /** This flag say that elementWill* events will be throw DataCollectionVetoException */
    protected boolean throwFlag = false;

    ////////////////////////////////////////////////////////////////////////////
    // Customizing functions for using in derived classes
    //

    /**
     *  Derived classes may use this method for prepare data (files,..) before test case run.
     */
     protected void prepareData()
     {
     }

    /**
     *  Method which derived classes must call, for setup testing ru.biosoft.access.core.DataCollection
     *  @param dc DataCollection which will be tested by this TestSuite
     */
    public void setDataCollection(DataCollection<DataElement> dc)
    {
        dataCollection = dc;
        if( dc != null )
        {
            listener = new TestDataCollectionListener();
            dataCollection.addDataCollectionListener( listener );
        }
    }

    /**
     * Method for override in derived classes.
     *
     * @return Original size of data collection (size that expected)
     */
    abstract protected int getOriginalSize() ;

    /**
     * Method for override in derived classes.
     * @return Original name of data collection (name that expected)
     */
    abstract protected String getOriginalName();

    ////////////////////////////////////////////////////////////////////////////
    // Utility functions for TestCases
    //

    /**
     *  Check that testing data collection is set (not null).
     *  @see #setDataCollection(DataCollection)
     */
    protected  void checkDataCollection()
    {
        assertNotNull("Please use setDataCollection(DataCollection) method",dataCollection );
    }

    /**
     * Clear all events data
     */
    protected void resetEvents()
    {
        eventAdded        = null;
        eventRemoved      = null;
        eventChanged      = null;
        eventWillAdd      = null;
        eventWillChange   = null;
        eventWillRemove   = null;
        willRemoveDE = null;
    }

    /**
     * Compare two data elements
     * @throws Exception
     */
    protected void compare ( ru.biosoft.access.core.DataElement de, DataElement de1 ) throws Exception
    {
        if ( de == de1 )
            return;
        if ( de.equals ( de1 ) )
            return;
        
        BeanInfo info = Introspector.getBeanInfo ( de.getClass ( ) );
        MethodDescriptor[] desc = info.getMethodDescriptors ( );
        for( MethodDescriptor element : desc )
        {
            Method m = element.getMethod ( );
            //check only string getters
            if ( m.getReturnType ( ) == String.class &&
                 m.getParameterTypes ( ).length == 0 )
            {
                assertTrue("object should be the same", ( "" + m.invoke ( de1 ) ).equals ( "" + m.invoke ( de ) ) );
            }
        }
    }
    
    /**
     *  Test that all events data correctly set after ru.biosoft.access.core.DataCollection.remove() method call.
     *  Use throwFlag
     *  @param de Data element that was removed in ru.biosoft.access.core.DataCollection.remove() method.
     *  @see #throwFlag
     *  @see mgl3.access.ru.biosoft.access.core.DataCollectionListener
     */
    protected void assertRemoveEvents(DataElement de) throws Exception
    {
        if (!throwFlag)
        {
            assertNotNull("ru.biosoft.access.core.DataCollectionListener.elementRemoved() not called after removing"
                          ,eventRemoved);
            assertSame( "ru.biosoft.access.core.DataCollectionListener.elementRemoved(), event.getDataElement() return "+
                        eventRemoved.getDataElementName()+
                        ". Should be "+de.getName()+". "
                        ,eventRemoved.getDataElementName(),de.getName());
        }
        else
        {
            assertNull("ru.biosoft.access.core.DataCollectionListener.elementRemoved() called after removing with veto",
                       eventRemoved);
        }
        assertNull("ru.biosoft.access.core.DataCollectionListener.elementAdded() called after removing"
                   ,eventAdded);
        assertNull("ru.biosoft.access.core.DataCollectionListener.elementChanged() called after removing"
                   ,eventChanged);
        assertNull("ru.biosoft.access.core.DataCollectionListener.elementWillAdd() called in removing"
                   ,eventWillAdd);
        assertNull("ru.biosoft.access.core.DataCollectionListener.elementWillChange() called in removing"
                   ,eventWillChange);
        assertNotNull("ru.biosoft.access.core.DataCollectionListener.elementWillRemove() not called before removing"
                      ,eventWillRemove);
    }

    /**
     *  Test that all events data correctly set after ru.biosoft.access.core.DataCollection.put() method call.
     *  Use throwFlag
     *  @param de Data element that was added in ru.biosoft.access.core.DataCollection.put() method.
     *  @see #throwFlag
     *  @see mgl3.access.ru.biosoft.access.core.DataCollectionListener
     */
    protected void assertPutEvents(DataElement  de) throws Exception
    {
        if ( throwFlag )
            assertNull("ru.biosoft.access.core.DataCollectionListener.elementAdded() called after adding with veto"
                       ,eventAdded);
        else
        {
            assertNotNull("ru.biosoft.access.core.DataCollectionListener.elementAdded() not called after adding"
                          ,eventAdded);

            DataElement added = eventAdded.getOwner().get( eventAdded.getDataElementName() );

            assertSame("ru.biosoft.access.core.DataCollectionListener.elementAdded()  ["+added+"] has wrong argument."+
                       " Should be: "+de+". "
                       ,added,de);
        }
        assertNull("ru.biosoft.access.core.DataCollectionListener.elementRemoved() called after adding",eventRemoved);
        assertNull("ru.biosoft.access.core.DataCollectionListener.elementChanged() called after adding",eventChanged);
        assertNull("ru.biosoft.access.core.DataCollectionListener.elementWillRemove() called before adding",eventWillRemove);
        assertNull("ru.biosoft.access.core.DataCollectionListener.elementWillChange() called before adding",eventWillChange);
        assertNotNull("ru.biosoft.access.core.DataCollectionListener.elementWillAdd() not called when adding",eventWillAdd);
    }

    /**
     *  Test that all events data correctly set after ru.biosoft.access.core.DataCollection.put() method call with existing data element.
     *  Use throwFlag
     *  @param de Data element that was changed in ru.biosoft.access.core.DataCollection.put() method.
     *  @see #throwFlag
     *  @see mgl3.access.ru.biosoft.access.core.DataCollectionListener
     */
    protected void assertChangeEvents(DataElement  de) throws Exception
    {
        if ( throwFlag )
            assertNull("ru.biosoft.access.core.DataCollectionListener.elementChanged() called after changing with veto" ,eventChanged );
        else
        {
            assertNotNull("ru.biosoft.access.core.DataCollectionListener.elementChanged() not called after changing" ,eventChanged );
            DataElement changed = eventChanged.getOwner().get( eventChanged.getDataElementName() );
            assertSame("ru.biosoft.access.core.DataCollectionListener.elementChanged()  ["+changed+"] has wrong argument. Should be: "+de
                       ,changed,de);
        }
        assertNull("ru.biosoft.access.core.DataCollectionListener.elementRemoved() called after changing",eventRemoved);
        assertNull("ru.biosoft.access.core.DataCollectionListener.elementChanged() called after changing",eventAdded);
        assertNull("ru.biosoft.access.core.DataCollectionListener.elementWillAdd()  called when changing",eventWillAdd);
        assertNull("ru.biosoft.access.core.DataCollectionListener.elementWillRemove() called before changing",eventWillRemove);
        assertNotNull("ru.biosoft.access.core.DataCollectionListener.elementWillChange() not called before changing",eventWillChange);
    }

    /**
     *  Test result of ru.biosoft.access.core.DataCollection.put() execution.
     *  Test that element was added or not added(if DataCollectionVetoException was thrown).
     *  @param de Data element that was added in ru.biosoft.access.core.DataCollection.put() method.
     *  @see mgl3.access.ru.biosoft.access.core.DataCollection#contains(String)
     *  @see mgl3.access.ru.biosoft.access.core.DataCollection#put(mgl3.access.ru.biosoft.access.core.DataElement)
     */
    protected void assertPut(DataElement  de)
    {
        String name = de.getName();
        int oSize = getOriginalSize();
        if ( throwFlag )
        {
            assertTrue("[ put(DataElement) ] ru.biosoft.access.core.DataElement with name "+name+" added (but veto was thrown)"
                   ,!dataCollection.contains(name));
            assertTrue("[ put(DataElement) ] ru.biosoft.access.core.DataElement "+name+" added (but veto was thrown)"
                   ,!dataCollection.contains(de));
            assertTrue("[ put(DataElement) ] Size changed (but veto was thrown)"
                   ,dataCollection.getSize()!=oSize);
        } else
        {
            assertTrue("[ put(DataElement) ] ru.biosoft.access.core.DataElement with name "+name+" not added"
                   ,dataCollection.contains(name));
            assertTrue("[ put(DataElement) ] ru.biosoft.access.core.DataElement "+name+" not added"
                   ,dataCollection.contains(de));
            assertEquals("[ put(DataElement) ] Size not incremented"
                         ,dataCollection.getSize(),oSize);
        }
    }


    /**
     *  Test result of ru.biosoft.access.core.DataCollection.remove() execution.
     *  Test that element was removed or not removed(if DataCollectionVetoException was thrown).
     *  @param de Data element that was removed in ru.biosoft.access.core.DataCollection.remove() method.
     *  @see mgl3.access.ru.biosoft.access.core.DataCollection#getSize()
     *  @see mgl3.access.ru.biosoft.access.core.DataCollection#contains(String)
     */
    protected void assertRemove(DataElement  de)
    {
        String name = de.getName();
        int oSize = getOriginalSize();

        if (throwFlag)
        {
            assertTrue("[ remove(DataElement) ] ru.biosoft.access.core.DataElement "+name+" deleted ",dataCollection.contains(name));
            assertTrue("[ remove(DataElement) ] ru.biosoft.access.core.DataElement "+name+" deleted ",dataCollection.contains(de));
            assertEquals("[ remove(DataElement) ] Size  changed",dataCollection.getSize(),oSize);
        } else
        {
            assertTrue("[ remove(String) ] ru.biosoft.access.core.DataElement with name "+name+" not deleted",!dataCollection.contains(name));
            assertTrue("[ remove(String) ] ru.biosoft.access.core.DataElement "+name+" not deleted",          !dataCollection.contains(de));
            assertEquals("[ remove(String) ] Size not decremented",                dataCollection.getSize()+1,oSize);
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Test cases
    //

    /**
     *  TestCase for ru.biosoft.access.core.DataCollection.getSize().
     *  Compare real size and needed size.
     *  @see ru.biosoft.access.core.DataCollection#getSize()
     *  @see test.DataCollectionTest#getOriginalSize()
     */
    public void testGetSize()
    {
        checkDataCollection();
        int size1 = getOriginalSize();
        int size2 = dataCollection.getSize();
        assertEquals("Size wrong. ",size1,size2) ;
    }

    /**
     * TestCase for ru.biosoft.access.core.DataCollection.getName().
     * Compare real name and needed name.
     * @see ru.biosoft.access.core.DataCollection#getName()
     * @see #getOriginalName()
     */
    public void testGetName()
    {
        checkDataCollection();
        String name1 = getOriginalName();
        String name2 = dataCollection.getName();
        assertEquals("Name wrong. " , name1,name2) ;
    }

    /**
     *  TestCase for ru.biosoft.access.core.DataCollection.iterator(),ru.biosoft.access.core.DataCollection.get(),ru.biosoft.access.core.DataCollection.contains().
     *  Extract all data elements to List using iterator() and then test that each data element from vector
     *  contains in data collection. Also test length of iterator and size of data collection.
     *  @see #getOriginalSize()
     *  @see mgl3.access.ru.biosoft.access.core.DataCollection#get(String)
     *  @see mgl3.access.ru.biosoft.access.core.DataCollection#contains(mgl3.access.ru.biosoft.access.core.DataElement)
     *  @see mgl3.access.ru.biosoft.access.core.DataCollection#contains(String)
     *  @see mgl3.access.ru.biosoft.access.core.DataCollection#iterator()
     */
    public void testIterator() throws Exception
    {
        checkDataCollection();
        Iterator<DataElement> it  = dataCollection.iterator();
        List<DataElement> v = new ArrayList<>();
        int i=0;
        DataElement de;
        for (; it.hasNext() && i<MAX_SIZE; i++)
        {
            de = it.next();
            assertNotNull("iterator.next() returns null",de);
            v.add(de);
        }
        try
        {
            DataElement absent = it.next();
            assertNull( "superfluous element extracted through iterator()",absent );
            assertTrue( "NoSuchElementException not thrown!!!",false );
        }
        catch( NoSuchElementException exc )
        {}

        assertEquals("Size and iterator length not equals.",getOriginalSize(),i) ;
        for (i=0; i<v.size(); i++)
        {
            de  = v.get(i);
            String name = de.getName();
            assertTrue("ru.biosoft.access.core.DataElement with name "+name+" doesn't exist in the collection",dataCollection.contains(de));
            assertTrue("Name "+name+" doesn't exist in the collection",dataCollection.contains(name));
            DataElement  de1 =  dataCollection.get(name);
            assertNotNull( "ru.biosoft.access.core.DataElement <"+name+"> must be in data collection.",de1 );
            assertTrue("get("+name+") return not equal object ru.biosoft.access.core.DataElement with name ("+de1.getName()+")",de1 == de);
        }
    }

    /**
     *  TestCase for ru.biosoft.access.core.DataCollection.put() and ru.biosoft.access.core.DataCollection.remove().
     *  Remove first element from data collection and then add it back.
     *  All events also test.
     *  @see #assertRemoveEvents(mgl3.access.ru.biosoft.access.core.DataElement)
     *  @see #assertPutEvents(mgl3.access.ru.biosoft.access.core.DataElement)
     *  @see #assertRemove(mgl3.access.ru.biosoft.access.core.DataElement)
     *  @see #assertPut(mgl3.access.ru.biosoft.access.core.DataElement)
     *  @see mgl3.access.ru.biosoft.access.core.DataCollection#put(mgl3.access.ru.biosoft.access.core.DataElement)
     *  @see mgl3.access.ru.biosoft.access.core.DataCollection#remove(String)
     */
    public void testPutRemove() throws Exception
    {
        checkDataCollection();
        if ( !dataCollection.isMutable() )
            return;

        // simple test for elements identity
        DataElement  de  = dataCollection.iterator().next();
        DataElement  de1 = dataCollection.get(de.getName());
        assertTrue("objects should be equal: "+de+"  not equals  "+de1,de==de1);

        // No veto
        throwFlag = false;

        // remove data element
        String name = de.getName();
        resetEvents();
        dataCollection.remove(name);
        // test removing
        assertRemoveEvents(de);
        assertRemove(de);

        // put data element back
        resetEvents();

        dataCollection.put(de);

        // test putting
        assertPutEvents(de);
        assertPut(de);

        resetEvents();
    }

    /**
     *  TestCase for ru.biosoft.access.core.DataCollection.put() and ru.biosoft.access.core.DataCollection.remove().
     *  Remove first element from data collection and then add it back.
     *  But in both cases DataElementVetoException is thrown and so no any changes should occurs.
     *  All events also test.
     *  @see #testPutRemove()
     *  @see #assertRemoveEvents(mgl3.access.ru.biosoft.access.core.DataElement)
     *  @see #assertPutEvents(mgl3.access.ru.biosoft.access.core.DataElement)
     *  @see #assertRemove(mgl3.access.ru.biosoft.access.core.DataElement)
     *  @see #assertPut(mgl3.access.ru.biosoft.access.core.DataElement)
     *  @see mgl3.access.ru.biosoft.access.core.DataCollection#put(mgl3.access.ru.biosoft.access.core.DataElement)
     *  @see mgl3.access.ru.biosoft.access.core.DataCollection#remove(String)
     */
    public void testPutRemoveWithVeto() throws Exception
    {
        checkDataCollection();
        if( !dataCollection.isMutable() )
            return;

        DataElement de = dataCollection.iterator().next();
        String name = de.getName();

        // Remove with veto and test it.
        throwFlag = true;
        resetEvents();
        dataCollection.remove(name);
        assertRemoveEvents(de);
        assertRemove(de);

        // Remove without veto
        throwFlag = false;
        dataCollection.remove(name);

        // Put with veto and test it.
        throwFlag = true;
        resetEvents();
        dataCollection.put(de);
        assertPutEvents(de);
        assertPut(de);

        // Put with veto
        throwFlag = false;
        dataCollection.put(de);

        resetEvents();
    }

    /**
     *  TestCase for ru.biosoft.access.core.DataCollection.put() and elementWillChange event.
     *  Try add element with veto exception.
     *  @see #testPutRemove()
     *  @see #testPutRemoveWithVeto()
     *  @see #assertChangeEvents(mgl3.access.ru.biosoft.access.core.DataElement)
     *  @see mgl3.access.ru.biosoft.access.core.DataCollection#put(mgl3.access.ru.biosoft.access.core.DataElement)
     */
    public void testWillChange() throws Exception
    {
        checkDataCollection();

        if( !dataCollection.isMutable() )
            return;

        throwFlag = false;
        DataElement  de  = dataCollection.iterator().next();
        String name = de.getName();
        resetEvents();
        DataElement  de1 =  dataCollection.put(de);
        compare ( de, de1 );
        assertChangeEvents(de);

        throwFlag = true;
        resetEvents();
        de1 =  dataCollection.put(de);
        compare ( de, de1 );
        assertChangeEvents(de);

        int oSize = getOriginalSize();

        assertTrue("[ put(DataElement) ] ru.biosoft.access.core.DataElement with name "+name+" not changed",dataCollection.contains(name));
        assertTrue("[ put(DataElement) ] ru.biosoft.access.core.DataElement "+name+" not changed",dataCollection.contains(de));

        int sSize = dataCollection.getSize();
        assertEquals("[ put(DataElement) ] Size changed.",oSize,sSize);
    }

    /**
     *  TestCase for events without EventListener.
     *  Remove event listener and try put/remove elements.
     *  All events are test.
     *  @see #testPutRemove()
     *  @see #testPutRemoveWithVeto()
     *  @see #testRemoveDataCollectionListener()
     *  @see mgl3.access.ru.biosoft.access.core.DataCollection#put(mgl3.access.ru.biosoft.access.core.DataElement)
     *  @see mgl3.access.ru.biosoft.access.core.DataCollection#remove(String)
     */
    public void testRemoveDataCollectionListener() throws Exception
    {
        checkDataCollection();

        if( !dataCollection.isMutable() )
            return;

        DataElement de = dataCollection.iterator().next();
        String name = de.getName();

        throwFlag = true;
        resetEvents();
        dataCollection.removeDataCollectionListener(listener);

        dataCollection.remove(name);
        dataCollection.put(de);

        assertNull("listener not removed  eventAdded not null", eventAdded);
        assertNull("listener not removed  eventRemoved not null", eventRemoved);
        assertNull("listener not removed  eventChanged not null", eventChanged);
        assertNull("listener not removed  eventWillAdd not null", eventWillAdd);
        assertNull("listener not removed  eventWillChange  not null", eventWillChange);
        assertNull("listener not removed  eventWillRemove  not null", eventWillRemove);

        resetEvents();
    }

     ///////////////////////////////////////////////////////////////////////////
     // Inner classes

    protected class TestDataCollectionListener implements DataCollectionListener
    {
        @Override
        public void elementAdded(DataCollectionEvent e)
        {
            eventAdded = e;
        }
        @Override
        public void elementChanged(DataCollectionEvent e)
        {
            eventChanged = e;
        }
        @Override
        public void elementRemoved(DataCollectionEvent e)
        {
            eventRemoved = e;
        }
        @Override
        public void elementWillAdd(DataCollectionEvent e) throws DataCollectionVetoException
        {
            eventWillAdd = e;
            if( throwFlag )
                throw new DataCollectionVetoException();

        }
        @Override
        public void elementWillChange(DataCollectionEvent e) throws DataCollectionVetoException
        {
            eventWillChange = e;
            if( throwFlag )
                throw new DataCollectionVetoException();
        }
        @Override
        public void elementWillRemove(DataCollectionEvent e) throws DataCollectionVetoException, Exception
        {
            eventWillRemove = e;
            willRemoveDE = e.getOwner().get(e.getDataElementName());

            if( throwFlag )
                throw new DataCollectionVetoException();
        }
    }

}// end of abstract class DataCollectionTest
