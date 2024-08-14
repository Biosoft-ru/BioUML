package ru.biosoft.bsa;

import ru.biosoft.exception.LoggedException;

/**
 * Interface to Track where you can add sites
 * @see SqlTrack - known implementation
 * @author lan
 */
public interface WritableTrack extends Track
{
    /**
     * Add site to the track
     * By current agreement site name should represent sequence name (name of bsa.AnnotatedSequence object which corresponds to the sequence)
     */
    public void addSite(Site site) throws LoggedException;
    
    /**
     * Should be called after adding all sites, so track can flush changes into permanent storage
     */
    public void finalizeAddition() throws LoggedException;
}
