This is a patched version of OJB 1.0.4.  The files that have changes are:

src/java/org/apache/ojb/broker/platforms/PlatformOracle9iImpl.java
src/java/org/apache/ojb/broker/util/IdentityArrayList.java
src/java/org/apache/ojb/broker/core/PersistenceBrokerAbstractImpl.java
src/java/org/apache/ojb/broker/metadata/DescriptorRepository.java
src/java/org/apache/ojb/broker/core/PersistenceBrokerImpl.java
src/java/org/apache/ojb/broker/accesslayer/JdbcAccessImpl.java

The changes to these file are as follows:


###########################################################################
# PlatformOracle9iImpl.java:                                              #
###########################################################################

Modified to allow for unwrapping of PreparedStatements from XAPool prepared
statements for CLOB support of greater than 4K in Oracle.  This is to allow
for OJB to work properly within the context of a Workflow plugin with
CLOBs.

A patch has been submitted and incorporated into OJB 1.0.5 to fix this
problem in the OJB product.  See the following JIRA:

http://issues.apache.org/jira/browse/OJB-101

Also, it appears there was a bug in unwrapping DBCP connections.  I patched
this to fall back to the database metadata as is done in Spring's
NativeJdbcExtractorAdapter.

http://issues.apache.org/jira/browse/OJB-101


###########################################################################
# IdentityArrayList.java:                                                 #
###########################################################################

Downloaded from the 1.0.5 branch to support the change to
PersistenceBrokerAbstractImpl.  See below.

###########################################################################
# PersistenceBrokerAbstractImpl.java                                      #
###########################################################################

Modified to fix a problem that Jay was having with the GL batch process.
Basically, there was concurrent access by mutliple threads happening
against this class and the arrays inside, causing ArrayIndexOutOfBounds
exceptions.  The multi threading was happening because of a finalize()
method being run in a different thread.  The solution to this was to
synchronize the collections inside and access to the collections.

###########################################################################
src/java/org/apache/ojb/broker/metadata/DescriptorRepository.java
###########################################################################

There was a concurrency issue with a cache of ClassDescriptors held by this class.
One instance of an update was not synchronized.

###########################################################################
src/java/org/apache/ojb/broker/core/PersistenceBrokerImpl.java
###########################################################################

Fixing OJB bug which throws an NPE when deleting objects with a proxied 1-1 contained object.
The original code failed to check the "null-ness" of the object after unwrapping the CGLIB proxy class.

KFSMI-282 : Fixing OJB bug which blanks out the PK fields of the main object when a 1:1 relationship object is null. 
The original code checked the "null-ness" of the object *before* unwrapping the CGLIB proxy class.
 
###########################################################################
src/java/org/apache/ojb/broker/accesslayer/JdbcAccessImpl.java
###########################################################################

Added better OJB execption logging.

###########################################################################
08-20-2010
KULRICE-4281
db-ojb-1.0.4-patch4.jar

src/java/org/apache/ojb/broker/core/PersistenceBrokerImpl.java
###########################################################################

Changed to fix issue with deletion from "super" table as per the following
jira issue in the OJB jira queue:

https://issues.apache.org/jira/browse/OJB-93

###########################################################################
05-11-2011
KULRICE-5135

fixed exception in proxy class when target class has protected setters

src/java/org/apache/ojb/broker/core/proxy/AbstractIndirectionHandler.java
###########################################################################