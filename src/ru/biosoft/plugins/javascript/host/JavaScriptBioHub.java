package ru.biosoft.plugins.javascript.host;

import java.util.Arrays;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;

import one.util.streamex.StreamEx;
import ru.biosoft.access.biohub.BioHub;
import ru.biosoft.access.biohub.BioHubRegistry;
import ru.biosoft.access.biohub.BioHubSupport;
import ru.biosoft.access.biohub.ReferenceType;
import ru.biosoft.access.biohub.ReferenceTypeRegistry;
import ru.biosoft.access.biohub.BioHubRegistry.MatchingStep;
import ru.biosoft.plugins.javascript.JSDescription;
import ru.biosoft.plugins.javascript.JavaScriptHostObjectBase;

public class JavaScriptBioHub extends JavaScriptHostObjectBase
{
    public String[] getTypes()
    {
        return ReferenceTypeRegistry.types().map( ReferenceType::getDisplayName ).sorted().toArray( String[]::new );
    }
    
    public String[][] getMatchingPlan(@JSDescription ( "Name of input reference type" ) String inputType,
            @JSDescription ( "Name of output reference type" ) String outputType,
            @JSDescription ( "Latin name of species used during matching") String species )
    {
        if(ReferenceTypeRegistry.optReferenceType(inputType) == null)
            throw new IllegalArgumentException("Invalid input type: "+inputType);
        if(ReferenceTypeRegistry.optReferenceType(outputType) == null)
            throw new IllegalArgumentException("Invalid output type: "+outputType);
        Properties input = BioHubSupport.createProperties( species, inputType );
        Properties output = BioHubSupport.createProperties( species, outputType );
        MatchingStep[] matchingPath = BioHubRegistry.getMatchingPath(input, output);
        if(matchingPath == null) return null;
        return StreamEx.of(matchingPath)
            .map( step -> new String[] {step.getType().getDisplayName(), step.getBioHub().getName(), String.valueOf(step.getLength()), String.valueOf(step.getQuality())} )
            .prepend( new String[] {inputType} )
            .toArray( String[][]::new );
    }
    
    public String[] getReachableTypes(@JSDescription("Name of reference type") String inputType,
            @JSDescription("Latin name of species used during matching") String species)
    {
        if(ReferenceTypeRegistry.optReferenceType(inputType) == null)
            throw new IllegalArgumentException("Invalid input type: "+inputType);
        Properties input = BioHubSupport.createProperties( species, inputType );
        ReferenceType[] types = BioHubRegistry.getReachableTypes(input);
        if(types == null) return null;
        return StreamEx.of(types).map( ReferenceType::getDisplayName ).sorted().toArray( String[]::new );
    }
    
    public String detectType(@JSDescription("Accession number to guess reference type") String inputID)
    {
        return ReferenceTypeRegistry.detectReferenceType(inputID).getDisplayName();
    }
    
    public String[] match(
            @JSDescription ( "Input accession number to match." ) String inputID,
            @JSDescription ( "Name of the reference type of input accession. If omitted, then detectType(accession) will be used to detect it." ) String inputType,
            @JSDescription ( "Name of target reference type." ) String outputType,
            @JSDescription ( "Latin name of species used during matching" ) String species)
    {
        if(ReferenceTypeRegistry.optReferenceType(inputType) == null)
            throw new IllegalArgumentException("Invalid input type: "+inputType);
        if(ReferenceTypeRegistry.optReferenceType(outputType) == null)
            throw new IllegalArgumentException("Invalid output type: "+outputType);
        Properties input = BioHubSupport.createProperties( species, inputType );
        Properties output = BioHubSupport.createProperties( species, outputType );
        Map<String, String[]> references = BioHubRegistry.getReferences(new String[] {inputID}, input, output, null);
        if(references == null) return new String[0];
        return references.get(inputID);
    }

    public String[] match(String inputID, String outputType, String species)
    {
        if(ReferenceTypeRegistry.optReferenceType(outputType) == null)
            throw new IllegalArgumentException("Invalid output type: "+outputType);
        Properties input = BioHubSupport.createProperties( species, ReferenceTypeRegistry.detectReferenceType(inputID) );
        Properties output = BioHubSupport.createProperties( species, outputType );
        Map<String, String[]> references = BioHubRegistry.getReferences(new String[] {inputID}, input, output, null);
        if(references == null) return new String[0];
        return references.get(inputID);
    }

    public String matchDebug(String inputID, String outputType, String species)
    {
        ReferenceType referenceType = ReferenceTypeRegistry.detectReferenceType(inputID);
        return "Detected type: "+referenceType.getDisplayName()+"\n"+matchDebug(inputID, referenceType.getDisplayName(), outputType, species);
    }

    public String matchDebug(
            @JSDescription ( "Input accession number to match." ) String inputID,
            @JSDescription ( "Name of the reference type of input accession. If omitted, then detectType(accession) will be used to detect it." ) String inputType,
            @JSDescription ( "Name of target reference type." ) String outputType,
            @JSDescription ( "Latin name of species used during matching" ) String species)
    {
        StringBuilder result = new StringBuilder();
        ReferenceType input = ReferenceTypeRegistry.optReferenceType(inputType);
        if(input == null) return "Error: input type not found: "+inputType;
        ReferenceType output = ReferenceTypeRegistry.optReferenceType(outputType);
        if(output == null) return "Error: output type not found: "+outputType;
        Properties inputProperties = BioHubSupport.createProperties( species, inputType );
        Properties outputProperties = BioHubSupport.createProperties( species, outputType );
        MatchingStep[] matchingPath = BioHubRegistry.getMatchingPath(inputProperties, outputProperties);
        if(matchingPath == null) return "Error: no matching plan available. Seems that target type is unreachable from source type";
        ReferenceType curType = input;
        Set<String> curResult = new TreeSet<>();
        curResult.add(inputID);
        result.append("["+curType.getDisplayName()+"]: "+String.join(",", curResult)+"\n");
        Properties properties = new Properties();
        properties.put(BioHub.SPECIES_PROPERTY, species);
        for(MatchingStep step: matchingPath)
        {
            result.append("-> Using "+step.getBioHub().getName()+" ("+step.getBioHub().getClass().getSimpleName()+"); q = "+step.getQuality()+"\n");
            Map<String, String[]> references = step.getReferences(curResult.toArray(new String[curResult.size()]), null);
            if(references == null)
            {
                result.append("Null result returned: aborting\n");
                break;
            }
            Set<String> newResult = StreamEx.ofValues( references ).nonNull().flatMap( Arrays::stream ).toCollection( TreeSet::new );
            if(newResult.isEmpty())
            {
                result.append("Empty result returned: aborting\n");
                break;
            }
            curResult = newResult;
            curType = step.getType();
            result.append("["+curType.getDisplayName()+"]: "+String.join(",", curResult)+"\n");
        }
        return result.toString();
    }
}
