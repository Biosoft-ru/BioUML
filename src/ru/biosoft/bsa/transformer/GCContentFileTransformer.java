package ru.biosoft.bsa.transformer;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.Iterator;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.file.AbstractFileTransformer;
import ru.biosoft.access.file.FileDataElement;
import ru.biosoft.access.support.BeanInfoEntryTransformer;
import ru.biosoft.access.support.SetPropertyCommand;
import ru.biosoft.access.support.TagCommand;
import ru.biosoft.access.support.TagEntryTransformer;
import ru.biosoft.bsa.track.GCContentTrack;

public class GCContentFileTransformer extends AbstractFileTransformer<GCContentTrack>
{

    private BeanInfoEntryTransformer<GCContentTrack> primaryTransformer = new BeanInfoEntryTransformer<GCContentTrack>()
    {
        @Override
        public Class getOutputType()
        {
            return GCContentTrack.class;
        }

        @Override
        public void init(DataCollection primaryCollection, DataCollection transformedCollection)
        {
            super.init( primaryCollection, transformedCollection );
            Iterator<TagCommand> i = commands.values().iterator();
            while ( i.hasNext() )
            {
                ((SetPropertyCommand) i.next()).setIndent( 6 );
            }
        }
    };

    @Override
    public void init(DataCollection<FileDataElement> primaryCollection, DataCollection<GCContentTrack> transformedCollection)
    {
        super.init( primaryCollection, transformedCollection );
        primaryTransformer.init( primaryCollection, transformedCollection );
    }

    @Override
    public Class<? extends GCContentTrack> getOutputType()
    {
        return GCContentTrack.class;
    }

    @Override
    public GCContentTrack load(File input, String name, DataCollection<GCContentTrack> origin) throws Exception
    {
        GCContentTrack track = new GCContentTrack( name, origin );
        try (FileReader reader = new FileReader( input ))
        {
            primaryTransformer.readObject( track, reader );
        }
        return track;
    }

    @Override
    public void save(File output, GCContentTrack element) throws Exception
    {
        try (FileWriter fw = new FileWriter( output ))
        {
            fw.write( primaryTransformer.getStartTag() + "    " + element.getName() + TagEntryTransformer.endl );
            primaryTransformer.writeObject( element, fw );
        }

    }

}
