package biouml.plugins.ensembl.biohub;


import ru.biosoft.access.biohub.ReferenceType;
import ru.biosoft.access.biohub.ReferenceTypeRegistry;
import ru.biosoft.util.TextUtil;

public class TypeRecord
{
    private ReferenceType type;
    private ExternalDBRestriction externalDBRestrition;
    private String ensemblTypeName;
    private boolean useLabel;
    private double quality;
    private String stripRegExp = "";
    private String toRegExp = "";
    private String versions;    // Ensembl versions which support this type record (* = all, -59 = up to 59, 60- = from 60)

    
    public TypeRecord(String versions, Class<? extends ReferenceType> typeClass, ExternalDBRestriction externalDBRestriction, double quality, boolean useLabel, String stripRegExp)
    {
        type = ReferenceTypeRegistry.getReferenceType(typeClass);
        this.versions = versions;
        this.externalDBRestrition = externalDBRestriction;
        this.useLabel = useLabel;
        this.stripRegExp = stripRegExp;
        if(this.stripRegExp != null && this.stripRegExp.contains("/"))
        {
            String[] fields = TextUtil.split( this.stripRegExp, '/' );
            this.stripRegExp = fields[0];
            this.toRegExp = fields[1];
        }
        this.quality = quality;
    }
    
    public TypeRecord(String versions, Class<? extends ReferenceType> typeClass, String ensemblTypeName, double quality, boolean useLabel, String stripRegExp)
    {
        this( versions, typeClass, new ExternalDBRestriction().setDefaultDBName( ensemblTypeName ), quality, useLabel, stripRegExp );
    }

    public TypeRecord(String versions, Class<? extends ReferenceType> typeClass, String ensemblTypeName, double quality, boolean useLabel)
    {
        this(versions, typeClass, ensemblTypeName, quality, useLabel, null);
    }

    public TypeRecord(String versions, Class<? extends ReferenceType> typeClass, String ensemblTypeName, double quality)
    {
        this(versions, typeClass, ensemblTypeName, quality, false);
    }

    public TypeRecord(String versions, Class<? extends ReferenceType> typeClass, String ensemblTypeName)
    {
        this(versions, typeClass, ensemblTypeName, 1);
    }

    public TypeRecord(String versions, Class<? extends ReferenceType> typeClass, String ensemblTypeName, String stripRegExp)
    {
        this(versions, typeClass, ensemblTypeName, 1, false, stripRegExp);
    }

    public ReferenceType getType()
    {
        return type;
    }

    public boolean isLabelUsed()
    {
        return useLabel;
    }

    public String getEnsemblTypeName()
    {
        return ensemblTypeName;
    }

    public String getXrefColumnName()
    {
        return ( isLabelUsed() ? "x.display_label" : "x.dbprimary_acc" );
    }

    public String getDbRestrictionClause(String species)
    {
        return "x.external_db_id IN (" + getDbQuery( species ) + ")";
    }

    public String getDbQuery(String species)
    {
        return externalDBRestrition.getExternalDBQuery( species );
    }

    public String getRestrictionClause(String species)
    {
        return getXrefColumnName() + "=? AND " + getDbRestrictionClause(species);
    }

    public double getQuality()
    {
        return quality;
    }

    public String strip(String id)
    {
        if( stripRegExp != null )
        {
            return id.replaceFirst(stripRegExp, toRegExp);
        }
        return id;
    }
    
    public boolean isVersionSupported(int version)
    {
        if(versions.equals("*")) return true;
        if(versions.startsWith("-")) return version <= Integer.parseInt(versions.substring(1));
        if(versions.endsWith("-")) return version >= Integer.parseInt(versions.substring(0, versions.length()-1));
        return false;
    }
    
    @Override
    public String toString()
    {
        return type.toString() + "/" + externalDBRestrition.toString();
    }
}