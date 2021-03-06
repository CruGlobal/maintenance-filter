Installation instructions
=========================


copy jars into target app's WEB-INF/lib folder:
* h2-1.2.142.jar (only if you are using org.ccci.maintenance.window.db.path below)
* joda-time-1.6.jar
* log4j-1.2.14.jar
* maintenance-filter-api-1-SNAPSHOT.jar
* maintenance-filter-server-1-SNAPSHOT.jar

(Note that versions are correct as of this writing,
but may not be correct by the time you read this.)

Edit the target app's WEB-INF/web.xml file, and add configuration similar to the following:
 ```xml
  <!-- if you have a datasource configured (via the appserver or something), use this: -->
  <context-param>
    <param-name>org.ccci.maintenance.window.datasource</param-name>
    <param-value>java:/jboss/maintenance_filter</param-value> 
    <!-- or whatever jndi location you'd like -->
  </context-param>

  <!-- 
    alternatively, if you want the filter to create and managed a connection pool, this 
    parameter indicates where the h2 local maintenance window database should be created 
  -->
  <context-param>
    <param-name>org.ccci.maintenance.window.db.path</param-name>
    <param-value>working-data/myapp-maintenance</param-value>
    <!-- 
      results in an H2 database file at working-data/myapp-maintenance.h2.db (relative to 
      the working path of the jvm)
    -->
  </context-param>

  <!--
    or, as a third alternative,
    if you want to use an Infinispan cache to store the maintenance window data,
    this parameter indicates where in JNDI the CacheContainer can be located.
  -->
  <context-param>
    <param-name>org.ccci.maintenance.window.infinispan.cache.location</param-name>
    <param-value>java:jboss/infinispan/container/maintenance-filter-windows</param-value>
    <!-- or whatever jndi location you'd like -->
  </context-param>


  <!-- you need to set a shared secret to authenticate the maintenance-filter-controller client -->
  <context-param>
    <param-name>org.ccci.maintenance.window.key</param-name>
    <param-value>7xs2v4pjdve3rfx</param-value>
    <!--  or whatever you'd like -->
  </context-param>


  <servlet>
    <servlet-name>MaintenanceControlServlet</servlet-name>
    <servlet-class>org.ccci.maintenance.MaintenanceControlServlet</servlet-class>
  </servlet>
  
  <servlet-mapping>
    <servlet-name>MaintenanceControlServlet</servlet-name>
    <url-pattern>/maintenanceControl/*</url-pattern>
    <!--  or whatever you'd like -->
  </servlet-mapping>

  <filter>
    <filter-name>MaintenanceFilter</filter-name>
    <filter-class>org.ccci.maintenance.MaintenanceServletFilter</filter-class>
    
    <!-- 
      This parameter indicates which urls should not be blocked by the filter.
      For example, here, any url starting with /ignored/ and
      any url ending with .jpg should not be blocked.
      These are regular expressions, not normal web.xml url mapping expressions.
      Note that the control servlet's url does not need to be listed here;
      it will be bypassed automatically.
    -->
    <init-param>
      <param-name>bypassUrlPatterns</param-name>
      <param-value>
        /ignored/.* ,
        .*\.jpg
      </param-value>
    </init-param>    
  </filter>
  <filter-mapping>
    <filter-name>MaintenanceFilter</filter-name>
    <!-- generally you will want to map to all urls -->
    <url-pattern>/*</url-pattern>
  </filter-mapping>
```


Usage on Tomcat
---------------

If you configure the maintenance filter to set up its own maintenance database
(eg by using `org.ccci.maintenance.window.db.path`),
then be aware that you may see these log lines when you shut down tomcat:
```
SEVERE: The web application [/myapp] appears to have started a thread named [H2 Log Writer MAINTENANCE_FILTER] but has failed to stop it. This is very likely to create a memory leak.
```
I believe this is due to the fact that H2 shuts down somewhat asynchronously,
and its shutdown thread isn't finished by the time Tomcat checks for un-cleaned-up threads.
I am not sure if this causes a webapp-classloader leak or not,
but I don't think it does.
