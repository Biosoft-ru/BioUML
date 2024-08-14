package ru.biosoft.bsa.server;

import java.util.logging.Level;
import java.util.logging.Logger;

import ru.biosoft.access.ClassLoading;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataCollectionConfigConstants;
import ru.biosoft.access.Entry;
import ru.biosoft.access.support.BeanInfoEntryTransformer;
import ru.biosoft.access.support.TagCommandSupport;
import ru.biosoft.bsa.SlicedTrack;
import ru.biosoft.bsa.Track;
import ru.biosoft.bsa.server.ClientTrack;
import ru.biosoft.bsa.track.GCContentTrack;
import ru.biosoft.bsa.view.TrackViewBuilder;

public class TrackTextTransformer extends BeanInfoEntryTransformer<Track>
{
    protected static final Logger log = Logger.getLogger(TrackTextTransformer.class.getName());

    @Override
    public void initCommands(Class<Track> type)
    {
        super.initCommands(type);

        addCommand(new ServerInfoTagCommand("VB", this));
        addCommand(new SliceLengthTagCommand("SL", this));
    }
    @Override
    public Class<Track> getOutputType()
    {
        return Track.class;
    }

    @Override
    public Track transformInput(Entry input) throws Exception
    {
        // TODO: more robust solution not based on track name
        if(input.getName().equals(GCContentTrack.NAME))
            return new GCContentTrack(getTransformedCollection());
        Track obj = new ClientTrack(getTransformedCollection(), input.getName());
        readObject(obj, input.getReader());
        return obj;
    }
    
    private static class SliceLengthTagCommand extends TagCommandSupport<Track>
    {
        public SliceLengthTagCommand(String tag, TrackTextTransformer transformer)
        {
            super(tag, transformer);
        }

        @Override
        public void addValue(String value)
        {
            ClientTrack track = (ClientTrack)transformer.getProcessedObject();
            track.setSliceLength(Integer.parseInt(value.trim())/10);
        }

        @Override
        public String getTaggedValue()
        {
            Track track = transformer.getProcessedObject();
            if(track instanceof SlicedTrack)
            {
                int sliceLength = ((SlicedTrack)track).getSliceLength();
                return getTag()+"  "+sliceLength;
            }
            return "";
        }
    }

    public static class ServerInfoTagCommand extends TagCommandSupport<Track>
    {
        public ServerInfoTagCommand(String tag, TrackTextTransformer transformer)
        {
            super(tag, transformer);
        }

        @Override
        public void addValue(String value)
        {
            value = value.trim();

            try
            {
                String plugins = transformer.getTransformedCollection().getInfo().getProperty(DataCollectionConfigConstants.PLUGINS_PROPERTY);
                Class<? extends TrackViewBuilder> clazz = ClassLoading.loadSubClass( value, plugins, TrackViewBuilder.class );
                TrackViewBuilder viewBuilder = clazz.newInstance();
                ClientTrack track = (ClientTrack)transformer.getProcessedObject();
                track.setViewBuilder(viewBuilder);
            }
            catch( Exception e )
            {
                log.log(Level.SEVERE, "Can not load track view builder '"+value+"' for track "+((ClientTrack)transformer.getProcessedObject()).getName(), e);
            }
        }

        @Override
        public String getTaggedValue()
        {
            Track track = transformer.getProcessedObject();
            return getTag()+"  "+track.getViewBuilder().getClass().getName();
        }
    }
}
