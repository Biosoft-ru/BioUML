package biouml.plugins.chemoinformatics;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.Nonnull;

import org.openscience.cdk.AtomContainer;
import org.openscience.cdk.CDKConstants;
import org.openscience.cdk.ChemFile;
import org.openscience.cdk.ConformerContainer;
import org.openscience.cdk.DefaultChemObjectBuilder;
import org.openscience.cdk.Isotope;
import org.openscience.cdk.Molecule;
import org.openscience.cdk.aromaticity.CDKHueckelAromaticityDetector;
import org.openscience.cdk.atomtype.CDKAtomTypeMatcher;
import org.openscience.cdk.config.Elements;
import org.openscience.cdk.exception.CDKException;
import org.openscience.cdk.exception.InvalidSmilesException;
import org.openscience.cdk.fingerprint.Fingerprinter;
import org.openscience.cdk.fingerprint.FingerprinterTool;
import org.openscience.cdk.geometry.GeometryTools;
import org.openscience.cdk.geometry.alignment.KabschAlignment;
import org.openscience.cdk.graph.ConnectivityChecker;
import org.openscience.cdk.interfaces.IAtom;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.interfaces.IAtomType;
import org.openscience.cdk.interfaces.IBond;
import org.openscience.cdk.interfaces.IChemFile;
import org.openscience.cdk.interfaces.IChemObject;
import org.openscience.cdk.interfaces.IChemObjectBuilder;
import org.openscience.cdk.interfaces.IMolecularFormula;
import org.openscience.cdk.interfaces.IMolecule;
import org.openscience.cdk.interfaces.IMoleculeSet;
import org.openscience.cdk.io.FormatFactory;
import org.openscience.cdk.io.ISimpleChemObjectReader;
import org.openscience.cdk.io.MDLWriter;
import org.openscience.cdk.io.ReaderFactory;
import org.openscience.cdk.io.SDFWriter;
import org.openscience.cdk.io.formats.CMLFormat;
import org.openscience.cdk.io.formats.IChemFormat;
import org.openscience.cdk.io.formats.IChemFormatMatcher;
import org.openscience.cdk.io.formats.IResourceFormat;
import org.openscience.cdk.io.formats.MDLFormat;
import org.openscience.cdk.io.formats.MDLV2000Format;
import org.openscience.cdk.io.formats.SDFFormat;
import org.openscience.cdk.io.iterator.IteratingMDLConformerReader;
import org.openscience.cdk.isomorphism.UniversalIsomorphismTester;
import org.openscience.cdk.isomorphism.matchers.IQueryAtom;
import org.openscience.cdk.isomorphism.matchers.OrderQueryBond;
import org.openscience.cdk.isomorphism.matchers.QueryAtomContainer;
import org.openscience.cdk.isomorphism.matchers.smarts.AromaticQueryBond;
import org.openscience.cdk.isomorphism.mcss.RMap;
import org.openscience.cdk.layout.StructureDiagramGenerator;
import org.openscience.cdk.nonotify.NNAtom;
import org.openscience.cdk.nonotify.NNAtomContainer;
import org.openscience.cdk.nonotify.NNChemFile;
import org.openscience.cdk.nonotify.NNMolecule;
import org.openscience.cdk.nonotify.NoNotificationChemObjectBuilder;
import org.openscience.cdk.similarity.Tanimoto;
import org.openscience.cdk.smiles.SmilesGenerator;
import org.openscience.cdk.smiles.SmilesParser;
import org.openscience.cdk.smiles.smarts.SMARTSQueryTool;
import org.openscience.cdk.tools.CDKHydrogenAdder;
import org.openscience.cdk.tools.manipulator.AtomContainerManipulator;
import org.openscience.cdk.tools.manipulator.AtomTypeManipulator;
import org.openscience.cdk.tools.manipulator.ChemFileManipulator;
import org.openscience.cdk.tools.manipulator.MolecularFormulaManipulator;

import biouml.standard.type.CDKRenderer;
import biouml.standard.type.Structure;
import biouml.standard.type.access.StructureTransformer;
import one.util.streamex.EntryStream;
import one.util.streamex.StreamEx;
import ru.biosoft.access.file.FileDataElement;
import ru.biosoft.access.core.CollectionFactory;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataCollectionConfigConstants;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.TransformedDataCollection;
import ru.biosoft.access.security.SecurityManager;
import ru.biosoft.plugins.javascript.JavaScriptHostObjectBase;
import ru.biosoft.util.LazyValue;
import ru.biosoft.util.TransformedIterator;
import ru.biosoft.util.Util;

public class JavaScriptCDK extends JavaScriptHostObjectBase
{
    protected static final Logger log = Logger.getLogger(JavaScriptCDK.class.getName());
    private static final String NEW_STRUCTURE_PREFIX = "STR_";

    public JavaScriptCDK()
    {

    }

    public Structure addExplicitHydrogens(Structure molecule) throws Exception
    {
        IMolecule cdkmolecule = structureToMolecule(molecule);
        addImplicitHydrogens(cdkmolecule);
        AtomContainerManipulator.convertImplicitToExplicitHydrogens(cdkmolecule);
        return moleculeToStructure(cdkmolecule, NEW_STRUCTURE_PREFIX + "ExplHydr");
    }

    public Structure addImplicitHydrogens(Structure molecule) throws Exception
    {
        IMolecule cdkmolecule = structureToMolecule(molecule);
        addImplicitHydrogens(cdkmolecule);
        return moleculeToStructure(cdkmolecule, NEW_STRUCTURE_PREFIX + "ImplHydr");

    }

    private void addImplicitHydrogens(IMolecule molecule) throws Exception
    {
        CDKAtomTypeMatcher matcher = CDKAtomTypeMatcher.getInstance(molecule.getBuilder());
        Iterator<IAtom> atoms = molecule.atoms().iterator();

        try
        {
            while( atoms.hasNext() )
            {
                IAtom atom = atoms.next();
                IAtomType type = matcher.findMatchingAtomType(molecule, atom);
                AtomTypeManipulator.configure(atom, type);
            }
            CDKHydrogenAdder hAdder = CDKHydrogenAdder.getInstance(molecule.getBuilder());
            hAdder.addImplicitHydrogens(molecule);
        }
        catch( CDKException e )
        {
            throw new Exception(e);
        }
    }

    public Structure removeExplicitHydrogens(Structure molecule)
    {
        IAtomContainer container = structureToMolecule(molecule);
        for( int i = container.getAtomCount() - 1; i >= 0; i-- )
        {
            IAtom atom = container.getAtom(i);
            if( "H".equals(atom.getSymbol()) )
            {
                container.removeAtomAndConnectedElectronContainers(atom);
            }
        }
        return moleculeToStructure(container, molecule.getName());
    }

    public Structure removeImplicitHydrogens(Structure molecule)
    {
        IAtomContainer container = structureToMolecule(molecule);
        for( IAtom atom : container.atoms() )
        {
            atom.setHydrogenCount(0);
        }
        return moleculeToStructure(container, molecule.getName());
    }

    private int calculateMissingHydrogens(IAtomContainer container, IAtom atom)
    {
        CDKAtomTypeMatcher matcher = CDKAtomTypeMatcher.getInstance(container.getBuilder());
        IAtomType type;
        try
        {
            type = matcher.findMatchingAtomType(container, atom);
            if( type == null || type.getAtomTypeName() == null )
                return 0;

            if( "X".equals(atom.getAtomTypeName()) )
            {
                return 0;
            }

            if( type.getFormalNeighbourCount() == CDKConstants.UNSET )
                return 0;

            // very simply counting:
            // each missing explicit neighbor is a missing hydrogen
            return type.getFormalNeighbourCount() - container.getConnectedAtomsCount(atom);
        }
        catch( CDKException e )
        {
            return 0;
        }
    }

    public boolean areIsomorphic(Structure molecule1, Structure molecule2) throws Exception
    {
        IMolecule mol1 = structureToMolecule(molecule1);
        IMolecule mol2 = structureToMolecule(molecule2);
        return UniversalIsomorphismTester.isIsomorph(mol1, mol2);
    }

    public double calculateMass(Structure molecule)
    {
        IMolecule cdkmolecule = structureToMolecule(molecule);
        IMolecularFormula mf = molecularFormulaObject(cdkmolecule);
        // use four digits in the precision
        double mass = MolecularFormulaManipulator.getNaturalExactMass(mf);
        mass = ( Math.round(mass * 10000.0) ) / 10000.0;

        return mass;
    }

    private IMolecularFormula molecularFormulaObject(IMolecule m)
    {
        IMolecularFormula mf = MolecularFormulaManipulator.getMolecularFormula(m);

        int missingHCount = 0;
        for( IAtom atom : m.atoms() )
        {
            missingHCount += calculateMissingHydrogens(m, atom);
        }

        if( missingHCount > 0 )
        {
            mf.addIsotope(m.getBuilder().newInstance(Isotope.class, Elements.HYDROGEN), missingHCount);
        }
        return mf;
    }

    public String calculateRMSD(Structure[] molecules) throws Exception
    {
        StringBuilder matrix = new StringBuilder();
        int molCount = molecules.length;
        for( int row = 0; row < molCount; row++ )
        {
            matrix.append(',').append(molecules[row].getName());
        }
        matrix.append("\n");
        for( int row = 0; row < molCount; row++ )
        {
            Structure rowMol = molecules[row];
            Structure[] alignedMols = kabsch(molecules, rowMol);
            matrix.append(molecules[row].getName()).append(',');
            for( int col = 0; col < molCount; col++ )
            {
                Structure colMol = alignedMols[col];
                IMolecule colMol1 = structureToMolecule(colMol);
                matrix.append(String.format("%.3f", colMol1.getProperty("MCSS-RMSD")));
                if( col < ( molCount - 1 ) )
                    matrix.append(',');
            }
            matrix.append('\n');
        }
        return matrix.toString();
    }

    public Structure[] kabsch(Structure[] structures, Structure molecule) throws Exception
    {
        if( structures.length < 2 )
            throw new IllegalArgumentException("List must contain at least two molecules.");

        List<Structure> results = new ArrayList<>();
        IMolecule cdkmolecule = structureToMolecule(molecule);

        List<IMolecule> molecules = new ArrayList<>();
        for( Structure struct : structures )
        {
            IMolecule mol = structureToMolecule(struct);
            molecules.add(mol);
        }
        IMolecule mcss = mcssMolecule(molecules);
        if( mcss.getAtomCount() < 3 )
            throw new Exception("The MCSS must have at least 3 atoms.");
        IMolecule firstSubstructure = getSubstructures(cdkmolecule, mcss)[0];

        for( IMolecule mol : molecules )
        {
            IMolecule substructure = getSubstructures(mol, mcss)[0];
            try
            {
                KabschAlignment ka = new KabschAlignment(firstSubstructure, substructure);
                ka.align();
                IAtomContainer clone = (IAtomContainer)mol.clone();
                ka.rotateAtomContainer(clone);
                clone.setProperty("MCSS-RMSD", ka.getRMSD());
                Structure result = moleculeToStructure(clone, NEW_STRUCTURE_PREFIX + "kabsh");
                results.add(result);
            }
            catch( CloneNotSupportedException exc )
            {
                throw new Exception("Failed to clone the input", exc);
            }
            catch( CDKException exception )
            {
            }
        }
        return results.toArray(new Structure[results.size()]);
    }

    public Structure mcss(Structure[] structures) throws Exception
    {
        List<IMolecule> molecules = new ArrayList<>();
        for( Structure struct : structures )
        {
            IMolecule mol = structureToMolecule(struct);
            molecules.add(mol);
        }
        IMolecule mol = mcssMolecule(molecules);
        return moleculeToStructure(mol, NEW_STRUCTURE_PREFIX + "mcss");
    }

    private IMolecule mcssMolecule(List<IMolecule> molecules) throws Exception
    {
        if( molecules.size() < 2 )
            throw new IllegalArgumentException("List must contain at least two molecules.");

        IMolecule firstMolecule = molecules.get(0);
        IAtomContainer mcss = firstMolecule;
        int counter = 1;
        for( IMolecule followupMolecule : molecules )
        {
            counter++;
            try
            {
                mcss = UniversalIsomorphismTester.getOverlaps(mcss, followupMolecule).get(0);

            }
            catch( CDKException exception )
            {
                throw new Exception("Could not determine MCSS, because of molecule " + counter + ": " + exception.getMessage());
            }
        }
        IMolecule newMolecule = new NNMolecule();
        newMolecule.add(mcss);
        return newMolecule;
    }

    public String calculateSMILES(Structure molecule)
    {
        if( molecule == null )
            return null;
        IAtomContainer cdkMol = structureToMolecule(molecule);


        // Create the SMILES
        SmilesGenerator generator = new SmilesGenerator();
        // Operate on a clone with removed hydrogens
        cdkMol = AtomContainerManipulator.removeHydrogens(cdkMol);
        IMolecule newMol;
        if( cdkMol instanceof IMolecule )
        {
            newMol = (IMolecule)cdkMol;
        }
        else
        {
            newMol = ( cdkMol.getBuilder() ).newInstance(Molecule.class, cdkMol);
        }
        String result = null;
        try
        {
            result = (String)SecurityManager.runPrivileged( () -> {
                return generator.createSMILES( newMol );
            } );
        }
        catch( Exception e )
        {
        }
        return result;
    }

    public Float[] calculateTanimoto(Structure[] calculateFor, Structure reference) throws Exception
    {
        List<Float> result = new ArrayList<>();
        IMolecule molecule = structureToMolecule(reference);
        Fingerprinter fp = new Fingerprinter();
        BitSet fingerprint = fp.getFingerprint(molecule);
        for( Structure structure : calculateFor )
        {
            result.add(calculateTanimoto(structure, fingerprint));
        }

        return result.toArray(new Float[result.size()]);
    }

    public Float calculateTanimoto(Structure structure, BitSet fingerprint) throws Exception
    {
        IMolecule molecule = structureToMolecule(structure);
        Fingerprinter fp = new Fingerprinter();
        BitSet fingerprint2 = fp.getFingerprint(molecule);
        return calculateTanimoto(fingerprint, fingerprint2);
    }

    public Float calculateTanimoto(BitSet fingerprint1, BitSet fingerprint2) throws Exception
    {
        return Tanimoto.calculate(fingerprint1, fingerprint2);
    }

    public Float calculateTanimoto(Structure calculateFor, Structure reference) throws Exception
    {
        Fingerprinter fp = new Fingerprinter();
        BitSet fingerprint1 = fp.getFingerprint(structureToMolecule(calculateFor));
        BitSet fingerprint2 = fp.getFingerprint(structureToMolecule(reference));
        return calculateTanimoto(fingerprint1, fingerprint2);
    }

    public String calculateTanimoto(Structure[] molecules) throws Exception
    {
        StringBuilder matrix = new StringBuilder();
        int molCount = molecules.length;
        for( int row = 0; row < molCount; row++ )
        {
            matrix.append(',').append(molecules[row].getName());
        }
        matrix.append('\n');
        for( int row = 0; row < molCount; row++ )
        {
            Structure rowMol = molecules[row];
            matrix.append(molecules[row].getName()).append(',');
            //BitSet reference = rowMol.getFingerprint(Property.USE_CALCULATED);
            for( int col = 0; col < row; col++ )
            {
                matrix.append(String.format("%.3f", 0.0));
                if( col < ( molCount - 1 ) )
                    matrix.append(',');
            }
            matrix.append(String.format("%.3f", 1.0));
            if( row < ( molCount - 1 ) )
                matrix.append(',');
            for( int col = ( row + 1 ); col < molCount; col++ )
            {
                Structure colMol = molecules[col];
                //BitSet compare = colMol.getFingerprint(Property.USE_CALCULATED);
                matrix.append(String.format("%.3f", calculateTanimoto(rowMol, colMol)));
                if( col < ( molCount - 1 ) )
                    matrix.append(',');
            }
            matrix.append('\n');
        }
        return matrix.toString();
    }



    public boolean fingerPrintMatches(Structure molecule, Structure subStructure) throws Exception
    {
        Fingerprinter fp = new Fingerprinter();
        BitSet fingerprint1 = fp.getFingerprint(structureToMolecule(molecule));
        BitSet fingerprint2 = fp.getFingerprint(structureToMolecule(subStructure));
        return FingerprinterTool.isSubset(fingerprint1, fingerprint2);
    }

    public Structure generate2dCoordinates(Structure molecule) throws Exception
    {
        Structure[] molecules = new Structure[] {molecule};
        Structure[] result = generate2dCoordinates(molecules);
        if( result != null && result.length > 0 )
            return result[0];
        else
            return null;
    }

    public Structure[] generate2dCoordinates(Structure[] molecules) throws Exception
    {
        IMolecule cdkmol = null;
        List<Structure> newMolecules = new ArrayList<>();

        for( Structure molecule : molecules )
        {
            cdkmol = structureToMolecule(molecule);
            String name = molecule.getName();

            IMoleculeSet mols = ConnectivityChecker.partitionIntoMolecules(cdkmol);

            StructureDiagramGenerator sdg = new StructureDiagramGenerator();

            IMolecule newmolecule = mols.getBuilder().newInstance(Molecule.class);
            for( IAtomContainer mol : mols.molecules() )
            {
                try
                {
                    sdg.setMolecule(cdkmol.getBuilder().newInstance(Molecule.class, mol));
                }
                catch( Exception e )
                {

                }
                sdg.generateCoordinates();
                IAtomContainer ac = sdg.getMolecule();
                newmolecule.add(ac);
            }
            // copy IAtomContainer properties
            newmolecule.setProperties(cdkmol.getProperties());

            newMolecules.add(moleculeToStructure(newmolecule, name + "_2d"));
        }
        return newMolecules.toArray(new Structure[newMolecules.size()]);
    }

    public Structure[] generate3dCoordinates(Structure[] molecules)
    {
        return null;
        /*IMolecule cdkmol = null;
        List<Structure> newMolecules = new ArrayList<Structure>();
        //TODO: Implement when 3D is available
        /*for( int i = 0; i < molecules.length; i++ )
        {
            cdkmol = structureToMolecule(molecules[i]);
            String name = molecules[i].getName();
            ModelBuilder3D mb3d;
            try
            {
                mb3d = ModelBuilder3D.getInstance();
            }
            catch( CDKException e )
            {
                throw new Exception(e.getMessage());
            }
            IMoleculeSet mols = ConnectivityChecker.partitionIntoMolecules(cdkmol);

            IMolecule newmolecule = cdkmol.getBuilder().newInstance(Molecule.class);

            for( IAtomContainer mol : mols.molecules() )
            {
                try
                {
                    IMolecule ac = mb3d.generate3DCoordinates((IMolecule)mol, false);
                    newmolecule.add(ac);
                }
                catch( NoSuchAtomTypeException ex )
                {
                    throw new Exception(ex.getMessage() + ", molecule number " + i, ex);
                }
                catch( Exception e )
                {
                    throw new Exception(e.getMessage(), e);
                }
            }
            newMolecules.add(moleculeToStructure(newmolecule, name + "_3d"));
        }
        return newMolecules.toArray(new Structure[newMolecules.size()]);*/
    }
    public Structure generate3dCoordinates(Structure molecule)
    {
        Structure[] molecules = new Structure[] {molecule};
        Structure[] result = generate3dCoordinates(molecules);
        if( result != null && result.length > 0 )
            return result[0];
        else
            return null;
    }

    public MoleculesInfo getInfo(String filename)
    {
        int numMols = 0;
        int num2d = 0;
        int num3d = 0;
        Structure[] lst;
        try
        {
            lst = loadMolecules(filename);
            if( lst != null )
            {
                for( Structure mol : lst )
                {
                    numMols++;
                    if( has2d(mol) )
                        num2d++;
                    if( has3d(mol) )
                        num3d++;
                }
                MoleculesInfo retInfo = new MoleculesInfo(numMols, num2d, num3d);
                return retInfo;
            }
        }
        catch( Exception e )
        {
            log.log(Level.FINE, "Could not count mols in file: " + filename + ". Reason: " + e.getMessage());
        }
        return null;
    }

    public Object getProperty(Structure molecule, String propertyName)
    {
        IMolecule cdkmolecule = structureToMolecule(molecule);
        if( cdkmolecule == null )
        {
            throw new IllegalArgumentException("Passed Molecule has a null IAtomContainer.");
        }
        return cdkmolecule.getProperty(propertyName);
    }

    public String molecularFormula(Structure molecule)
    {
        return molecularFormula(structureToMolecule(molecule));
    }

    private String molecularFormula(IAtomContainer mol)
    {
        return MolecularFormulaManipulator.getString(molecularFormulaObject(mol));
    }

    private IMolecularFormula molecularFormulaObject(IAtomContainer m)
    {
        IMolecularFormula mf = MolecularFormulaManipulator.getMolecularFormula(m);

        int missingHCount = 0;
        for( IAtom atom : m.atoms() )
        {
            missingHCount += calculateMissingHydrogens(m, atom);
        }

        if( missingHCount > 0 )
        {
            mf.addIsotope(m.getBuilder().newInstance(Isotope.class, Elements.HYDROGEN), missingHCount);
        }
        return mf;
    }

    public boolean has2d(Structure mol)
    {
        return GeometryTools.has2DCoordinates(structureToMolecule(mol));
    }

    public boolean has3d(Structure mol)
    {
        return GeometryTools.has3DCoordinates(structureToMolecule(mol));
    }

    public int getNoMolecules(String filename) throws Exception
    {
        if( filename == null )
            throw new IllegalArgumentException("Cannot get number of molecules: file was null");

        DataElement de = CollectionFactory.getDataElement(filename);

        if( de == null )
            throw new IllegalArgumentException("Data element is null");
        if( de instanceof TransformedDataCollection )
        {
            if( ( (TransformedDataCollection)de ).getTransformer() instanceof StructureTransformer )
            {
                return ( (TransformedDataCollection)de ).getSize();
            }
        }

        Structure[] lst = loadMolecules(filename);
        if( lst != null )
            return lst.length;

        return -1;
    }

    public Structure[] getSmartsMatches(Structure molecule, String smarts) throws Exception
    {

        SMARTSQueryTool querytool;

        try
        {
            querytool = new SMARTSQueryTool(smarts);
        }
        catch( CDKException e )
        {
            throw new Exception("Could not parse SMARTS query", e);
        }

        try
        {
            IAtomContainer ac = structureToMolecule(molecule);
            if( querytool.matches(ac) )
            {
                List<Structure> retac = new ArrayList<>();
                int nmatch = querytool.countMatches();
                log.log(Level.FINE, "Found " + nmatch + " SMARTS matches");

                List<List<Integer>> mappings = querytool.getMatchingAtoms();
                for( int i = 0; i < nmatch; i++ )
                {
                    List<Integer> atomIndices = mappings.get(i);
                    IAtomContainer match = ac.getBuilder().newInstance(AtomContainer.class);
                    for( Integer aindex : atomIndices )
                    {
                        IAtom atom = ac.getAtom(aindex);
                        match.addAtom(atom);
                    }
                    retac.add(moleculeToStructure(match, NEW_STRUCTURE_PREFIX + "SM" + i));
                }
                return retac.toArray(new Structure[retac.size()]);
            }
            return null;
        }
        catch( CDKException e )
        {
            throw new Exception("A problem occured trying to match SMARTS query", e);
        }
    }

    public Structure[] getSubstructures(Structure molecule, Structure substructure) throws Exception
    {
        IMolecule[] mols = getSubstructures(structureToMolecule(molecule), structureToMolecule(substructure));
        Structure[] molecules = new Structure[mols.length];
        int i = 0;
        for( IAtomContainer mol : mols )
        {
            molecules[i++] = moleculeToStructure(mol, NEW_STRUCTURE_PREFIX + "Sub" + i);
        }
        return molecules;
    }

    private IMolecule[] getSubstructures(IMolecule originalContainer, IMolecule substructure) throws Exception
    {
        // the below code is going to use IAtom.Id, so we need to keep
        // track of the originals. At the same time, we overwrite them
        // with internal identifiers.
        for( int i = 0; i < originalContainer.getAtomCount(); i++ )
        {
            IAtom atom = originalContainer.getAtom(i);
            // the new identifier is simply the position in the container
            atom.setID(String.valueOf(i));
        }

        List<IAtomContainer> uniqueMatches = new ArrayList<>();
        try
        {
            // get all matches, which may include duplicates
            List<List<RMap>> substructures = UniversalIsomorphismTester.getSubgraphMaps(originalContainer, substructure);
            for( List<RMap> substruct : substructures )
            {
                // convert the RMap into an IAtomContainer
                IAtomContainer match = new NNAtomContainer();
                for( RMap mapping : substruct )
                {
                    IBond bond = originalContainer.getBond(mapping.getId1());
                    for( IAtom atom : bond.atoms() )
                        match.addAtom(atom);
                    match.addBond(bond);
                }
                // OK, see if we already have an equivalent match
                boolean foundEquivalentSubstructure = false;
                for( IAtomContainer mol : uniqueMatches )
                {
                    QueryAtomContainer matchQuery = createQueryContainer(match);
                    if( UniversalIsomorphismTester.isIsomorph(mol, matchQuery) )
                        foundEquivalentSubstructure = true;
                }
                if( !foundEquivalentSubstructure )
                {
                    // make a clone (to ensure modifying it doesn't change the
                    // original), and wrap in a CDKMolecule.
                    uniqueMatches.add((IAtomContainer)match.clone());
                }
            }
        }
        catch( CDKException | CloneNotSupportedException e )
        {
            throw new Exception("Error while finding substructures: " + e.getMessage(), e);
        }
        // set up a List<ICDKMolecule> return list
        Molecule[] molecules = new Molecule[uniqueMatches.size()];
        int i = 0;
        for( IAtomContainer mol : uniqueMatches )
        {
            molecules[i++].add(mol);
        }
        return molecules;
    }

    private static QueryAtomContainer createQueryContainer(IAtomContainer container)
    {
        QueryAtomContainer queryContainer = new QueryAtomContainer();
        for( int i = 0; i < container.getAtomCount(); i++ )
        {
            queryContainer.addAtom(new NNAtom(container.getAtom(i).getID()));
        }
        Iterator<IBond> bonds = container.bonds().iterator();
        while( bonds.hasNext() )
        {
            IBond bond = bonds.next();
            int index1 = container.getAtomNumber(bond.getAtom(0));
            int index2 = container.getAtomNumber(bond.getAtom(1));
            if( bond.getFlag(CDKConstants.ISAROMATIC) )
            {
                queryContainer.addBond(new AromaticQueryBond((IQueryAtom)queryContainer.getAtom(index1), (IQueryAtom)queryContainer
                        .getAtom(index2), IBond.Order.SINGLE));
            }
            else
            {
                queryContainer.addBond(new OrderQueryBond((IQueryAtom)queryContainer.getAtom(index1), (IQueryAtom)queryContainer
                        .getAtom(index2), bond.getOrder()));
            }
        }
        return queryContainer;
    }


    public boolean isConnected(Structure molecule) throws Exception
    {
        List<IAtomContainer> containers = partition(structureToMolecule(molecule));
        return containers.size() == 1;
    }

    public Structure[] partition(Structure molecule) throws Exception
    {
        List<IAtomContainer> containers = partition(structureToMolecule(molecule));
        return EntryStream.of(containers)
            .mapKeyValue( (i, ac) -> moleculeToStructure(ac, NEW_STRUCTURE_PREFIX + "Part" + ( i + 1 )) )
            .toArray( Structure[]::new );
    }

    private List<IAtomContainer> partition(IMolecule molecule) throws Exception
    {
        IAtomContainer todealwith = molecule;
        IMoleculeSet set = ConnectivityChecker.partitionIntoMolecules(todealwith);
        List<IAtomContainer> result = new ArrayList<>();
        for( IAtomContainer container : set.atomContainers() )
        {
            result.add(container);
        }
        return result;
    }

    public boolean isValidSmarts(String smarts)
    {
        try
        {
            new SMARTSQueryTool(smarts);
            return true;
        }
        catch( Exception error )
        {
            return false;
        }
    }

    /**
     * Reads files and extracts conformers if available. Currently limited to
     * read SDFiles, CMLFiles is for the future.
     * @throws Exception
     */
    public Structure[] loadConformers(String filename) throws Exception
    {
        Iterator<IAtomContainer> it = createConformerIterator(filename);

        List<Structure> mols = new ArrayList<>();

        int i = 1;
        while( it.hasNext() )
        {
            IMolecule molecule = (IMolecule)it.next();
            String molName = (String)molecule.getProperty(CDKConstants.TITLE);
            if( molName == null || molName.equals("") )
            {
                molName = NEW_STRUCTURE_PREFIX + "conf" + i++;
            }
            Structure str = moleculeToStructure(molecule, molName);
            mols.add(str);
        }
        clearFile();
        if( mols.isEmpty() )
            throw new IllegalArgumentException("No conformers could be read");
        return mols.toArray(new Structure[mols.size()]);
    }

    private Iterator<IAtomContainer> createConformerIterator(String filename) throws Exception
    {
        File file = getFile(filename);
        FileInputStream fileStream = new FileInputStream(file);
        return new MDLConformerReader(fileStream, NoNotificationChemObjectBuilder.getInstance());
    }

    private static class MDLConformerReader extends TransformedIterator<ConformerContainer, IAtomContainer>
    {
        public MDLConformerReader(InputStream input, IChemObjectBuilder builder)
        {
            super(new IteratingMDLConformerReader(input, builder));
        }

        @Override
        protected IAtomContainer transform(ConformerContainer cdkMol)
        {
            return (IAtomContainer)cdkMol;
        }
    }

    public boolean perceiveAromaticity(Structure mol) throws Exception
    {
        IAtomContainer todealwith = structureToMolecule(mol);
        try
        {
            AtomContainerManipulator.percieveAtomTypesAndConfigureAtoms(todealwith);
            return CDKHueckelAromaticityDetector.detectAromaticity(todealwith);
        }
        catch( CDKException ex )
        {
            throw new Exception("Problems perceiving aromaticity: " + ex.getMessage());
        }
    }

    //TODO: check if need to implement saving methods
    /*
    public void saveCML (ICDKMolecule molecule, String filename)
    {
    }
    public void saveMDLMolfile (ICDKMolecule molecule, String filename)
    {
    }
    public void saveMolecule (IMolecule mol, String filename)
    {
    }
    public void saveMolecule (IMolecule mol, String filename, boolean overwrite)
    {
    }
    public void saveMolecule (IMolecule mol, String filename, IChemFormat filetype)
    {
    }
    public void saveMolecule (IMolecule mol, boolean overwrite)
    {
    }
    public void saveMolecule (IMolecule mol)
    {
    }
    public void saveSDFile (String file, List<IMolecule> entries)
    {
    }
     */

    public boolean smartsMatches(Structure molecule, String smarts) throws Exception
    {
        SMARTSQueryTool querytool;

        try
        {
            querytool = new SMARTSQueryTool(smarts);
        }
        catch( CDKException e )
        {
            throw new Exception("Could not parse SMARTS query", e);
        }

        try
        {
            return querytool.matches(structureToMolecule(molecule));
        }
        catch( CDKException e )
        {
            if( e.getMessage().contains("Timeout for AllringsFinder exceeded") )
            {
                throw new Exception("The AllringsFinder in CDK " + "did not succed in time", e);
            }
            throw new Exception("A problem occured trying " + "to match SMARTS query", e);
        }
    }

    public boolean subStructureMatches(Structure molecule, Structure subStructure)
    {
        try
        {
            return UniversalIsomorphismTester.isSubgraph(structureToMolecule(molecule), structureToMolecule(subStructure));
        }
        catch( CDKException e )
        {
            throw new RuntimeException(e);
        }
    }

    public Structure[] subStructureMatches(Structure[] molecules, Structure subStructure)
    {
        return StreamEx.of( molecules ).filter( Util.safePredicate( (Structure mol) -> subStructureMatches( mol, subStructure ) ) )
                .toArray( Structure[]::new );
    }

    public int totalFormalCharge(Structure molecule) throws Exception
    {
        IAtomContainer todealwith = structureToMolecule(molecule);
        int totalCharge = 0;
        for( IAtom atom : todealwith.atoms() )
        {
            totalCharge += atom.getFormalCharge() == null ? 0 : atom.getFormalCharge();
        }
        return totalCharge;
    }

    /**
     * File formats, reading and writing
     */

    // ReaderFactory used to instantiate IChemObjectReaders
    private static final LazyValue<ReaderFactory> readerFactory = new LazyValue<ReaderFactory>("CDK reader factory")
    {
        @Override
        protected ReaderFactory doGet() throws Exception
        {
            ReaderFactory readerFactory = new ReaderFactory();
            CDKManagerHelper.registerSupportedFormats(readerFactory);
            return readerFactory;
        }
    };

    // ReaderFactory used solely to determine chemical file formats
    private static final LazyValue<FormatFactory> formatsFactory = new LazyValue<>("CDK format factory", () ->
    {
        FormatFactory formatsFactory = new FormatFactory();
        CDKManagerHelper.registerAllFormats(formatsFactory);
        return formatsFactory;
    });

    public IChemFormat getFormat(String type)
    {
        List<IChemFormatMatcher> formats = formatsFactory.get().getFormats();
        for( IChemFormatMatcher format : formats )
        {
            if( format.getClass().getName().substring("org.openscience.cdk.io.formats.".length()).equals(type) )
                return format;
        }
        return null;
    }

    public String getFormats()
    {
        StringBuffer buffer = new StringBuffer();
        List<IChemFormatMatcher> formats = formatsFactory.get().getFormats();
        for( IChemFormatMatcher format : formats )
        {
            buffer.append(format.getClass().getName().substring("org.openscience.cdk.io.formats.".length()));
            buffer.append(": ");
            buffer.append(format.getFormatName());
            buffer.append('\n');
        }
        return buffer.toString();
    }

    public IChemFormat guessFormatFromExtension(String file)
    {
        if( file.endsWith(".mdl") )
        {
            return (IChemFormat)MDLV2000Format.getInstance();
        }
        for( IChemFormat aFormat : formatsFactory.get().getFormats() )
        {
            if( aFormat == MDLFormat.getInstance() )
            {
                // never match this one: it's outdated and != MDLV2000Format
            }
            else if( file.endsWith("." + aFormat.getPreferredNameExtension()) )
            {
                return aFormat;
            }
        }
        return null;
    }

    private IChemFormat determineIChemFormat(FileInputStream fileStream) throws IOException
    {
        return formatsFactory.get().guessFormat(new BufferedReader(new InputStreamReader(fileStream)));
    }

    public IChemFormat determineIChemFormatOfString(String content) throws IOException
    {
        return formatsFactory.get().guessFormat(new StringReader(content));
    }

    public IChemFormat determineIChemFormat(String path) throws Exception
    {
        IChemFormat format = null;
        if( path == null )
            throw new IllegalArgumentException("File was null");

        DataElement de = CollectionFactory.getDataElement(path);

        if( de == null )
            throw new IllegalArgumentException("Data element is null");

        File file = null;
        if( de instanceof FileDataElement )
        {
            file = ( (FileDataElement)de ).getFile();
            try( FileInputStream fileStream = new FileInputStream( file ) )
            {
                format = determineIChemFormat( fileStream );
            }
        }
        else if( de instanceof TransformedDataCollection )
        {
            if( ( (TransformedDataCollection)de ).getTransformer() instanceof StructureTransformer )
            {
                format = (IChemFormat)SDFFormat.getInstance();
            }
        }
        return format;
    }

    private File getFile(String filename) throws Exception
    {
        if( filename == null )
            throw new IllegalArgumentException("File was null");

        DataElement de = CollectionFactory.getDataElement(filename);

        if( de == null )
            throw new IllegalArgumentException("Data element is null");
        File file = null;
        if( de instanceof FileDataElement )
        {
            file = ( (FileDataElement)de ).getFile();

        }
        else if( de instanceof TransformedDataCollection )
        {
            DataCollection inputPrimary = ( (TransformedDataCollection)de ).getPrimaryCollection();
            Properties properties = inputPrimary.getInfo().getProperties();
            String path = properties.getProperty(DataCollectionConfigConstants.FILE_PATH_PROPERTY, ".");
            if( !path.endsWith(File.separator) )
                path += File.separator;

            File collectionFile = new File(path + properties.getProperty(DataCollectionConfigConstants.FILE_PROPERTY));
            if( ( (TransformedDataCollection)de ).getTransformer() instanceof StructureTransformer )
            {
                //file = new File(collectionFile.getParentFile(), collectionFile.getName().replace(".dat", ".sdf"));
                file = new File(collectionFile.getParentFile(), collectionFile.getName() + ".sdf");
                if( filesToDelete == null )
                    filesToDelete = new ArrayList<>();
                filesToDelete.add(file);

                try
                {
                    SDFExporter.exportStructures((DataCollection)de, file);
                }
                catch( Exception e )
                {
                    log.log(Level.SEVERE, "Can not export structures");
                    throw new Exception("Can not create sdf file for " + de);
                }
            }
            else
            {
                file = collectionFile;
            }
        }

        if( file == null )
            throw new IllegalArgumentException("Can not get file for data element " + filename);

        return file;

    }

    private List<File> filesToDelete = null;
    private void clearFile()
    {
        if( filesToDelete != null )
        {
            for( File f : filesToDelete )
            {
                f.delete();
            }
        }
    }

    public Structure loadMolecule(String filename) throws Exception
    {
        File file = getFile(filename);
        try( FileInputStream fileStream = new FileInputStream( file ) )
        {
            IChemFormat format = determineIChemFormat( fileStream );
            Structure loadedMol = loadMolecule( fileStream, format, NEW_STRUCTURE_PREFIX + "FromFile" );
            clearFile();
            return loadedMol;
        }
    }

    private Structure loadMolecule(InputStream instream, IChemFormat format, String name) throws Exception
    {
        // Create the reader
        ISimpleChemObjectReader reader = readerFactory.get().createReader(format);
        if( reader == null )
        {
            throw new Exception("Could not create reader in CDK.");
        }

        try
        {
            reader.setReader(instream);
        }
        catch( CDKException e1 )
        {
            throw new RuntimeException("Failed to set the reader's inputstream", e1);
        }

        // Do some customizations...
        CDKManagerHelper.customizeReading(reader);

        List<IAtomContainer> atomContainersList = new ArrayList<>();

        // Read file
        try
        {
            if( reader.accepts(ChemFile.class) )
            {
                IChemFile chemFile = reader.read(new NNChemFile());
                atomContainersList = ChemFileManipulator.getAllAtomContainers(chemFile);
            }
            else if( reader.accepts(Molecule.class) )
            {
                atomContainersList.add(reader.read(new NNMolecule()));
            }
            else
            {
                throw new RuntimeException("Failed to read file.");
            }
        }
        catch( CDKException e )
        {
            throw new RuntimeException("Failed to read file", e);
        }

        // Store the chemFormat used for the reader
        IResourceFormat chemFormat = reader.getFormat();
        log.log(Level.FINE, "Read CDK chemfile with format: " + chemFormat.getFormatName());

        int nuMols = atomContainersList.size();
        log.log(Level.FINE, "This file contained: " + nuMols + " molecules");
        if( atomContainersList.size() == 0 )
            throw new RuntimeException("File did not contain any molecule");
        if( atomContainersList.size() > 1 )
            log.log(Level.FINE, "Ignoring all but the first molecule.");

        IAtomContainer containerToReturn = atomContainersList.get(0);
        // sanatize the input for certain file formats
        IMolecule retmol = new Molecule(containerToReturn);
        // try to recover certain information for certain content types
        sanatizeFileInput(format, retmol);
        String molName = (String)containerToReturn.getProperty(CDKConstants.TITLE);
        if( molName == null )
            molName = ( name == null ) ? NEW_STRUCTURE_PREFIX + "Loaded" : name;
        Structure retstruct = moleculeToStructure(retmol, molName);
        return retstruct;
    }

    public Structure[] loadMolecules(String filename) throws Exception
    {
        List<Structure> loaded = loadMolecules(filename, null);
        return loaded.toArray(new Structure[loaded.size()]);
    }

    private List<Structure> loadMolecules(String filename, IChemFormat format) throws Exception
    {
        File file = getFile(filename);
        FileInputStream fileStream = new FileInputStream(file);
        List<Structure> moleculesList = new ArrayList<>();

        //System.out.println("Number of formats supported: " + readerFactory.getFormats().size());
        ISimpleChemObjectReader reader = null;
        if( format == null )
        {
            reader = readerFactory.get().createReader(fileStream);
        }
        else
        {
            reader = readerFactory.get().createReader(format);
        }

        if( reader == null )
        {

            // Try SMILES
            List<Structure> moleculesList2 = loadSMILESFile(file);
            if( moleculesList2 != null && moleculesList2.size() > 0 )
                return moleculesList2;

            // Ok, not even SMILES works
            throw new Exception("Could not create reader in CDK.");
        }

        try
        {
            fileStream.close();
            fileStream = new FileInputStream(file);
            reader.setReader(fileStream);
        }
        catch( CDKException e1 )
        {
            throw new Exception("Could not set the reader's input.");
        }

        IChemFile chemFile = new org.openscience.cdk.ChemFile();

        // Do some customizations...
        CDKManagerHelper.customizeReading(reader);

        // Read file
        try
        {
            chemFile = reader.read(chemFile);
        }
        catch( CDKException e )
        {
            throw new Exception("Cannot read file: " + e.getMessage(), e);
        }
        finally
        {
            fileStream.close();
            clearFile();
        }

        // Store the chemFormat used for the reader
        IChemFormat chemFormat = (IChemFormat)reader.getFormat();
        System.out.println("Read CDK chemfile with format: " + chemFormat.getFormatName());

        List<IAtomContainer> atomContainersList = ChemFileManipulator.getAllAtomContainers(chemFile);

        for( IAtomContainer mol : atomContainersList )
        {
            // try to recover certain information for certain content types
            sanatizeFileInput(chemFormat, mol);

            //Associate molecule with the file it comes from
            //mol.setResource(file);

            String moleculeName = molecularFormula(mol);
            // If there's a CDK property TITLE (read from file), use that
            // as name
            if( mol instanceof IMolecule )
            {

                IMolecule imol = (IMolecule)mol;

                String molName = (String)imol.getProperty("PUBCHEM_IUPAC_TRADITIONAL_NAME");

                if( molName == null || ( molName.equals("") ) )
                    molName = (String)imol.getProperty(CDKConstants.TITLE);

                if( molName != null && ! ( molName.equals("") ) )
                {
                    moleculeName = molName;
                }
            }
            Structure str = moleculeToStructure(mol, moleculeName);
            moleculesList.add(str);
        }
        return moleculesList;
    }

    public @Nonnull Structure[] loadSMILESFile(String filename) throws Exception
    {
        File file = getFile(filename);
        List<Structure> structures = loadSMILESFile(file);
        clearFile();
        return structures.toArray(new Structure[structures.size()]);
    }

    private @Nonnull List<Structure> loadSMILESFile(File file) throws Exception
    {
        if( file == null )
            throw new IllegalArgumentException("File is null");

        List<String[]> list = new LinkedList<>();
        try( BufferedInputStream buf = new BufferedInputStream(new FileInputStream(file));
                InputStreamReader reader = new InputStreamReader(buf);
                BufferedReader br = new BufferedReader(reader) )
        {
            if( !br.ready() )
            {
                throw new IOException("File: " + file.getName() + " is not ready to read.");
            }

            String line = br.readLine();

            if( line == null )
                throw new IOException("File: " + file.getName() + " has null contents");
            int cnt = 0;
            while( line != null )
            {
                //            System.out.println("Line " + cnt + ": " + line);
                Scanner smilesScanner = new Scanner(line).useDelimiter("\\s+");
                String part1 = null;
                String part2 = null;
                if( smilesScanner.hasNext() )
                {
                    part1 = smilesScanner.next();
                    if( smilesScanner.hasNext() )
                    {
                        part2 = smilesScanner.next();
                    }
                }
                if( part1 != null )
                {
                    if( part2 != null )
                    {
                        list.add(new String[] {part1, part2});
                    }
                    else
                    {
                        list.add(new String[] {part1, "entry-" + cnt});
                    }
                    //                System.out
                    //                        .println("  - " + part1 + " -> " + entries.get(part1));
                }
                // Get next line
                line = br.readLine();
                cnt++;
            }
        }
        // Depict where the smiles are, in first or second
        boolean smilesInFirst = true;
        String firstKey = list.get(0)[0];
        String firstVal = list.get(0)[1];
        Structure mol = null;
        try
        {
            mol = fromSMILES(firstKey, null);
        }
        catch( Exception e )
        {
        }
        if( mol == null )
        {
            try
            {
                fromSMILES(firstVal, null);
                smilesInFirst = false;
            }
            catch( Exception e )
            {
            }
        }
        List<Structure> mols = new ArrayList<>();
        for( String[] part : list )
        {
            if( smilesInFirst )
            {
                try
                {
                    mol = fromSMILES(part[0], part[1]);
                    mols.add(mol);
                }
                catch( Exception e )
                {
                }
            }
            else
            {
                try
                {
                    mol = fromSMILES(part[1], part[0]);
                    mols.add(mol);
                }
                catch( Exception e )
                {
                }
            }
        }
        return mols;
    }

    private void sanatizeFileInput(IChemFormat format, IAtomContainer molecule)
    {
        if( format == MDLV2000Format.getInstance() )
        {
            sanatizeMDLV2000MolFileInput(molecule);
        }
    }

    private void sanatizeMDLV2000MolFileInput(IAtomContainer molecule)
    {
        if( molecule != null && molecule.getAtomCount() > 0 )
        {
            CDKHydrogenAdder hAdder = CDKHydrogenAdder.getInstance(molecule.getBuilder());
            CDKAtomTypeMatcher matcher = CDKAtomTypeMatcher.getInstance(molecule.getBuilder());
            try
            {
                // perceive atom types
                IAtomType[] types = matcher.findMatchingAtomType(molecule);
                for( int i = 0; i < molecule.getAtomCount(); i++ )
                {
                    if( types[i] != null )
                    {
                        IAtom atom = molecule.getAtom(i);
                        // set properties needed for H adding and aromaticity
                        atom.setAtomTypeName(types[i].getAtomTypeName());
                        atom.setHybridization(types[i].getHybridization());
                        hAdder.addImplicitHydrogens(molecule, atom);
                    }
                }
                // perceive aromaticity
                CDKHueckelAromaticityDetector.detectAromaticity(molecule);
            }
            catch( CDKException e )
            {
                log.log(Level.SEVERE, "sanatizeMDLV2000MolFileInput error", e);
            }
        }
    }

    public String determineFormat(String path) throws Exception
    {
        IChemFormat format = determineIChemFormat(path);
        return format == null ? "Unknown" : format.getFormatName();
    }

    public Structure fromCml(String cml) throws Exception
    {
        return fromCml(cml, NEW_STRUCTURE_PREFIX + "cml");
    }

    public Structure fromCml(String cml, String name) throws Exception
    {
        if( cml == null )
            throw new IllegalArgumentException("Input cannot be null");

        ByteArrayInputStream bais = new ByteArrayInputStream(cml.getBytes());

        return loadMolecule((InputStream)bais, (IChemFormat)CMLFormat.getInstance(), name);
    }


    public Structure fromSMILES(String smilesDescription) throws Exception
    {
        return fromSMILES(smilesDescription, NEW_STRUCTURE_PREFIX + "SMILES");
    }

    public Structure fromSMILES(String smilesDescription, String name) throws Exception
    {
        SmilesParser parser = (SmilesParser)SecurityManager.runPrivileged( () -> {
            return new SmilesParser( DefaultChemObjectBuilder.getInstance() );
        } );
        try
        {
            IMolecule mol = parser.parseSmiles(smilesDescription);
            return moleculeToStructure(mol, name);
        }
        catch( InvalidSmilesException e )
        {
            throw new IllegalArgumentException("SMILES string is invalid", e);
        }
    }

    public Structure fromString(String molstring) throws Exception
    {
        return fromString(molstring, NEW_STRUCTURE_PREFIX + "string");
    }

    public Structure fromString(String molstring, String name) throws Exception
    {
        if( molstring == null )
            throw new IllegalArgumentException("Input cannot be null.");
        if( molstring.length() == 0 )
            throw new IllegalArgumentException("Input cannot be empty.");

        IChemFormat format = determineIChemFormatOfString(molstring);
        if( format == null )
            throw new Exception("Could not identify format for the input string.");

        return loadMolecule(new ByteArrayInputStream(molstring.getBytes()), format, name);
    }

    public String getMDLMolfileString(Structure molecule)
    {
        IMolecule cdkmolecule = structureToMolecule(molecule);

        StringWriter stringWriter = new StringWriter();
        MDLWriter writer = new MDLWriter(stringWriter);
        try
        {
            writer.writeMolecule(cdkmolecule);
            writer.close();
        }
        catch( Exception exc )
        {
            log.log(Level.SEVERE, "Error while creating MDL molfile string: " + exc.getMessage(), exc);
            return null;
        }
        return stringWriter.toString();
    }


    /**
     * Convert BioUML Structure to CDK IMolecule and vice versa
     */

    private IMolecule structureToMolecule(Structure structure)
    {
        return CDKRenderer.loadMolecule( structure );
    }

    private Structure moleculeToStructure(IAtomContainer mol, String name)
    {
        if( name == null )
            name = "New structure";
        Structure struct = new Structure(null, name);
        //Due to bug in CDK::MDLWriter with pseudoAtom we could have error here
        try
        {
            struct.setData(getStructureData(mol));
        }
        catch( Exception e )
        {

        }
        return struct;
    }

    /**
     * Transform CDK structure to string in MOL format
     */
    public static String getStructureData(IChemObject mol) throws Exception
    {
        StringWriter writer = new StringWriter();
        SDFWriter sdfwriter = new SDFWriter(writer);
        sdfwriter.write(mol);
        sdfwriter.close();
        String structData = writer.toString();
        int ind = structData.indexOf("$$$$");
        if( ind != -1 )
        {
            structData = structData.substring(0, ind - 1);
        }
        return structData;
    }
}
