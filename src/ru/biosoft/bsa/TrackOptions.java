package ru.biosoft.bsa;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;

import com.developmentontheedge.beans.annot.PropertyDescription;
import com.developmentontheedge.beans.annot.PropertyName;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.file.GenericFileDataCollection;

public class TrackOptions
{
    private Track track;
    private GenomeSelector genomeSelector;
    private ChrNameMapping chrMapping;
    protected ChrCache chrCache;

    public TrackOptions(Track track, Properties properties)
    {
        this.track = track;
        chrMapping = ChrNameMapping.getMapping(properties);
        DataElementPath seqBase = DataElementPath.create(properties.getProperty(Track.SEQUENCES_COLLECTION_PROPERTY));
        chrCache = new ChrCache(seqBase);
        setGenomeSelector(new GenomeSelector(this.track));
    }

    public void setChrNameMapping(ChrNameMapping chrNameMapping)
    {
        this.chrMapping = chrNameMapping;
        chrCache.clear();
    }

    public String internalToExternal(String chr)
    {
        if( chrMapping == null )
            return chr;
        String res = chrMapping.srcToDst(chr);
        if( res == null )
            res = chr;
        return res;
    }

    public String externalToInternalName(String chr)
    {
        if( chrMapping == null )
            return chr;
        String res = chrMapping.dstToSrc(chr);
        if( res == null )
            res = chr;
        return res;
    }

    public Sequence getChromosomeSequence(String chrName)
    {
        return chrCache.getSequence(chrName);
    }

    //Properties for BeanInfo editor
    @PropertyName("Chromosome name mapping")
    @PropertyDescription("Chromosome name mapping")
    public String getChrMapping()
    {
        if( chrMapping == null )
            return ChrNameMapping.NONE_MAPPING;
        return chrMapping.getName();
    }

    public void setChrMapping(String chrNameMappingStr)
    {
        if( chrMapping == null || !chrMapping.getName().equals(chrNameMappingStr) )
        {
            setChrNameMapping(ChrNameMapping.getMapping(chrNameMappingStr));
            //TODO ?: update parent collection with new value
            //            if( chrMapping != null )
            //                TrackUtils.addTrackProperty(this, ChrNameMapping.PROP_CHR_MAPPING, chrNameMappingStr);
            //            else
            //                TrackUtils.addTrackProperty(this, ChrNameMapping.PROP_CHR_MAPPING, null);
        }
    }

    @PropertyName("Genome (sequences collection)")
    @PropertyDescription("Genome (sequences collection)")
    public GenomeSelector getGenomeSelector()
    {
        return genomeSelector;
    }

    public void setGenomeSelector(GenomeSelector genomeSelector)
    {
        if( genomeSelector != this.genomeSelector )
        {
            //TODO ?: use listener
            //            GenomeSelectorTrackParentUpdater listener = new GenomeSelectorTrackParentUpdater(track, genomeSelector, () -> {
            //                chrCache.clear();
            //                DataElementPath seqBase = TrackUtils.getTrackSequencesPath(track);
            //                chrCache.setSeqBase(seqBase);
            //                return;
            //            });
            //            genomeSelector.addPropertyChangeListener(listener);
        }
        this.genomeSelector = genomeSelector;
    }

    public static class GenomeSelectorTrackParentUpdater implements PropertyChangeListener
    {
        private final Track element;
        private final GenomeSelector genomeSelector;
        private final Runnable action;

        public GenomeSelectorTrackParentUpdater(Track element, GenomeSelector genomeSelector, Runnable action)
        {
            this.element = element;
            this.genomeSelector = genomeSelector;
            this.action = action;
        }

        @Override
        public void propertyChange(PropertyChangeEvent evt)
        {
            String newValue = evt.getNewValue() == null ? null : evt.getNewValue().toString();

            Map<String, String> newProps = new HashMap<String, String>();
            if( "*".equals(evt.getPropertyName()) )
            {

                DataElementPath collectionPath = genomeSelector.getSequenceCollectionPath();

                newProps.put(Track.SEQUENCES_COLLECTION_PROPERTY, collectionPath == null ? null : collectionPath.toString());
                //TrackUtils.addTrackProperty(element, Track.SEQUENCES_COLLECTION_PROPERTY, collectionPath == null ? null : collectionPath.toString());
                action.run();
                String genomeId = genomeSelector.getGenomeId();
                if( genomeId != null )
                    newProps.put(Track.GENOME_ID_PROPERTY, genomeId);
                //                    TrackUtils.addTrackProperty(element, Track.GENOME_ID_PROPERTY, genomeId);
            }
            else if( "sequencePath".equals(evt.getPropertyName()) )
            {
                //TrackUtils.addTrackProperty(element, Track.SEQUENCES_COLLECTION_PROPERTY, newValue);
                newProps.put(Track.SEQUENCES_COLLECTION_PROPERTY, newValue);
                action.run();
            }
            else if( "genomeId".equals(evt.getPropertyName()) )
            {
                //TrackUtils.addTrackProperty(element, Track.GENOME_ID_PROPERTY, newValue);
                newProps.put(Track.GENOME_ID_PROPERTY, newValue);
            }

            if( !newProps.isEmpty() )
            {
                DataCollection<?> parent = element.getOrigin();
                GenericFileDataCollection fdc = null;
                if( parent instanceof GenericFileDataCollection )
                    fdc = (GenericFileDataCollection) parent;
                if( fdc != null )
                {
                    //@todo rename
                    Map<String, Object> fi = fdc.getFileInfo(element.getName());
                    Map<String, String> fiProps = fi == null ? new LinkedHashMap<>() : (Map<String, String>) fi.get("properties");
                    fiProps.putAll(newProps);
                    Properties properties = new Properties();
                    properties.putAll(fiProps);
                    try
                    {
                        fdc.storeElementProperties( element, null, properties );
                    }
                    catch (Exception e)
                    {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }

                }
            }
        }
    }
}
