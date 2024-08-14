package biouml.workbench;

import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import javax.swing.ImageIcon;

import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.repository.RepositoryPane;
import ru.biosoft.access.repository.RepositoryRenderer;
import ru.biosoft.access.security.Permission;
import ru.biosoft.util.IconUtils;
import  biouml.plugins.server.access.ClientModule;

public class DatabaseRenderer extends RepositoryRenderer
{
    public static final String LOCAL_DATABASE_ICON = "localDatabaseIcon.png";
    public static final String REMOTE_PUBLIC_DATABASE_ICON = "remotePublicDatabaseIcon.png";
    public static final String REMOTE_PUBLIC_DATABASE_ICON2 = "remotePublicDatabaseIcon2.png";
    public static final String REMOTE_PUBLIC_READ_DATABASE_ICON = "remotePublicReadDatabaseIcon.png";
    public static final String REMOTE_PROTECTED_DATABASE_ICON = "remoteProtectedDatabaseIcon.png";
    public static final String REMOTE_PROTECTED_DATABASE_ICON2 = "remoteProtectedDatabaseIcon2.png";
    public static final String REMOTE_PROTECTED_READ_DATABASE_ICON = "remoteProtectedReadDatabaseIcon.png";
    public static final String REMOTE_PROTECTED_READ_DATABASE_ICON2 = "remoteProtectedReadDatabaseIcon2.png";
    public static final String REMOTE_NOT_PROTECTED_DATABASE_ICON = "remoteNotProtectedDatabaseIcon.png";

    protected Map<String, ImageIcon> icons = new HashMap<>();

    public DatabaseRenderer(RepositoryPane repositoryPane)
    {
        super(repositoryPane);
    }

    @Override
    protected ImageIcon getIcon(DataElementPath path)
    {
        if( path.getParentPath().optParentCollection() == null )
        {
            ImageIcon icon = null;
            DataElement element = repositoryPane.getForName(path);
            if( element instanceof ClientModule )
            {
                //server collection
                int permission = Permission.WRITE;
                try
                {
                    Permission p = ((ClientModule)element).getPermission();
                    if( p != null )
                    {
                        permission = p.getPermissions();
                    }
                    int protectionStatus = ((ClientModule)element).getProtectionStatus();

                    if( protectionStatus == 0 )
                    {
                        icon = getIcon(REMOTE_NOT_PROTECTED_DATABASE_ICON);
                    }
                    else if( protectionStatus == 1 )
                    {
                        icon = getIcon(REMOTE_PUBLIC_READ_DATABASE_ICON);
                    }
                    else if( protectionStatus == 2 )
                    {
                        if( ( permission & Permission.WRITE ) != 0 )
                        {
                            icon = getIcon(REMOTE_PUBLIC_DATABASE_ICON2);
                        }
                        else
                        {
                            icon = getIcon(REMOTE_PUBLIC_DATABASE_ICON);
                        }
                    }
                    if( protectionStatus == 3 )
                    {
                        if( ( permission & Permission.READ ) != 0 )
                        {
                            icon = getIcon(REMOTE_PROTECTED_READ_DATABASE_ICON2);
                        }
                        else
                        {
                            icon = getIcon(REMOTE_PROTECTED_READ_DATABASE_ICON);
                        }
                    }
                    else if( protectionStatus == 4 )
                    {
                        if( ( permission & Permission.WRITE ) != 0 )
                        {
                            icon = getIcon(REMOTE_PROTECTED_DATABASE_ICON2);
                        }
                        else
                        {
                            icon = getIcon(REMOTE_PROTECTED_DATABASE_ICON);
                        }
                    }
                }
                catch( Exception e )
                {
                    //nothing to do
                }
            }
            else
            {
                //local collection
                icon = getIcon(LOCAL_DATABASE_ICON);
            }

            if( icon != null )
            {
                return icon;
            }
        }

        return super.getIcon(path);
    }

    protected ImageIcon getIcon(String iconName)
    {
        if( icons.containsKey(iconName) )
        {
            return icons.get(iconName);
        }
        else
        {
            URL url = getClass().getResource("resources/" + iconName);
            if( url != null )
            {
                ImageIcon icon = IconUtils.getImageIcon( url );
                icons.put(iconName, icon);
                return icon;
            }
        }
        return null;
    }
}
