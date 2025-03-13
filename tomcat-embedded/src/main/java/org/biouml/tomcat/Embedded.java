package org.biouml.tomcat;

import org.apache.catalina.Context;
import org.apache.catalina.loader.WebappLoader;
import org.apache.catalina.startup.Tomcat;
import org.apache.catalina.core.StandardHost;

import java.io.File;
import java.util.Properties;

import java.security.Policy;
import java.security.Permissions;
import javax.management.MBeanTrustPermission;
import java.security.AllPermission;

public class Embedded
{
    public static void main(String[] args) throws Exception 
    {
        Properties properties = System.getProperties();
        System.out.println("properties " + properties );
          
        String warFileOne = properties.getProperty("biouml.war");
        String warFileTwo = properties.getProperty("bioumlweb.war");

        String port = properties.getProperty("biouml.http.port");

        Tomcat tomcat = new Tomcat();
        tomcat.setPort( Integer.valueOf( port ) ); 
        ( ( StandardHost )tomcat.getHost() ).setUnpackWARs(false);

        Context context1 = tomcat.addWebapp("/biouml", new File(warFileOne).getAbsolutePath());
        WebappLoader loader1 = new WebappLoader(Thread.currentThread().getContextClassLoader());
        context1.setLoader(loader1);
        System.out.println("Deploying " + warFileOne + " at /biouml");

        Context context2 = tomcat.addWebapp("/bioumlweb", new File(warFileTwo).getAbsolutePath());
        WebappLoader loader2 = new WebappLoader(Thread.currentThread().getContextClassLoader());
        context2.setLoader(loader2);
        System.out.println("Deploying " + warFileTwo + " at /bioumlweb");

        Permissions perms = new Permissions();
        //perms.add(new MBeanTrustPermission("register"));
        perms.add(new AllPermission());

        // Add other required permissions as necessary

        Policy.setPolicy(new Policy() {
            public java.security.PermissionCollection getPermissions(java.security.CodeSource codesource) {
                return perms;
            }
        });

        if( Runtime.version().feature() <= 11 )
        {
            System.setSecurityManager(new SecurityManager());
        }
        tomcat.start();
        tomcat.getServer().await();
    }
}
