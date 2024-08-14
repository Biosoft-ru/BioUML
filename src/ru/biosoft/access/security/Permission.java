package ru.biosoft.access.security;

import java.util.HashMap;
import java.util.Map;

/**
 * Contains information for access to ProtectedDataCollection
 */
public class Permission
{
    public static final int INFO   = 0b00001;
    public static final int READ   = 0b00010;
    public static final int WRITE  = 0b00100;
    public static final int DELETE = 0b01000;
    public static final int ADMIN  = 0b10000;
    public static final int ALL    = 0b11111;

    private static Map<String, Integer> securityMap = new HashMap<>();
    {
        securityMap.put("getSize", INFO);
        securityMap.put("getDataElementType", INFO);
        securityMap.put("isMutable", INFO);
        securityMap.put("getInfo", INFO | ADMIN);
        securityMap.put("contains", INFO);
        securityMap.put("get", READ);
        securityMap.put("getDescriptor", READ);
        securityMap.put("iterator", READ);
        securityMap.put("getNameList", READ);
        securityMap.put("put", WRITE);
        securityMap.put("remove", WRITE | DELETE);
        securityMap.put("close", WRITE | DELETE);
        securityMap.put("release", WRITE | DELETE);
        securityMap.put("getFromCache", READ | ADMIN);
        securityMap.put("removeFromCache", ADMIN);
        securityMap.put("isAcceptable", READ);
        securityMap.put("reinitialize", ADMIN);
        // TODO: this is a temporary fix for Graph search. "|READ" should be
        // deleted
        securityMap.put("addDataCollectionListener", ADMIN | READ);
        securityMap.put("removeDataCollectionListener", ADMIN | READ);
        securityMap.put("propagateElementWillChange", INFO);
        securityMap.put("propagateElementChanged", INFO);
        securityMap.put("isPropagationEnabled", INFO);
        securityMap.put("setPropagationEnabled", ADMIN | WRITE | DELETE);
        securityMap.put("isNotificationEnabled", INFO);
        securityMap.put("setNotificationEnabled", ADMIN | WRITE | DELETE);
        // ////////////

        securityMap.put("getDisplayName", INFO | ADMIN);
        securityMap.put( "setDisplayName", WRITE);
        securityMap.put("getDescription", INFO | ADMIN);
        securityMap.put("setDescription", WRITE);
        securityMap.put("isVisible", INFO | ADMIN);
        securityMap.put("setVisible", ADMIN);
        securityMap.put("isVisibleChildren", INFO | ADMIN);
        securityMap.put("setVisibleChildren", ADMIN);
        securityMap.put("getNodeImage", INFO | ADMIN);
        securityMap.put("setNodeImage", ADMIN);
        securityMap.put("getChildrenNodeImage", INFO | ADMIN);
        securityMap.put("setChildrenNodeImage", ADMIN);
        securityMap.put("getComparator", INFO | ADMIN);
        securityMap.put("setComparator", ADMIN);
        securityMap.put("isLateChildrenInitialization", INFO | ADMIN);
        securityMap.put("setLateChildrenInitialization", ADMIN);
        securityMap.put("hasError", INFO | ADMIN);
        securityMap.put("getError", INFO | ADMIN);
        securityMap.put("setError", ADMIN);
        securityMap.put("getQuerySystem", INFO | ADMIN);
        securityMap.put("isQuerySystemInitialized", INFO | ADMIN);
        securityMap.put("setQuerySystem", ADMIN);
        securityMap.put("getUsedFiles", INFO | ADMIN);
        securityMap.put("addUsedFiles", ADMIN);
        securityMap.put("getProperties", INFO | ADMIN);
        securityMap.put("getProperty", INFO | ADMIN);
        securityMap.put("writeProperty", WRITE | ADMIN);
    }

    public Permission(String str)
    {
        String[] values = str.split("\n");
        if( values.length != 4 ) throw new IllegalArgumentException("Invalid permission format");
        this.permissions = Integer.parseInt(values[0].trim());
        this.userName = values[1].trim();
        this.sessionId = values[2].trim();
        this.expirationTime = Long.parseLong(values[3].trim()) + System.currentTimeMillis();
    }

    public Permission(int permissions, String userName, String sessionId, long expirationTime)
    {
        this.permissions = permissions;
        this.userName = userName;
        this.sessionId = sessionId;
        this.expirationTime = expirationTime;
    }

    private int permissions;

    public int getPermissions()
    {
        return permissions;
    }
    
    public boolean isMethodAllowed(String method)
    {
        if( securityMap.containsKey(method) )
        {
            return ( securityMap.get(method).intValue() & permissions ) != 0;
        }
        return false;
    }
    
    public boolean isAllowed(int access)
    {
        return access == 0 || (permissions & access) != 0;
    }
    
    public boolean isReadAllowed()
    {
        return isAllowed(READ);
    }

    public boolean isWriteAllowed()
    {
        return isAllowed(WRITE);
    }
    
    public boolean isDeleteAllowed()
    {
        return isAllowed( DELETE );
    }

    public boolean isInfoAllowed()
    {
        return isAllowed(INFO);
    }

    public boolean isAdminAllowed()
    {
        return isAllowed(ADMIN);
    }
    
    private String userName;

    public String getUserName()
    {
        return userName;
    }

    private String sessionId;

    public String getSessionId()
    {
        return sessionId;
    }

    private long expirationTime;

    public long getExpirationTime()
    {
        return expirationTime;
    }

    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append(permissions);
        sb.append('\n');
        sb.append(userName);
        sb.append('\n');
        sb.append(sessionId);
        sb.append('\n');
        sb.append(expirationTime - System.currentTimeMillis());
        return sb.toString();
    }

}
