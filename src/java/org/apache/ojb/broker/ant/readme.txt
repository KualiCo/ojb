=====================================================================

            OJB VerifyMappingsTask Readme.txt				

=====================================================================
	
author:     Daren Drummond
            mailto:daren@softwarearena.com
			
version:    0.9, 07/11/2002


Object/Relational mapping tools can significantly decrease the time to 
implementation large projects because they greatly reduce the amount of 
custom code needed to persist data.  Unfortunately, the mapping layer that 
these tools use to match class fields to database fields has to be maintained 
during the development cycle.  All too often bugs are introduced into the 
system because a change to a database table or class field was not propagated 
all the way through the persistence chain.  Large projects undergoing lots
of uncontrolled changes are especially susceptible to these types of bugs.

By using the Ant OJB VerifyMappingsTask the developer can find mapping errors 
in the OJB repository.xml file during the build process.  Using this Ant task 
will save you (the deveoloper) the trouble of starting up your EJB server, 
deploying and testing your application simply to find basic mapping errors. This
way, your builds can "fail fast" and help you find bugs as soon as they are 
introduced.  Over the lifetime of a project this will save significant 
development time and testing effort.

----------------------------
Using the VerifyMappingsTask
----------------------------
1. Add your jdbcDriver to your ant boot classpath 
   (the classpath you use to start ant)
   
2. Add the OJB jar file to your ant boot classpath
   (the classpath you use to start ant)

3. Add a taskdef tag to your build.xml file to define the VerifyMappingsTask
   (see example below)
   
4. Add the verifymappings task to an ant target (see example below).
   NOTE: Before verifymappings task runs you should have completed:
    a) Set up your database (must be running)
    b) Built all of your classes
    c) Written your repository files.
   		
5. Optionally use the -quiet parameter when running ant to suppress warnings
   and detailed processing messages from the VerifyMappingsTask.

----------------------------
Configuration Attributes
----------------------------

- propertiesFile	

  The path and file name of the to the OJB properties file. Required.

- repositoryFile	

  The path and file name of the to the OJB repository file. Required.
					
- verifyclasspath	

  The classpath for loading the classes specified by the 
  <class-descriptor></class-descriptor> tags in the OJB repository.xml
  file.  Required.

- [ignoreFieldNameCase]  

  A flag indicating if field name case will be considered when 
  searching for table column names. Valid values are "true" or "false".
  Optional.

- [useStrictTypeChecking]

  A flag indicating that stict type checking of database column types will be 
  used when searching for column names. Valid values are "true" or "false".
  If this value is set to "true" then the task will log a warning if the 
  table column jdbc type doesn't match the type specified in the OJB 
  repository field descriptor.  Optional.

- [useXMLValidation]

  A flag indicating that W3c xml validation will be used to verify the OJB 
  repository.xml file.  Valid values are "true" or "false".  Optional.

- [failonerror]

  A flag indicating that this Ant task will throw a BuildException if it 
  encounters any verification errors in the repository.xml file.  In most 
  cases, this will have the effect of stopping the build process.  Valid 
  values are "true" or "false".  Optional.


Optional as a group:

If none of these attributes are specified then the task will try to use the 
jdbc connection information specified in the repository.xml file.  However,
if you specify one of these attributes then you must supply all of them.

- [jdbcDriver]     the fully qualified class name of the jdbc driver you 
                   will use to communicate with the database.   
					
- [url]            the connection url for your jdbc driver.  If your database
                   requires you to specify a schema, then you should specify
                   the schema on the url.
					
- [logon]          the logon account to use for the database.

- [password]       the password to use for the logon.					
					
				

----------------------------
Example XML
----------------------------

<!-- Define the classpath for the build -->
    <path id="runtime-classpath">
        <path refid="compilation-classpath"/>
        <pathelement path="${build.dir}/test/ojb"/>
    </path>
    
<!-- Access the classpath as a property -->
    <property name="runtime.classpath" refid="runtime-classpath"/>

<!-- Define the custom task -->

    <target name="declare" depends="main">
        <taskdef name="verifymappings" classname="org.apache.ojb.broker.ant.VerifyMappingsTask">
            <classpath refid="runtime-classpath" />
        </taskdef>
    </target>

<!-- set the verification options -->
	<target name="verify" depends="declare" description="Verifies the ojb mapping file.">
        	<verifymappings propertiesFile="${build.dir}/test/ojb/OJB.properties" 
        					repositoryFile="${build.dir}/test/ojb/repository.xml" 
        					jdbcDriver="org.hsqldb.jdbcDriver"
        					url="jdbc:hsqldb:target/test/OJB"
        					logon="sa"
        					password="" 
        					ignoreFieldNameCase="true"
        					useStrictTypeChecking="false"
        					verifyclasspath="${runtime.classpath}"
        					useXMLValidation="true"
        					failonerror="true"/>     					
   	</target>  
	
			