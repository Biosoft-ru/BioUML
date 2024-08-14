package biouml.standard._test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

import biouml.standard.type.Species;
import ru.biosoft.access._test.AbstractBioUMLTest;
import ru.biosoft.access.core.CollectionFactory;

public class SpeciesTest extends AbstractBioUMLTest
{
    static class MyContainer implements Serializable
    {
        private static final long serialVersionUID = 1L;
        Species species;
    }
    
    public void testSpeciesSerialization() throws Exception
    {
        CollectionFactory.createRepository( "../data" );
        MyContainer myContainer = new MyContainer();
        myContainer.species = Species.getSpecies( "Homo sapiens" );
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream( baos );
        oos.writeObject( myContainer );
        byte[] data = baos.toByteArray();
        ByteArrayInputStream bais = new ByteArrayInputStream( data );
        ObjectInputStream ois = new ObjectInputStream( bais );
        MyContainer myContainer2 = (MyContainer)ois.readObject();
        assertEquals(myContainer.species.getLatinName(), myContainer2.species.getLatinName());
    }
}
