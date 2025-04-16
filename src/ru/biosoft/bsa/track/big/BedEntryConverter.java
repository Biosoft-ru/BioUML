package ru.biosoft.bsa.track.big;

import ru.biosoft.bigbed.BedEntry;

//converter should fetch chromInfo and chromMapping from BigBedTrack
public interface BedEntryConverter<T>
{
    static final String PROP_PREFIX = "BigBedConverter.";
    T fromBedEntry(BedEntry e);
    BedEntry toBedEntry(T t);
}
