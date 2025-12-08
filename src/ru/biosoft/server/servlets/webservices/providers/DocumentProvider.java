package ru.biosoft.server.servlets.webservices.providers;

import java.io.File;
import java.io.IOException;
import ru.biosoft.access.core.CloneableDataElement;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.DataCollectionUtils;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.core.filter.Filter;
import ru.biosoft.access.file.FileDataElement;
import ru.biosoft.access.core.TextDataElement;
import ru.biosoft.access.security.Permission;
import ru.biosoft.server.servlets.webservices.BiosoftWebRequest;
import ru.biosoft.server.servlets.webservices.JSONResponse;
import ru.biosoft.server.servlets.webservices.WebException;
import ru.biosoft.table.RowFilter;
import ru.biosoft.table.TableDataCollection;
import ru.biosoft.util.TextUtil2;
import biouml.model.Diagram;

/**
 * Common methods for the documents
 * @author lan
 */
public class DocumentProvider extends WebJSONProviderSupport
{
    @Override
    public void process(BiosoftWebRequest arguments, JSONResponse response) throws IOException, WebException
    {
        String action = arguments.getAction();
        if(action.equals("save"))
        {
            DataElementPath path = arguments.getDataElementPath().getTargetPath();
            String newPathStr = arguments.get("newPath");
            DataElementPath targetPath = newPathStr == null ? path : DataElementPath.create(newPathStr).getTargetPath();
            String comment = arguments.get("comment");
            String filter = arguments.get( "filter" );
            if(!saveAs(path, targetPath, comment, filter))
            {
                response.sendAdditionalJSON(null);
            } else
            {
                response.sendString("ok");
            }
        }
        else if(action.equals("getcontent"))
        {
            sendContent(arguments.getDataElementPath(), response);
        }
        else if( action.equals("savecontent") )
        {
            saveContent(arguments.getDataElementPath(), arguments.getDataElementPath("newPath"), TextUtil2.stripUnicodeMagic(arguments.getString("content")));
            response.sendString("ok");
            return;
        }
        else
            throw new WebException("EX_QUERY_PARAM_INVALID_VALUE", BiosoftWebRequest.ACTION);
    }

    /**
     * 
     * @param oldPath
     * @param newPath
     * @param comment comment for commit (if history is switched on)
     * @param filter optional filter JS condition
     * @return
     * @throws WebException
     */
    private boolean saveAs(DataElementPath oldPath, DataElementPath newPath, String comment, String filter) throws WebException
    {
        DataElement de = oldPath.optDataElement();
        if(de == null)
        {
            Object bean = WebBeanProvider.getBean(oldPath.toString());
            if(bean instanceof DataElement)
                de = (DataElement)bean;
        }
        if(de == null)
            throw new WebException("EX_QUERY_NO_ELEMENT", oldPath);
        Filter<DataElement> rowFilter = Filter.INCLUDE_ALL_FILTER;
        if(de instanceof DataCollection && filter != null)
        {
            rowFilter = new RowFilter(filter, (DataCollection<?>)de);
        }
        DataCollection<?> parent = newPath.optParentCollection();
        if(parent == null)
            throw new WebException("EX_QUERY_NO_ELEMENT", newPath.getParentPath());
        if(!parent.isMutable())
            throw new WebException("EX_ACCESS_CANNOT_SAVE", newPath.getParentPath(), "access denied");
        de = DataCollectionUtils.fetchPrimaryElement(de, Permission.READ);
        if(!newPath.equals(oldPath))
        {
            if(de instanceof TableDataCollection)
            {
                try
                {
                    de = ((TableDataCollection)de).clone(parent, newPath.getName(), rowFilter);
                }
                catch( Exception e )
                {
                    throw new WebException(e, "EX_ACCESS_CANNOT_COPY", oldPath, newPath, e.getMessage());
                }
            } else if(de instanceof CloneableDataElement)
            {
                try
                {
                    de = ((CloneableDataElement)de).clone(parent, newPath.getName());
                }
                catch (CloneNotSupportedException cns)
                {
                    //Workaround for pending FileDataElement.copy
                    if(de instanceof FileDataElement)
                    {
                        File newFile = DataCollectionUtils.getChildFile( parent, newPath.getName() );
                        try
                        {
                            de = ((FileDataElement) de).cloneWithFile( parent, newPath.getName(), newFile );
                        }
                        catch (CloneNotSupportedException e)
                        {
                            throw new WebException( e, "EX_ACCESS_CANNOT_COPY", oldPath, newPath, e.getMessage() );
                        }
                    }
                }
                catch( Exception e )
                {
                    throw new WebException(e, "EX_ACCESS_CANNOT_COPY", oldPath, newPath, e.getMessage());
                }
            }
            else
                throw new WebException("EX_QUERY_COPY_NOT_SUPPORTED", oldPath);
            if(de == null)
                throw new WebException("EX_ACCESS_CANNOT_COPY", oldPath, newPath, "no result");
        }
        try
        {
            newPath.save(de);
        }
        catch( Exception e )
        {
            throw new WebException(e, "EX_ACCESS_CANNOT_SAVE", newPath.getParentPath(), e.getMessage());
        }
        return true;
    }

    private static void sendContent(DataElementPath path, JSONResponse response) throws WebException, IOException
    {
        DataElement de = path.optDataElement();
        if(!(de instanceof TextDataElement))
            throw new WebException("EX_QUERY_UNSUPPORTED_ELEMENT", path);
        response.sendString(((TextDataElement)de).getContent());
    }

    public static void saveContent(DataElementPath path, DataElementPath newPath, String content) throws WebException
    {
        TextDataElement de;
        DataCollection<DataElement> parent;
        if(!path.equals(newPath))
        {
            try
            {
                parent = newPath.getParentCollection();
                de = (TextDataElement)getDataElement(path, TextDataElement.class).clone(parent, newPath.getName());
            }
            catch( CloneNotSupportedException e )
            {
                throw new WebException(e, "EX_INTERNAL_DURING_ACTION", "Save as");
            }
        } else
        {
            parent = path.getParentCollection();
            de = getDataElement(path, TextDataElement.class);
        }
        if(!parent.isMutable())
        {
            throw new WebException("EX_ACCESS_READ_ONLY", DataElementPath.create(parent));
        }
        de.setContent(content);
        try
        {
            parent.put(de);
        }
        catch( Exception e )
        {
            throw new WebException("EX_ACCESS_CANNOT_SAVE", DataElementPath.create(de), e.getMessage());
        }
    }
}
