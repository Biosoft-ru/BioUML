package biouml.plugins.chemoinformatics;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.openscience.cdk.interfaces.IChemFile;
import org.openscience.cdk.io.CMLReader;
import org.openscience.cdk.io.FormatFactory;
import org.openscience.cdk.io.ISimpleChemObjectReader;
import org.openscience.cdk.io.PDBReader;
import org.openscience.cdk.io.ReaderFactory;
import org.openscience.cdk.io.cml.CMLCoreModule;
import org.openscience.cdk.io.cml.CMLStack;
import org.openscience.cdk.io.cml.MDMoleculeConvention;
import org.openscience.cdk.io.formats.CMLFormat;
import org.openscience.cdk.io.formats.IChemFormatMatcher;
import org.openscience.cdk.io.formats.IResourceFormat;
import org.openscience.cdk.io.formats.MDLV2000Format;
import org.openscience.cdk.io.formats.MDLV3000Format;
import org.openscience.cdk.io.formats.PDBFormat;
import org.openscience.cdk.io.formats.SDFFormat;
import org.openscience.cdk.io.listener.PropertiesListener;

public class CDKManagerHelper
{
    private static final Logger logger = Logger.getLogger( CDKManagerHelper.class.getName() );

    /**
     * Register all formats that we support for reading in Bioclipse.
     *
     * @param fac CDK {@link ReaderFactory} to help reading.
     */
    public static void registerSupportedFormats(ReaderFactory fac)
    {
        IResourceFormat[] supportedFormats = {
                PDBFormat.getInstance(),
                SDFFormat.getInstance(),
                CMLFormat.getInstance(),
                MDLV2000Format.getInstance(),
                MDLV3000Format.getInstance()
        };
        for( IResourceFormat format : supportedFormats )
        {
            if( !fac.getFormats().contains( format ) )
                fac.registerFormat( (IChemFormatMatcher)format );
        }
    }

    /**
     * Register all formats known to the CDK.
     */
    public static void registerAllFormats(FormatFactory fac)
    {
        try( InputStream iStream = CDKManagerHelper.class.getClassLoader().getResourceAsStream( "/io-formats.set" );
                BufferedReader reader = new BufferedReader( new InputStreamReader( iStream, StandardCharsets.ISO_8859_1 ) ) )
        {
            while( reader.ready() )
            {
                // load them one by one
                String formatName = reader.readLine();
                try
                {
                    Class<?> formatClass = CDKManagerHelper.class.getClassLoader().loadClass( formatName );
                    Method getinstanceMethod = formatClass.getMethod( "getInstance", new Class[0] );
                    Object format = getinstanceMethod.invoke( null, new Object[0] );
                    if( format instanceof IChemFormatMatcher )
                    {
                        IChemFormatMatcher matcher = (IChemFormatMatcher)format;
                        fac.registerFormat( matcher );
                        logger.log( Level.FINE, "Loaded IO format: " + format.getClass().getName() );
                    }
                }
                catch( ClassNotFoundException exception )
                {
                    logger.log( Level.WARNING, "Could not find this ChemObjectReader: " + formatName, exception );
                }
                catch( Exception exception )
                {
                    logger.log( Level.WARNING, "Could not find this ChemObjectReader: " + formatName, exception );
                }
            }
        }
        catch( IOException e )
        {
            logger.log( Level.WARNING, "Error loading all formats", e );
        }
    }

    public static void customizeReading(ISimpleChemObjectReader reader)
    {
        logger.log( Level.FINE, "customingIO, reader found: " + reader.getClass().getName() );
        logger.log( Level.FINE, "Found # IO settings: " + reader.getIOSettings().length );
        if( reader instanceof PDBReader )
        {
            Properties customSettings = new Properties();
            customSettings.setProperty( "DeduceBonding", "false" );

            PropertiesListener listener = new PropertiesListener( customSettings );
            reader.addChemObjectIOListener( listener );
        }

        if( reader instanceof CMLReader )
        {
            ( (CMLReader)reader ).registerConvention( "md:mdMolecule", new MDMoleculeConvention( (IChemFile)null ) );
            logger.log( Level.FINE, "****** CmlReader, registered MDMoleculeConvention" );

            ( (CMLReader)reader ).registerConvention( "bioclipse:atomType", new CMLCoreModule( (IChemFile)null )
            {
                List<String> atomTypes = new ArrayList<>();

                @Override
                protected void newAtomData()
                {
                    super.newAtomData();
                    atomTypes = new ArrayList<>();
                };

                @Override
                protected void storeAtomData()
                {
                    super.storeAtomData();

                    boolean hasAtomType = false;
                    if( atomTypes.size() == atomCounter )
                    {
                        hasAtomType = true;
                    }
                    else
                    {
                        logger.debug( "No atom types: " + elid.size(), " != " + atomCounter );
                    }
                    if( hasAtomType )
                    {
                        for( int i = 0; i < atomCounter; i++ )
                        {
                            currentAtom = currentMolecule.getAtom( i );
                            currentAtom.setAtomTypeName( atomTypes.get( i ) );
                        }
                    }
                }

                @Override
                public void endElement(CMLStack xpath, String uri, String name, String raw)
                {
                    if( xpath.endsWith( "atom", "atomType" ) )
                    {
                        while( ( atomTypes.size() + 1 ) < atomCounter )
                            atomTypes.add( null );
                        atomTypes.add( currentChars );
                    }
                    else
                    {
                        super.endElement( xpath, uri, name, raw );
                    }
                }
            } );
        }
    }
}
