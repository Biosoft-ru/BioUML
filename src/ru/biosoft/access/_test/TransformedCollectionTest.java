package ru.biosoft.access._test;

import java.io.BufferedReader;
import java.util.Iterator;

import com.developmentontheedge.application.ApplicationUtils;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataCollectionEvent;
import ru.biosoft.access.core.DataCollectionInfo;
import ru.biosoft.access.core.DataCollectionListener;
import ru.biosoft.access.core.DataCollectionVetoException;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.Entry;
import ru.biosoft.access.core.TransformedDataCollection;
import ru.biosoft.access.core.Transformer;


// TODO: High should extend DataCollectionTest
abstract public class TransformedCollectionTest extends FileEntryCollectionTest
{
    /** Same instance of collection that contained in TransformedDataCollection.  */
    protected DataCollection aggCollection = null;

    /** Listener of aggregated collection events. */
    ru.biosoft.access.core.DataCollectionListener aggListener = null;

    /** Events that set in aggregated collection event listener */
    DataCollectionEvent eventAggAdded      = null;
    DataCollectionEvent eventAggRemoved    = null;
    DataCollectionEvent eventAggChanged    = null;
    DataCollectionEvent eventAggWillAdd    = null;
    DataCollectionEvent eventAggWillChange = null;
    DataCollectionEvent eventAggWillRemove = null;
    DataElement willRemoveAggDE;

    /**
     * Returns output class of transformed ru.biosoft.access.core.DataElement
     * <br>Should be overridden in derived collection.
     *
     * @return output class of transformed ru.biosoft.access.core.DataElement
     */
    abstract public Class getOutputClass();

    /**
     * Returns input class of transformed ru.biosoft.access.core.DataElement
     * <br>Should be overridden in derived collection.
     * @return input class of transformed ru.biosoft.access.core.DataElement
     */
    abstract public Class getInputClass();

    /**
     * Clear all events data
     */
    @Override
    protected void resetEvents()
    {
        super.resetEvents();
        eventAggAdded        = null;
        eventAggRemoved      = null;
        eventAggChanged      = null;
        eventAggWillAdd      = null;
        eventAggWillChange   = null;
        eventAggWillRemove   = null;
        willRemoveAggDE = null;
    }


    protected void resetAggDataCollection() throws Exception
    {
        if ( aggCollection != null)
        {
            if (aggListener != null)
            {
                aggCollection.removeDataCollectionListener(aggListener);
            }
            aggCollection.close();
            aggCollection = null;
        }
    }

    public void setAggDataCollection( DataCollection aggCollection )
    {
        this.aggCollection = aggCollection;
        if ( aggCollection!=null )
            aggCollection.addDataCollectionListener(aggListener = new ru.biosoft.access.core.DataCollectionListener()
                  {
                      @Override
                    public void elementAdded(DataCollectionEvent e)
                      {
                          eventAggAdded   = e;
                      }
                      @Override
                    public void elementChanged(DataCollectionEvent e)
                      {
                          eventAggChanged = e;
                      }
                      @Override
                    public void elementRemoved(DataCollectionEvent e)
                      {
                          eventAggRemoved = e;
                      }
                      @Override
                    public void elementWillAdd(DataCollectionEvent e)    throws DataCollectionVetoException
                      {
                          eventAggWillAdd = e;
                          if (throwFlag)
                              throw new DataCollectionVetoException();

                      }
                      @Override
                    public void elementWillChange(DataCollectionEvent e) throws DataCollectionVetoException
                      {
                          eventAggWillChange = e;
                          if (throwFlag)
                              throw new DataCollectionVetoException();
                      }
                      @Override
                    public void elementWillRemove(DataCollectionEvent e) throws DataCollectionVetoException, Exception
                      {
                          eventAggWillRemove = e;
                          willRemoveAggDE = e.getOwner().get( e.getDataElementName() );
                          if (throwFlag)
                              throw new DataCollectionVetoException();
                      }
                  });
    }

    @Override
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

            //System.out.println("added="+added);
            //System.out.println("de="+de);

            assertSame("ru.biosoft.access.core.DataCollectionListener.elementAdded()  ["+added+"] has wrong argument."+
                       " Should be: "+de+". "
                       ,added,de);
        }
        assertNull("ru.biosoft.access.core.DataCollectionListener.elementRemoved() called after adding",eventRemoved);
        //assertNull("ru.biosoft.access.core.DataCollectionListener.elementChanged() called after adding",eventChanged);
        assertNull("ru.biosoft.access.core.DataCollectionListener.elementWillRemove() called before adding",eventWillRemove);
        //assertNull("ru.biosoft.access.core.DataCollectionListener.elementWillChange() called before adding",eventWillChange);

        if ( !throwFlag )
            assertTrue( "Wrong type of eventAdded data. Should be "+getOutputClass()+", really "+eventAdded.getOwner().get( eventAdded.getDataElementName() ).getClass(),
                    getOutputClass().isAssignableFrom(eventAdded.getOwner().get( eventAdded.getDataElementName() ).getClass()) );

        if ( throwFlag )
            assertNull("Aggregated ru.biosoft.access.core.DataCollectionListener.elementAdded() called after adding with veto" ,eventAggAdded );
        else
        {
            assertNotNull("Aggregated ru.biosoft.access.core.DataCollectionListener.elementAdded() not called after adding" ,eventAggAdded );
        }
        assertNull("Aggregated ru.biosoft.access.core.DataCollectionListener.elementRemoved() called after adding"   ,eventAggRemoved );
        assertNull("Aggregated ru.biosoft.access.core.DataCollectionListener.elementChanged() called after adding"   ,eventAggChanged );

        if ( throwFlag )
        {
            //assertNull("Aggregated ru.biosoft.access.core.DataCollectionListener.elementWillAdd() not called when adding" , eventAggWillAdd );
        } else
        {
            assertNotNull("Aggregated ru.biosoft.access.core.DataCollectionListener.elementWillAdd() not called when adding",eventAggWillAdd );
            assertTrue( "Wrong type of aggregated eventWillAdd data. Should be "+getInputClass()+", really "+eventAggWillAdd.getOwner().get( eventAggWillAdd.getDataElementName() ).getClass(),
                    getInputClass().isAssignableFrom(eventAggWillAdd.getOwner().get( eventAggWillAdd.getDataElementName() ).getClass()) );
        }
        assertNull("Aggregated ru.biosoft.access.core.DataCollectionListener.elementWillRemove() called before adding",    eventAggWillRemove );
        assertNull("Aggregated ru.biosoft.access.core.DataCollectionListener.elementWillChange() called before adding" ,   eventAggWillChange );
    }

    @Override
    protected void assertChangeEvents(DataElement  de) throws Exception
    {
        if ( throwFlag )
            assertNull("ru.biosoft.access.core.DataCollectionListener.elementChanged() called after changing with veto" ,eventChanged );
        else
        {
            assertNotNull("ru.biosoft.access.core.DataCollectionListener.elementChanged() not called after changing" ,eventChanged );
            DataElement changed = eventChanged.getOwner().get( eventChanged.getDataElementName() );

            //System.out.println("changed="+changed);
            //System.out.println("de="+de);

            assertSame("ru.biosoft.access.core.DataCollectionListener.elementChanged()  ["+changed+"] has wrong argument. Should be: "+de
                       ,changed,de);
        }
        assertNull("ru.biosoft.access.core.DataCollectionListener.elementRemoved() called after changing",eventRemoved);
        assertNull("ru.biosoft.access.core.DataCollectionListener.elementChanged() called after changing",eventAdded);
        assertNull("ru.biosoft.access.core.DataCollectionListener.elementWillAdd()  called when changing",eventWillAdd);
        assertNull("ru.biosoft.access.core.DataCollectionListener.elementWillRemove() called before changing",eventWillRemove);
    }

    @Override
    protected void assertRemoveEvents(DataElement de) throws Exception
    {
        if ( !throwFlag )
        {
            assertNotNull("ru.biosoft.access.core.DataCollectionListener.elementRemoved() not called after removing", eventRemoved );
            assertNotNull("eventRemoved hasn't data element.", eventRemoved.getDataElementName());
            assertNotNull("Aggregated ru.biosoft.access.core.DataCollectionListener.elementRemoved() not called after removing", eventAggRemoved );
            assertNotNull("Aggregated ru.biosoft.access.core.DataCollectionListener.elementWillRemove() not called before removing",eventAggWillRemove);

            assertTrue( "Wrong type of eventWillRemove data. Should be "+getOutputClass()+", really "+willRemoveDE.getClass(),
                    getOutputClass().isAssignableFrom( willRemoveDE.getClass() ) );
            //assertTrue( "Wrong type of eventRemoved data. Should be "+getOutputClass()+", really "+eventRemoved.getOwner().get( eventRemoved.getDataElementName() ).getClass(),
            //        getOutputClass().isAssignableFrom(eventRemoved.getOwner().get( eventRemoved.getDataElementName() ).getClass()) );
            assertTrue( "Wrong type of aggregated eventWillRemove data. Should be "+getInputClass()+", really "+willRemoveAggDE.getClass(),
                    getInputClass().isAssignableFrom( willRemoveAggDE.getClass() ) );
        }
        else
        {
            assertNull("Aggregated ru.biosoft.access.core.DataCollectionListener.elementRemoved() called after removing with veto",eventAggRemoved );
            //assertNotNull("Aggregated ru.biosoft.access.core.DataCollectionListener.elementWillRemove() not called before removing with veto",eventAggWillRemove);
            assertNull("Aggregated ru.biosoft.access.core.DataCollectionListener.elementWillRemove() called before removing with veto",eventAggWillRemove);
        }
        assertNull("Aggregated ru.biosoft.access.core.DataCollectionListener.elementAdded() called after removing" ,   eventAggAdded      );
        assertNull("Aggregated ru.biosoft.access.core.DataCollectionListener.elementChanged() called after removing" , eventAggChanged    );
        assertNull("Aggregated ru.biosoft.access.core.DataCollectionListener.elementWillAdd() called in removing" ,    eventAggWillAdd    );
        assertNull("Aggregated ru.biosoft.access.core.DataCollectionListener.elementWillChange() called in removing" , eventAggWillChange );
    }

    /**
     * @PENDING  ClassCastException is unfounded thrown
     *
     * @exception Exception
     */
    public void testTransformer() throws Exception
    {
        try
        {
            assertTrue("getOriginalSize() should not return 0",         getOriginalSize()>0 );
            assertTrue("DataCollection should not be empty",            dataCollection.getSize()>0 );
            assertTrue("Aggregated DataCollection should not be empty", aggCollection.getSize()>0 );

            Transformer transformer = ((TransformedDataCollection)dataCollection).getTransformer();

            DataElement  d0         = (DataElement)aggCollection.iterator().next();
            DataElement di1         = transformer.transformInput ( d0  );
            DataElement do1         = transformer.transformOutput( di1 );
            DataElement di2         = transformer.transformInput ( do1 );

            DataCollectionInfo aggInfo = aggCollection.getInfo();
            if( aggInfo==null || aggInfo.getQuerySystem()==null )
                compare(di1,di2);
        } catch (RuntimeException ex)
        {
            if ( !( ex.getCause() instanceof ClassCastException) )
                throw ex;
        }
    }


    /**
     * Method should be overrided in derived collection.
     * It compares two data elements. If data elements are not identical,derived
     * method should show assertion.
     * @param de1 first ru.biosoft.access.core.DataElement for compare
     * @param de2 second ru.biosoft.access.core.DataElement for compare
     * @exception Exception any errors
     */
    @Override
    public abstract void compare(DataElement de1,ru.biosoft.access.core.DataElement de2) throws Exception;


    /**
     * Checks whether tested data collections are initialized
     */
    public void testIdentity()
    {
        assertNotNull( "dataCollection not setted.",dataCollection );
        assertNotNull( "aggregated dataCollection not setted.",aggCollection );
    }

    /**
     * Destroy all created data collections.
     * @see CollectionFactoryUtils.access.CollectionFactory#destroyCollection(mgl3.access.ru.biosoft.access.core.DataCollection)
     */
    @Override
    protected void tearDown() throws Exception
    {
        resetDataCollection();
        resetAggDataCollection();
    }

    protected void resetDataCollection() throws Exception
    {
        if( dataCollection != null )
        {
            if (listener != null)
            {
                dataCollection.removeDataCollectionListener( listener );
            }
            dataCollection.close();
            dataCollection = null;
        }
    }



    /**
     * Compares output result of current transformer with correct file sample
     * @param name name of ru.biosoft.access.core.DataElement
     * @param origFileName expected right sample file path
     * @exception Exception any errors
     */
    public void checkTransformer(String name, String origFileName) throws Exception
    {
        assertNotNull( "Data collection is not created", dataCollection );

        Iterator<DataElement> it = dataCollection.iterator();
        assertNotNull( "Data collection doesn't provide a valid iterator", it );
        assertTrue( "Data collection is unexpectedly empty", it.hasNext() );
        DataElement  d1         = it.next();
        Transformer<ru.biosoft.access.core.DataElement, DataElement> transformer = ((TransformedDataCollection<ru.biosoft.access.core.DataElement, DataElement>)dataCollection).getTransformer();

        DataElement  d0         = aggCollection.get(name);
        assertNotNull("Object is null: "+name, d0);

        DataElement di1         = transformer.transformInput ( d0  );
        DataElement do1         = transformer.transformOutput( di1 );

        try (BufferedReader br0 = new BufferedReader( ( (Entry)do1 ).getReader() );
                BufferedReader br1 = ApplicationUtils.utfReader( origFileName ))
        {
            String str = null;
            int numb = 0;
            while( ( str = br0.readLine() ) != null )
            {
                numb++;
                String str1 = br1.readLine();
                if( str1 != null ) str1 = str1.trim();
                assertEquals( "file: <" + origFileName + ">, line " + numb, str1, str.trim() );
            }
        }
    }

    public DataCollection getAggCollection()
    {
        return aggCollection;

    }

    public void setAggCollection(DataCollection collection)
    {
        aggCollection  = collection;
    }

    public DataCollection getDataCollection()
    {
        return dataCollection;

    }

//    public void setDataCollection(DataCollection collection)
//    {
//        dataCollection  = collection;
//    }

}
