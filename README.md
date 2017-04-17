maintenance-filter
==================

A simple java servlet-based utility for restricting access to a webapp when it's undergoing maintenance.


Basic Idea
==========
A simple command-line client is used to to tell one or many servers when a 'maintenance window' is going to occur.  
This includes when the window starts, when it ends, and what message should be displayed in the meantime.  The 
server will store this info in a local file-based store, and at the appropriate time, will automatically start 
displaying the message, thereby shutting off user access to the app.


Other Features
==============
* you can skip the message screen with a query parameter, so it's easy to test the site is working while everyone
  is still locked out
* messages can contain html, which is useful for links to pages with further information
* if only part of your app is really a combination of apps, you can set up specialized filters for the various pieces 
  and disable only one at a time


Requirements
============
* Servlet environment
* Commons-IO
* Joda-Time
* log4j
* H2 (if you don't configure your own datasource)

Usage
=====
see [maintenance-filter-server's readme](/maintenance-filter-server/README.md) for server-side configuration.

Client-Side
1) Build the project using Maven
2) Create a config file (see an example config file [here](/maintenance-filter-controller/src/test/resources/maintenanceWindowUpdate.yml))
3) From the command line, run the maintenance-filter-controller jar (in the target directory) with the config file path as a parameter:
````
java -jar maintenance-filter-controller-1-SNAPSHOT.jar --configurationFile configurationFilePath
````


License
=======
maintenance-filter is released under the MIT license:  http://www.opensource.org/licenses/MIT


