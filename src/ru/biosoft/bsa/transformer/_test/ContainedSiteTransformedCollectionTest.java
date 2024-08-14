package ru.biosoft.bsa.transformer._test;

import java.util.Iterator;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataCollectionInfo;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.Index;
import ru.biosoft.access.core.QuerySystem;
import ru.biosoft.access.core.TransformedDataCollection;
import ru.biosoft.access.core.Transformer;
import ru.biosoft.access._test.TransformedCollectionTest;
import ru.biosoft.bsa.AnnotatedSequence;
import ru.biosoft.bsa.Sequence;
import ru.biosoft.bsa.Site;
import ru.biosoft.bsa.Track;
import ru.biosoft.bsa.transformer.SequenceQuerySystem;

import com.developmentontheedge.beans.DynamicPropertySet;

/** @todo Document */
public abstract class ContainedSiteTransformedCollectionTest extends TransformedCollectionTest
{
    protected void checkProperties(DynamicPropertySet properties1, DynamicPropertySet properties2)
    {
        if( properties1 == null && properties2 == null )
            return;
        assertNotNull("Properties1 should not be null", properties1);
        assertNotNull("Properties2 should not be null", properties2);

        Iterator<String> it1 = properties1.nameIterator();
        // properties test
        while( it1.hasNext() )
        {
            String k = it1.next();
            String v1 = properties1.getValueAsString(k);
            String v2 = properties2.getValueAsString(k);
            assertNotNull("Unknown proprerty", v2);
            assertEquals(v1, v2);
        }
    }

    @Override
    public void testTransformer() throws Exception
    {
        try
        {
            assertTrue("getOriginalSize() should not return 0", getOriginalSize() > 0);
            assertTrue("DataCollection should not be empty", dataCollection.getSize() > 0);
            assertTrue("Aggregated DataCollection should not be empty", aggCollection.getSize() > 0);

            Transformer transformer = ( (TransformedDataCollection)dataCollection ).getTransformer();
            DataElement d0 = (DataElement)aggCollection.iterator().next();

            // we remove index first:
            QuerySystem querySystem = dataCollection.getInfo().getQuerySystem();
            Index index = querySystem.getIndex(SequenceQuerySystem.SEQUENCE_INDEX);

            index.remove(d0.getName());
            DataElement di1 = transformer.transformInput(d0);

            DataElement do1 = transformer.transformOutput(di1);

            index.remove(d0.getName());
            DataElement di2 = transformer.transformInput(do1);
            index.remove(d0.getName());

            DataCollectionInfo aggInfo = aggCollection.getInfo();
            if( aggInfo == null || aggInfo.getQuerySystem() == null )
                compare(di1, di2);
        }
        catch( RuntimeException ex )
        {
            if( ! ( ex.getCause() instanceof ClassCastException ) )
                throw ex;
        }
    }

    @Override
    public void compare(DataElement di1, DataElement di2) throws Exception
    {
        AnnotatedSequence ss1 = (AnnotatedSequence)di1;
        AnnotatedSequence ss2 = (AnnotatedSequence)di2;
        assertEquals("Maps has different sizes.", ss1.getSize(), ss2.getSize());
        checkProperties(ss1.getProperties(), ss2.getProperties());
        Iterator<Track> it = ss1.iterator();
        while( it.hasNext() )
        {
            Track track1 = it.next();
            String name = track1.getName();
            Track track2 = ss2.get(name);

            Iterator<Site> it2 = track1.getAllSites().iterator();
            DataCollection<Site> track2Sites = track2.getAllSites();
            while( it2.hasNext() )
            {
                Site site1 = it2.next();
                String name2 = site1.getName();
                Site site2 = track2Sites.get(name2);

                assertNotNull("site with name <" + name + "> not found in di1.", site1);
                assertNotNull("site with name <" + name + "> not found in di2.", site2);

                assertEquals("Wrong start in " + name, site1.getStart(), site2.getStart());
                assertEquals("Wrong length", site1.getLength(), site2.getLength());
                assertEquals("Wrong precision", site1.getPrecision(), site2.getPrecision());
                assertEquals("Wrong strand", site1.getStrand(), site2.getStrand());
                assertEquals("Wrong type", site1.getType(), site2.getType());
                checkProperties(site1.getProperties(), site2.getProperties());
            }
        }

        // check of sequences
        Sequence seq1 = ss1.getSequence();
        Sequence seq2 = ss2.getSequence();

        assertEquals(seq1.isCircular(), seq2.isCircular());
        assertEquals(seq1.getLength(), seq2.getLength());

        for( int i = 1; i <= seq1.getLength(); i++ )
        {
            assertEquals(seq1.getLetterAt(i), seq2.getLetterAt(i));
        }
    }
}
