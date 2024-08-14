package biouml.workbench.diagram;

import java.beans.BeanInfo;
import java.beans.Introspector;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import one.util.streamex.StreamEx;

import java.util.logging.Level;
import java.util.logging.Logger;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionRegistry;

import com.developmentontheedge.beans.DynamicProperty;
import com.developmentontheedge.application.Application;

import ru.biosoft.access.ClassLoading;
import biouml.model.Diagram;
import biouml.model.DiagramType;
import biouml.model.DiagramTypeConverter;
import biouml.model.xml.XmlDiagramType;

public class DiagramTypeConverterRegistry
{
    /** Utility class that stores classes of diagram type and corresponding converter. */
    public static class Conversion
    {
        public Conversion(Object diagramType, Class<? extends DiagramTypeConverter> converter)
        {
            this.diagramType = diagramType;
            this.converter = converter;

            if( diagramType instanceof Class )
            {
                diagramTypeDisplayName = ( (Class<?>)diagramType ).getName();
                try
                {
                    BeanInfo info = Introspector.getBeanInfo((Class<?>)diagramType);
                    diagramTypeDisplayName = info.getBeanDescriptor().getDisplayName();
                }
                catch( Exception e )
                {
                }
            }
            else if( diagramType instanceof String )
            {
                XmlDiagramType diagramTypeObj = XmlDiagramType.getTypeObject((String)diagramType);
                if( diagramTypeObj != null )
                {
                    diagramTypeDisplayName = diagramTypeObj.getTitle();
                }
            }
        }

        private final Object diagramType;
        public Object getDiagramType()
        {
            return diagramType;
        }

        private String diagramTypeDisplayName;
        public String getDiagramTypeDisplayName()
        {
            return diagramTypeDisplayName;
        }

        private final Class<? extends DiagramTypeConverter> converter;
        public Class<? extends DiagramTypeConverter> getConverter()
        {
            return converter;
        }

        @Override
        public String toString()
        {
            return getDiagramTypeDisplayName();
        }
    }

    ///////////////////////////////////////////////////////////////////

    private static Logger log = Logger.getLogger(DiagramTypeConverterRegistry.class.getName());
    private static HashMap<String,List<Conversion>> map;
    private static HashMap<String,List<Conversion>> elementConverterMap;

    private static HashMap<String, Conversion> converterNameMap;

    public static final String CONVERSION = "conversion";
    public static final String DIAGRAM_TYPE_FROM = "from";
    public static final String DIAGRAM_TYPE_TO = "to";
    public static final String Converter_CLASS = "converter";
    public static final String ELEMENT_CONVERSION = "elementConversion";
    public static final String CONVERTER_NAME = "name";

    public static Conversion[] getPossibleConversions(String diagramType)
    {
        if( map == null )
            loadExtensions("biouml.workbench.diagramTypeConverter");

        if( !map.containsKey(diagramType) )
            return new Conversion[0];

        List<Conversion> list = map.get(diagramType);
        Conversion[] conversions = new Conversion[list.size()];
        return list.toArray(conversions);
    }

    protected static void loadExtensions(String extensionPointId)
    {
        map = new HashMap<>();
        elementConverterMap = new HashMap<>();
        converterNameMap = new HashMap<>();
        IExtensionRegistry registry = Application.getExtensionRegistry();
        IConfigurationElement[] extensions = registry.getConfigurationElementsFor(extensionPointId);

        for( IConfigurationElement extension : extensions )
        {
            String pluginId = extension.getNamespaceIdentifier();
            try
            {
                String extensionName = extension.getName();
                if( extensionName.equals(CONVERSION) )
                {
                    String from = extension.getAttribute(DIAGRAM_TYPE_FROM);
                    if( from == null )
                        throw new Exception("diagram type absents (from)");

                    String to = extension.getAttribute(DIAGRAM_TYPE_TO);
                    if( to == null )
                        throw new Exception("diagram type absents (to)");

                    Object diagramType = null;
                    if( to.startsWith(XmlDiagramType.class.getName()) )
                    {
                        int ind = to.lastIndexOf(':');
                        if( ind != -1 )
                            diagramType = to.substring(ind + 1, to.length());
                        else
                            throw new Exception("xml diagram type (to) should contains notation name");
                    }
                    else
                    {
                        diagramType = ClassLoading.loadSubClass( to, pluginId, DiagramType.class );
                    }

                    String converter = extension.getAttribute(Converter_CLASS);
                    if( converter == null )
                        throw new Exception("converter type absents");

                    Class<? extends DiagramTypeConverter> converterClass = ClassLoading.loadSubClass( converter, pluginId, DiagramTypeConverter.class );
                    if( map.containsKey(from) )
                        map.get(from).add(new Conversion(diagramType, converterClass));
                    else
                    {
                        List<Conversion> list = new ArrayList<>();
                        list.add(new Conversion(diagramType, converterClass));
                        map.put(from, list);
                    }

                    if( log.isLoggable( Level.FINE ) )
                        log.log(Level.FINE, "Diagram type converter loaded: " + from + " -> " + to + ", converter=" + converter);
                }
                else if( extensionName.equals(ELEMENT_CONVERSION) )
                {
                    String from = extension.getAttribute(DIAGRAM_TYPE_FROM);
                    if( from == null )
                        throw new Exception("diagram type absents (from)");

                    String to = extension.getAttribute(DIAGRAM_TYPE_TO);
                    if( to == null )
                        throw new Exception("diagram type absents (to)");

                    String name = extension.getAttribute(CONVERTER_NAME); //name can be null

                    Class<? extends DiagramType> toClass = ClassLoading.loadSubClass( to, pluginId, DiagramType.class );
                    String converter = extension.getAttribute(Converter_CLASS);
                    if( converter == null )
                        throw new Exception("converter type absents");

                    Class<? extends DiagramTypeConverter> converterClass = ClassLoading.loadSubClass( converter, pluginId, DiagramTypeConverter.class );

                    if( name != null )
                        converterNameMap.put(name, new Conversion(toClass, converterClass));

                    if( elementConverterMap.containsKey(from) )
                        elementConverterMap.get(from).add(new Conversion(toClass, converterClass));
                    else
                    {
                        List<Conversion> list = new ArrayList<>();
                        list.add(new Conversion(toClass, converterClass));
                        elementConverterMap.put(from, list);
                    }

                    if( log.isLoggable( Level.FINE ) )
                        log.log(Level.FINE, "Diagram node converter loaded: " + from + " -> " + to + ", converter=" + converter);
                }
            }
            catch( Throwable t )
            {
                log.log(Level.SEVERE, "Can not load diagram type converter, extension=" + extension.getName() + ", error: " + t + ".");
            }
        }
    }

    public static Conversion[] getPossibleElementConversions(String diagramType)
    {
        if( elementConverterMap == null )
            loadExtensions("biouml.workbench.diagramTypeConverter");

        if( !elementConverterMap.containsKey(diagramType) )
            return new Conversion[0];

        List<Conversion> list = elementConverterMap.get(diagramType);
        return list.toArray(new Conversion[list.size()]);
    }

    public static void assignConverterToDiagram(Diagram diagram, DiagramTypeConverter converter) throws Exception
    {
        if( converterNameMap == null )
            loadExtensions("biouml.workbench.diagramTypeConverter");
        String converterName = StreamEx.ofKeys( converterNameMap, val -> val.getConverter().equals( converter.getClass() ) )
                .findAny().orElseThrow(
                        () -> new Exception( "Can not assign converter " + converter.getClass().getName() + "to diagram "
                                + diagram.getName() ) );
        diagram.getAttributes().add(new DynamicProperty("converter", String.class, converterName));
    }

    public static boolean checkConverter(Diagram diagram, DiagramTypeConverter converter)
    {
        if( converterNameMap == null )
            loadExtensions("biouml.workbench.diagramTypeConverter");
        Object converterName = diagram.getAttributes().getValue("converter");
        return converterName != null && converterNameMap.containsKey( converterName );
    }

    public static Conversion[] getDiagramElementConverter(Diagram diagram)
    {
        if( converterNameMap == null )
            loadExtensions("biouml.workbench.diagramTypeConverter");
        Object converterName = diagram.getAttributes().getValue("converter");
        if( converterName != null && converterNameMap.containsKey( converterName ) )
            return new Conversion[] {converterNameMap.get(converterName)};
        return getPossibleElementConversions(diagram.getType().getClass().getName());
    }
}
