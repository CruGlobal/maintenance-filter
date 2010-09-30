Installation instructions:

copy jars into target app's WEB-INF/lib folder:
* guava-r07.jar
* h2-1.2.142.jar
* joda-time-1.6.jar
* log4j-1.2.14.jar
* maintenance-filter-api-1.0-SNAPSHOT.jar
* maintenance-filter-server-1.0-SNAPSHOT.jar

(Note that versions are correct as of this writing, but may not be correct by the time you read this.)

Edit the target app's WEB-INF/web.xml file, and add configuration similar to the following:

  <!-- this parameter indicates where the h2 local maintenance window database should be created -->
  <context-param>
    <param-name>org.ccci.maintenance.window.db.path</param-name>
    <param-value>testapp</param-value>
    <!-- results in database file at target/testapp.h2.db -->
  </context-param>
  
  <servlet>
    <servlet-name>MaintenanceControlServlet</servlet-name>
    <servlet-class>org.ccci.maintenance.MaintenanceControlServlet</servlet-class>
  </servlet>
  
  <servlet-mapping>
    <servlet-name>MaintenanceControlServlet</servlet-name>
    <url-pattern>/maintenanceControl/*</url-pattern>
  </servlet-mapping>

  <filter>
    <filter-name>MaintenanceFilter</filter-name>
    <filter-class>org.ccci.maintenance.MaintenanceServletFilter</filter-class>
    
    <!-- 
      This parameter indicates which urls should not be blocked by the filter.
      For example, here, any url starting with /ignored/ and
      any url ending with .jpg should not be blocked.
      These are regular expressions, not normal web.xml url mapping expressions.
      Note that the control servlet's url does not need to be listed here; it will be bypassed automatically.
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
  