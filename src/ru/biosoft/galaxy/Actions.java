package ru.biosoft.galaxy;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import ru.biosoft.galaxy.filters.DataTable;
import ru.biosoft.galaxy.filters.DataTablesPool;
import ru.biosoft.galaxy.parameters.FileParameter;
import ru.biosoft.galaxy.parameters.MetaParameter;
import ru.biosoft.galaxy.parameters.Parameter;
import ru.biosoft.util.XmlStream;
import ru.biosoft.util.XmlUtil;

public class Actions
{
    private static final Logger log = Logger.getLogger( Actions.class.getName() );
    private final Element xml;

    private static final Object STATIC_LOCK = new Object();
    private Object lock = STATIC_LOCK;

    public Actions(Element xml)
    {
        this.xml = xml;
        Document owner = xml.getOwnerDocument();
        if(owner != null)
            lock = owner;
    }

    /**
     * Apply actions to target parameter in the context of params.
     *
     * @param target
     * @param params
     */
    public void apply(FileParameter target, ParametersContainer params)
    {
        synchronized( lock )
        {
            for( Element action : getActionList( xml, params ) )
                try
                {
                    applyAction( action, target, params );
                }
                catch( Exception e )
                {
                    log.log(Level.SEVERE,  "Can not apply action", e );
                }
        }
    }

    private static List<Element> getActionList(Element root, ParametersContainer params)
    {
        List<Element> result = XmlStream.elements( root, "action" ).toList();
        for( Element e : XmlUtil.elements( root, "conditional" ) )
        {
            if( !e.hasAttribute( "name" ) )
                throw new RuntimeException( "No 'name' attribute in 'conditional' tag" );
            String name = e.getAttribute( "name" );
            Parameter p = params.getParameterBySimplePath( name );
            if( p == null )
                throw new RuntimeException( "Parameter '" + name + "' not found" );
            String value = p.toString();
            XmlStream.elements( e, "when" )
                .findFirst( whenElement -> whenElement.getAttribute( "value" ).equals( value ) )
                .ifPresent( whenElement -> result.addAll( getActionList( whenElement, params ) ));
        }
        return result;
    }

    private void applyAction(Element action, Parameter target, ParametersContainer params)
    {
        if( !action.hasAttribute( "type" ) )
            throw new RuntimeException( "No 'type' attribute in 'action' tag" );
        String actionType = action.getAttribute( "type" );
        switch( actionType )
        {
            case "metadata":
                if( !action.hasAttribute( "name" ) )
                    throw new RuntimeException( "No 'name' attribute in 'action' tag" );
                String name = action.getAttribute( "name" );
                Element option = XmlUtil.getChildElement( action, "option" );
                if( option == null )
                    throw new RuntimeException( "No 'option' child in 'action' tag" );
                String value = getOptionValue( option, params );
                target.getMetadata().put( name, new MetaParameter( name, value, "", "" ) );
                break;
            case "format":
                option = XmlUtil.getChildElement( action, "option" );
                if( option == null )
                    throw new RuntimeException( "No 'option' child in 'action' tag" );
                value = getOptionValue( option, params );
                target.getAttributes().put( "format", value );
                break;
            default:
                throw new RuntimeException( "Unknown action type '" + actionType + "'" );
        }
    }

    private String getOptionValue(Element option, ParametersContainer params)
    {
        if( !option.hasAttribute( "type" ) )
            throw new RuntimeException( "No 'type' attribute in 'option' tag" );
        String optionType = option.getAttribute( "type" );
        switch( optionType )
        {
            case "from_param":
                if( !option.hasAttribute( "name" ) )
                    throw new RuntimeException( "No 'name' attribute in 'option' tag" );
                String paramName = option.getAttribute( "name" );
                if( !option.hasAttribute( "param_attribute" ) )
                    throw new RuntimeException( "No 'param_attribute' in 'option' tag" );
                String paramAttrib = option.getAttribute( "param_attribute" );
                Parameter param = params.getParameterBySimplePath( paramName );
                if( param == null )
                    throw new RuntimeException( "Parameter '" + paramName + "' not found" );
                if( !param.getMetadata().containsKey( paramAttrib ) )
                    throw new RuntimeException( "Parameter '" + paramName + "' doesn't have '" + paramAttrib + "' metadta" );
                return param.getMetadata().get( paramAttrib ).getValue().toString();
            case "from_data_table":
                if( !option.hasAttribute( "name" ) )
                    throw new RuntimeException( "No 'name' attribute in 'option' tag" );
                String tableName = option.getAttribute( "name" );

                if( !option.hasAttribute( "column" ) )
                    throw new RuntimeException( "No 'column' attribute in 'option' tag" );
                int column = Integer.parseInt( option.getAttribute( "column" ) );

                DataTable dataTable = DataTablesPool.getDataTable( tableName );
                if( dataTable == null )
                    throw new RuntimeException( "Table '" + tableName + "' not found" );

                for( String[] row : dataTable.getContent() )
                {
                    boolean validRow = true;
                    for( Element filter : XmlUtil.elements( option, "filter" ) )
                        if( filter.getAttribute( "type" ).equals( "param_value" ) && filter.hasAttribute( "ref" ) )
                        {
                            String ref = filter.getAttribute( "ref" );
                            Parameter p = params.getParameterBySimplePath( ref );
                            if( p == null )
                                throw new RuntimeException( "Parameter '" + ref + "' not found" );
                            int filterColumn = filter.hasAttribute( "column" ) ? Integer.parseInt( filter.getAttribute( "column" ) ) : 0;
                            String value = p.toString();
                            if( !row[filterColumn].equals( value ) )
                            {
                                validRow = false;
                                break;
                            }
                        }
                    if( !validRow )
                        continue;
                    return row[column];
                }
                throw new RuntimeException( "No valid rows found in " + tableName );
            default:
                throw new RuntimeException( "Unknown option type '" + optionType );
        }
    }

}
