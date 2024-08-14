package biouml.plugins.ensembl.biohub;

import java.util.Properties;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.biohub.ReferenceType;
import ru.biosoft.access.biohub.ReferenceTypeRegistry;
import biouml.model.Module;
import biouml.plugins.ensembl.EnsemblConstants;
import biouml.plugins.ensembl.tabletype.AffymetrixProbeTableType;
import biouml.plugins.ensembl.tabletype.AgilentProbeTableType;
import biouml.plugins.ensembl.tabletype.EnsemblTranscriptTableType;


public class ProbeToEnsemblTranscriptHub extends ExternalToEnsemblHub
{
    private static final String SQL_TRANSCRIPT = "SELECT DISTINCT ts.stable_id,sr.name FROM xref x "
            + "JOIN object_xref o USING(xref_id) "
            + "JOIN transcript_stable_id ts ON(ts.transcript_id=o.ensembl_id) "
            + "JOIN transcript t ON (ts.transcript_id=t.transcript_id)"
            + "JOIN seq_region sr ON(t.seq_region_id=sr.seq_region_id) "
            + "WHERE ensembl_object_type='Transcript' AND ";

    private static final String SQL_PROBE = "SELECT x.dbprimary_acc,sr.name FROM #FUNC_GEN#.probe "
            + "JOIN #FUNC_GEN#.object_xref o ON(ensembl_id=probe_id) "
            + "JOIN #FUNC_GEN#.xref x USING(xref_id) "
            + "JOIN transcript_stable_id ts ON(ts.stable_id=x.dbprimary_acc) "
            + "JOIN transcript t ON (ts.transcript_id=t.transcript_id)"
            + "JOIN seq_region sr ON(t.seq_region_id=sr.seq_region_id) "
            + "WHERE ensembl_object_type='Probe' AND probe.name=?";

    private static final String SQL_PROBE_SET = "SELECT x.dbprimary_acc,sr.name FROM #FUNC_GEN#.probe_set "
            + "JOIN #FUNC_GEN#.object_xref o ON(ensembl_id=probe_set_id) "
            + "JOIN #FUNC_GEN#.xref x USING(xref_id) "
            + "JOIN transcript_stable_id ts ON(ts.stable_id=x.dbprimary_acc) "
            + "JOIN transcript t ON (ts.transcript_id=t.transcript_id)"
            + "JOIN seq_region sr ON(t.seq_region_id=sr.seq_region_id) "
            + "WHERE ensembl_object_type='ProbeSet' AND probe_set.name=?";

    private final TypeRecord[] supportedTypeRecords = {new TypeRecord("*", AffymetrixProbeTableType.class, "AFFY%"),
            new TypeRecord("*", AgilentProbeTableType.class, "AgilentProbe"),
    //new TypeRecord(IlluminaProbeTableType.class, "Illumina%"),
    };

    private ReferenceType outputType = ReferenceTypeRegistry.getReferenceType(EnsemblTranscriptTableType.class);

    public ProbeToEnsemblTranscriptHub(Properties properties)
    {
        super(properties);
    }

    public String getFuncGenDBName(DataElementPath ensemblPath)
    {
        try
        {
            DataCollection<?> dc = Module.getModule( ensemblPath.getDataElement() );
            return dc.getInfo().getProperty(EnsemblConstants.FUNCGEN_DATABASE_PROPERTY);
        }
        catch( Exception e )
        {
            return null;
        }
    }

    public String getQueryTemplateVersion(DataElementPath ensemblPath)
    {
        try
        {
            DataCollection<?> dc = Module.getModule( ensemblPath.getDataElement() );
            return dc.getInfo().getProperty("queryTemplateVersion");
        }
        catch( Exception e )
        {
            return null;
        }
    }

    @Override
    protected ReferenceType getOutputType()
    {
        return outputType;
    }

    @Override
    protected String getQueryTemplate(String species, DataElementPath ensemblPath, TypeRecord typeRecord)
    {
        String funcDbName      = getFuncGenDBName(ensemblPath);
        String templateVersion = getQueryTemplateVersion(ensemblPath);
        if( funcDbName == null )
        	return SQL_TRANSCRIPT + typeRecord.getRestrictionClause(species);
        if (templateVersion != null && templateVersion.equals("2")) {
        	return "SELECT DISTINCT ts.stable_id,sr.name FROM xref x JOIN object_xref " +
     	   "o USING(xref_id) JOIN transcript_stable_id ts ON(ts.transcript_id=o.ensembl_id) " +
            "JOIN transcript t ON (ts.transcript_id=t.transcript_id) JOIN seq_region sr " +
     	   "ON(t.seq_region_id=sr.seq_region_id) JOIN gxp_probe_transcript gx " +
            "ON(ts.stable_id=gx.transcript) WHERE ensembl_object_type='Transcript' " +
     	   "AND probe=?";
        }

        return SQL_PROBE.replace("#FUNC_GEN#", funcDbName)
                + " UNION DISTINCT " + SQL_PROBE_SET.replace( "#FUNC_GEN#", funcDbName )
                + " UNION DISTINCT " + SQL_TRANSCRIPT + typeRecord.getRestrictionClause(species);
    }

    @Override
    protected TypeRecord[] getSupportedTypeRecords()
    {
        return supportedTypeRecords;
    }

    @Override
    protected String getObjectType()
    {
        return "Probe";
    }
}
