package ru.biosoft.bsa.track.big;

import org.jetbrains.bio.big.BedEntry;

public interface BedEntryConverter<T>
{
    static final String PROP_PREFIX = "BigBedConverter.";
    T fromBedEntry(BedEntry e);
    BedEntry toBedEntry(T t);
}
