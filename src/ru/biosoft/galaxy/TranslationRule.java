package ru.biosoft.galaxy;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

public class TranslationRule
{
    private final String remoteName;
    private final String localName;
    private final String defaultValue;
    private String appendSeparator, appendStart, appendJoin;
    private final Map<String, String> translationMap = new HashMap<>();
    private final Map<String, String> appendParamMap = new HashMap<>();
    
    public TranslationRule(String remoteName, String localName, String defaultValue)
    {
        super();
        this.remoteName = remoteName;
        this.localName = localName;
        this.defaultValue = defaultValue;
    }
    
    public void setAppendOptions(String appendSeparator, String appendStart, String appendJoin)
    {
        this.appendSeparator = appendSeparator;
        this.appendJoin = appendJoin;
        this.appendStart = appendStart;
    }

    public String getRemoteName()
    {
        return remoteName;
    }

    public String getLocalName()
    {
        return localName;
    }
    
    public void addAppendParameter(String name, String missingValue)
    {
        appendParamMap.put(name, missingValue);
    }

    public void addTranslation(String from, String to)
    {
        translationMap.put(from, to);
    }
    
    public String translate(String value, Map<String, String> params)
    {
        StringBuilder result = new StringBuilder();
        if(value == null) result.append(defaultValue);
        else if(translationMap.containsKey(value)) result.append(translationMap.get(value));
        else result.append(value);
        if(!appendParamMap.isEmpty())
        {
            for(Entry<String, String> entry: appendParamMap.entrySet())
            {
                String appendValue = params.getOrDefault(entry.getKey(), entry.getValue());
                result.append( ( result.toString().contains(appendStart) ? appendSeparator : appendStart )).append(entry.getKey())
                        .append(appendJoin).append(appendValue);
            }
        }
        return result.toString();
    }
}