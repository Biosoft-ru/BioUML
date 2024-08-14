package ru.biosoft.bsa;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

import one.util.streamex.StreamEx;

import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.core.DataElementPathSet;

public class BindingElement implements Iterable<TranscriptionFactor>
{
    private final String name;
    private List<TranscriptionFactor> factors;
    private DataElementPathSet factorPaths;
    private String[] factorNames;
    public BindingElement(String name, Collection<? extends TranscriptionFactor> factors)
    {
        this.name = name;
        this.factors = new ArrayList<>(factors);
    }
    
    public BindingElement(String name, DataElementPathSet factors)
    {
        this.name = name;
        this.factorPaths = factors;
    }
    
    public String getName()
    {
        return name;
    }
    
    private synchronized StreamEx<TranscriptionFactor> factors()
    {
        if(factors == null)
        {
            factors = factorPaths.stream().map( DataElementPath::optDataElement ).filter( TranscriptionFactor.class::isInstance )
                    .map( tf -> (TranscriptionFactor)tf ).collect( Collectors.toList() );
        }
        return StreamEx.of(factors);
    }
    
    public TranscriptionFactor[] getFactors()
    {
        return factors().toArray( TranscriptionFactor[]::new );
    }
    
    private String getFactorName(TranscriptionFactor factor)
    {
        String taxon = factor.getSpeciesName();
        if( taxon != null )
            return factor.getDisplayName() + "/" + taxon + " (" + factor.getName() + ")";
        return factor.getDisplayName() + " (" + factor.getName() + ")";
    }
    
    public String[] getFactorNames()
    {
        if(factorNames == null)
        {
            factorNames = factors().map( this::getFactorName ).toArray( String[]::new );
        }
        return factorNames;
    }
    
    @Override
    public Iterator<TranscriptionFactor> iterator()
    {
        return factors().iterator();
    }
    
    @Override
    public String toString()
    {
        return getName();
    }
}

