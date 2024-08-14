package ru.biosoft.access;

import java.io.File;
import java.io.FilePermission;
import java.io.IOException;
import java.lang.reflect.ReflectPermission;
import java.security.AccessController;
import java.security.Permission;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.PropertyPermission;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import one.util.streamex.StreamEx;
import ru.biosoft.access.security.CodePrivilege;
import ru.biosoft.access.security.CodePrivilegeType;
import ru.biosoft.access.security.PrivilegedAction;
import ru.biosoft.exception.InternalException;
import ru.biosoft.exception.LoggedClassNotFoundException;
import ru.biosoft.util.TempFileManager;
import ru.biosoft.util.TempFiles;

@CodePrivilege({})
public class BiosoftSecurityManager extends SecurityManager
{
    private static final Logger log = Logger.getLogger( BiosoftSecurityManager.class.getName() );
    private static final CodePrivilegeType[] NO_PRIVILEGES = new CodePrivilegeType[0];
    private static BiosoftSecurityManager instance;
    private final ThreadLocal<StackTraceElement> sandboxThread = new ThreadLocal<>();
    private final ThreadLocal<Boolean> escaped = new ThreadLocal<>();

    private final Map<String, CodePrivilegeType[]> classPrivileges = new ConcurrentHashMap<>();

    private final Set<String> disabledRuntimePermissions = new HashSet<>( Arrays.asList(
            "setSecurityManager", "exitVM", "queuePrintJob", "setFactory", "createSecurityManager",
            "shutdownHooks", "setIO", "getFileSystemAttributes", "setupApplication", "modifyRepository"));

    private final Set<String> allowedPaths = new HashSet<>();
    private String resourcesTempPath;
    private String tmpPath;
    private String tmpPathResolved;
    private String sharedFolderPath;

    public BiosoftSecurityManager()
    {
        if(instance != null)
        {
            throw new InternalError("Already instantiated");
        }
        instance = this;
        String systemRoot = System.getenv().get("SystemRoot");
        if(systemRoot != null)
        {
            allowedPaths.add(new File(systemRoot, "Fonts").getAbsolutePath());
        }
        allowedPaths.add("/var/cache");
        allowedPaths.add("/usr/share/fonts");
        allowedPaths.add("/usr/share/texmf/fonts");
        allowedPaths.add("/usr/share/texlive");
        allowedPaths.add("/usr/X11R6/lib/X11/fonts");
        allowedPaths.add("/var/lib/defoma");
        //Sometimes need access to /usr/lib/jvm/java-6-openjdk-common/jre/lib/resources.jar that is not in java.home
        allowedPaths.add("/usr/lib/jvm");

        allowedPaths.add("/plugins");
        allowedPaths.add("/home/jovyan/plugins");

        allowedPaths.add("/home/jovyan/.cache");
        allowedPaths.add("/home/jovyan/.fontconfig");

        allowedPaths.add("/etc/fedora-release");
        allowedPaths.add("/etc/lsb-release");
        allowedPaths.add("/etc/redhat-release");
        allowedPaths.add("/etc/turbolinux-release");
        allowedPaths.add("/etc/SuSE-release");

        if(ru.biosoft.access.security.SecurityManager.isTestMode())
        {
            try
            {
                allowedPaths.add( new File("../out").getCanonicalPath() );
                allowedPaths.add( new File("../plugins").getCanonicalPath() );
            }
            catch( IOException e )
            {
                throw new InternalException( e );
            }
        }
        String serverPath = System.getProperty("biouml.server.path");
        if( serverPath != null )
        {
            allowedPaths.add(new File(serverPath, "plugins").getAbsolutePath());
        }
        String catalinaHome = System.getProperty("catalina.home");
        if( catalinaHome != null )
        {
            allowedPaths.add((new File(catalinaHome, "webapps")).getAbsolutePath());
            allowedPaths.add((new File(catalinaHome, "endorsed")).getAbsolutePath());
        }
        String jrePath = System.getProperty("java.home");
        if( jrePath != null )
        {
            jrePath = ( new File(jrePath) ).getAbsolutePath();
            int ind = jrePath.lastIndexOf(File.separator);
            if( ind != -1 )
            {
                jrePath = jrePath.substring(0, ind);
            }
            allowedPaths.add(jrePath);
        }
        
        String tmpDirStr = System.getProperty( "java.io.tmpdir", "/tmp" );
        File tmpDir = new File(tmpDirStr);
        tmpPathResolved = tmpPath = tmpDir.getAbsolutePath();
        try
        {
            tmpPathResolved = tmpDir.getCanonicalPath();
        }
        catch( IOException e )
        {
            log.log( Level.WARNING, "Can not resolve " + tmpPathResolved, e );
        }
        
        try
        {
            resourcesTempPath = TempFileManager.getDefaultManager().getResourcesTempDirectory().getCanonicalPath();
        }
        catch( IOException e )
        {
            resourcesTempPath = TempFileManager.getDefaultManager().getResourcesTempDirectory().getAbsolutePath();
        }
        allowedPaths.add(resourcesTempPath);
        StreamEx.of("java.ext.dirs", "java.library.path")
            .flatMap( prop -> StreamEx.split(System.getProperty(prop, ""), File.pathSeparatorChar) )
            .map( File::new ).map( File::getAbsolutePath ).forEach( allowedPaths::add );

        classPrivileges.put(Class.class.getName(), new CodePrivilegeType[] {CodePrivilegeType.REFLECTION});
        // This line is necessary to ensure that CodePrivilege class is loaded before any sandbox code executes.
        // otherwise some tests fall
        classPrivileges.put(getClass().getName(), getClass().getAnnotation(CodePrivilege.class).value());
    }

    public static void runInSandbox(Runnable runnable)
    {
        if( instance == null )
            runnable.run();
        else
            instance.doRunInSandbox( runnable );
    }

    public static Object escapeFromSandbox(PrivilegedAction runnable) throws Exception
    {
        if( instance == null )
            return runnable.run();
        return instance.doEscapeFromSandbox( runnable );
    }

    public static void addAllowedReadPath(String path)
    {
        instance.doAddAllowedReadPath(path);
    }

    public static void setSharedFolder(String path)
    {
        instance.doSetSharedFolder(path);
    }

    private void doRunInSandbox(Runnable runnable)
    {
        sandboxThread.set(new Exception().getStackTrace()[1]);
        try
        {
            runnable.run();
        }
        finally
        {
            sandboxThread.set(null);
        }
    }

    private Object doEscapeFromSandbox(PrivilegedAction runnable) throws Exception
    {
        if( isEscaped() )
        {
            return runnable.run();
        }
        StackTraceElement callerElement = ( new Exception() ).getStackTrace()[2];
        if( callerElement.isNativeMethod() )//Called via reflection
            throw new SecurityException( "Access denied" );
        escaped.set( true );
        try
        {
            return runnable.run();
        }
        finally
        {
            escaped.set( false );
        }
    }

    private void doAddAllowedReadPath(String path)
    {
        if(isSandbox())
            throw new SecurityException("Access denied");
        allowedPaths.add(path);
    }

    private void doSetSharedFolder(String path)
    {
        if(isSandbox())
            throw new SecurityException("Access denied");
        this.sharedFolderPath = path;
    }

    private boolean isSandbox()
    {
        StackTraceElement sandbox = sandboxThread.get();
        return sandbox != null && !isEscaped();
    }

    private boolean isEscaped()
    {
        return escaped.get() != null && escaped.get();
    }

    private boolean isAllowed(CodePrivilegeType privilege)
    {
        StackTraceElement[] trace = new Exception().getStackTrace();
        StackTraceElement sandBoxBound = sandboxThread.get();
        for( StackTraceElement element : trace )
        {
            if( element.equals(sandBoxBound) )
                return false;
            if( element.getClassName().equals(AccessController.class.getName()) )
                return true;
            for( CodePrivilegeType privilegeType : getClassPrivileges(element.getClassName()) )
                if( privilegeType == privilege )
                    return true;
        }
        return false;
    }

    public boolean isFileAllowed(Permission perm, String path)
    {
        String serverPath = System.getProperty("biouml.server.path");
        if( startsWith(path, serverPath) && ( ru.biosoft.access.security.SecurityManager.isAdmin() || isAllowed(CodePrivilegeType.REPOSITORY) ) )
            return true;
        String homePath = System.getProperty("user.home");
        if( homePath != null && startsWith( path, homePath + "/.eclipse") )
            return true;
        if( homePath != null && startsWith( path, homePath + "/.java") )
            return true;
        String currentPath = System.getProperty("user.dir");
        if( startsWith(path, currentPath) )
            return true;
        
        if( startsWith(path, tmpPath) || startsWith( path, tmpPathResolved ) )
            return true;
        
        if( startsWith( path, resourcesTempPath ) && isAllowed( CodePrivilegeType.TEMP_RESOURCES_ACCESS ))
            return true;
        if( sharedFolderPath != null && startsWith( path, sharedFolderPath ) && (isAllowed( CodePrivilegeType.SHARED_FOLDER_ACCESS ) || Boolean.valueOf( System.getProperty( "biouml.node" ) ) ) )
            return true;
        if( perm.getActions().equals("read") )
        {
            for(String allowedPath: allowedPaths)
            {
                if(startsWith(path, allowedPath)) return true;
            }
        }
        log.log( Level.WARNING, "File not allowed (" + perm + "): " + path, new Throwable() );
        return false;
    }

    protected CodePrivilegeType[] getClassPrivileges(String className)
    {
        return classPrivileges.computeIfAbsent( className, cn -> {
                    try
                    {
                        CodePrivilege annotation = ClassLoading.loadClass( className ).getAnnotation(CodePrivilege.class);
                        if( annotation != null )
                        {
                            return annotation.value();
                        }
                    }
                    catch( LoggedClassNotFoundException e )
                    {
                    }
                    return NO_PRIVILEGES;
                });
    }

    private boolean startsWith(String path, String allowedPath)
    {
        if(allowedPath == null)
            return false;
        path = path.replace('\\', '/');
        allowedPath = allowedPath.replace('\\', '/');
        if(!path.startsWith(allowedPath))
            return false;
        if( path.length() == allowedPath.length() || allowedPath.endsWith( "/" ) )
            return true;
        return path.charAt(allowedPath.length()) == '/';
    }

    @Override
    public void checkPermission(Permission perm)
    {
        //super.checkPermission(perm);

        //additional check for JavaScript
        if( isSandbox() )
        {
            if( perm instanceof FilePermission )
            {
                String path = perm.getName();
                try
                {
                    if( isFileAllowed(perm, ( new File(path) ).getCanonicalPath()) )
                        return;
                    if( isFileAllowed(perm, ( new File(path) ).getAbsolutePath()) )
                        return;
                }
                catch( IOException e )
                {
                }
                throw new SecurityException("Access denied to file: " + path);
            }
            else if(perm instanceof PropertyPermission)
            {
                if( perm.getActions().contains("write") && !isAllowed(CodePrivilegeType.SYSTEM))
                    throw new SecurityException("Access denied to change property "+perm.getName());
            }
            else if(perm instanceof ReflectPermission)
            {
                if(perm.getName().equals("suppressAccessChecks") && !isAllowed(CodePrivilegeType.REFLECTION))
                    throw new SecurityException("Access denied to reflection mechanism");
            }
            else if(perm instanceof RuntimePermission)
            {
                String name = perm.getName();
                int pos = name.indexOf(".");
                if(pos >= 0) name = name.substring(0, pos);
                if( disabledRuntimePermissions.contains(name) && !isAllowed(CodePrivilegeType.SYSTEM) )
                    throw new SecurityException( "Access denied to function: "+name );
                if( name.equals("modifyThread") && !isAllowed(CodePrivilegeType.THREAD))
                    throw new SecurityException( "Access denied to function: "+name );
            }
        }
    }

    @Override
    public void checkExec(String cmd)
    {
        if( isSandbox() )
        {
            if(!isAllowed( CodePrivilegeType.LAUNCH ))
                throw new SecurityException("Access denied to launch "+cmd);
        }
        super.checkExec(cmd);
    }

    @Override
    public void checkAccess(ThreadGroup g)
    {
        if( isSandbox() )
        {
            if(ru.biosoft.access.security.SecurityManager.isAdmin() || isAllowed(CodePrivilegeType.THREAD))
                return;
            throw new SecurityException("Access denied to threads manipulation");
        }
    }
}
