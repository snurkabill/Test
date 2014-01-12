
# Developer setup

MercurialEclipse uses Maven. MercurialEclipse depends on JavaHg and extensions for JavaHg.

1. Install maven
    * Install the command line tool
    * Install the Maven Eclipse plugin - m2eclipse 

2. Get the source code
    * Clone MercurialEclipse into your workspace as two projects - one for /feature and one for /plugin 
    * Clone JavaHg projects
        * https://bitbucket.org/aragost/javahg-parent
        * https://bitbucket.org/aragost/javahg 
        * https://bitbucket.org/aragost/javahg-ext-mq
        * https://bitbucket.org/aragost/javahg-ext-rebase
        * https://bitbucket.org/nexj/javahg-ext-largefiles
        
3. Install JavaHg dependencies into local Maven repository: for each of the JavaHg projects:
	* Update to the appropriate Mercurial tag for the version that's needed
	   * See /plugin/pom.xml dependencies section for the current version
	* Run `mvn install` from the working directory. This runs unit tests before installing into Maven
	* (Option) Run `mvn eclipse:eclipse` from the working directory. This makes it easier to use in Eclipse

3. Cause MercurialEclipse to use above build dependencies:
    * If you have m2eclipse installed, in your run configuration dropdown there will be 
      a 'MercurialEclipse-MavenInit' launch - select the plugin project in the navigator view and 
      then run this launch.
    * This will download dependencies and generate .classpath and other files
    * As an alternative to running the launch you can run `mvn clean eclipse:eclipse` on the command line
    * If there are Java errors related to Mylyn install the Mylyn plugin into your Eclipse
    
# Troubleshooting
  
* After running any maven command, (including 'MercurialEclipse-MavenInit') refresh the appropriate 
  project in the navigator view.
    
# Building releases
  
1. Update to the appropriate tag
2. Re-run the `MercurialEclipse-MavenInit` launch. 
    * If the build fails due to missing dependent versions (if the version is not uploaded to the central 
      maven repository) then do the following.
        * Update to the appropriate tag of the missing javahg dependency
        * Invoke the install maven task
        * Re-run the `MercurialEclipse-MavenInit` launch.
