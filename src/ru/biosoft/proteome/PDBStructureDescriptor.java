package ru.biosoft.proteome;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.VectorDataCollection;
import ru.biosoft.access.biohub.BioHub;
import ru.biosoft.access.biohub.BioHubRegistry;
import ru.biosoft.access.biohub.ReferenceType;
import ru.biosoft.access.biohub.ReferenceTypeRegistry;
import ru.biosoft.proteome.table.Structure3D;
import ru.biosoft.table.TableColumn;
import ru.biosoft.table.columnbeans.Descriptor;

/**
 * Special descriptor for columns with PDB structures
 */
public class PDBStructureDescriptor extends VectorDataCollection implements Descriptor
{
    public PDBStructureDescriptor(DataCollection parent, Properties properties)
    {
        super(parent, properties);
    }

    @Override
    public Map<String, Object> getColumnValues(List<String> names) throws Exception
    {
        Properties input = new Properties();
        input.setProperty(BioHub.TYPE_PROPERTY, ReferenceTypeRegistry.getReferenceType(UniprotReferenceType.class).getDisplayName());
        Properties output = new Properties();
        output.setProperty(BioHub.TYPE_PROPERTY, ReferenceTypeRegistry.getReferenceType(PDBReferenceType.class).getDisplayName());
        Map<String, String[]> hubResults = BioHubRegistry.getReferences(names.toArray(new String[names.size()]), input, output, null);

        Map<String, Object> result = new HashMap<>();
        if( hubResults != null )
        {
            for( String name : names )
            {
                String[] pdbLinks = hubResults.get(name);
                if( ( pdbLinks != null ) && ( pdbLinks.length > 0 ) )
                {
                    Structure3D structure = new Structure3D();
                    for( String pdbLink : pdbLinks )
                    {
                        structure.addLink(pdbLink, "http://www.pdb.org/pdb/files/" + pdbLink + ".pdb");
                    }
                    result.put(name, structure);
                }
            }
        }
        return result;
    }

    @Override
    public ReferenceType getInputReferenceType()
    {
        return null;
    }

    @Override
    public TableColumn createColumn()
    {
        return new TableColumn(getName(), Structure3D.class);
    }
}
