This document explains how to set up a development environment and how to build releases of MercurialEclipse whether for creating a custom version or for contributing to the project.
The document might seem long but its apparent length is only due of detailed instructions. You should be up and running in about 5 to 10 minutes.

# Setting up a development environment
The environment consists of Eclipse and Maven. Steps for both are detailed right below.
* You need an Eclipse instance with the "Eclipse Platform SDK" and "Eclipse SDK" plug-ins. On my installation for MercurialEclipse bugfixing (Eclipse Indigo 3.7R2), these plug-ins are available on the update site at http://download.eclipse.org/eclipse/updates/3.7 . I recommend you associate this Eclipse instance with a specific workspace.
* You need Maven, both as a plug-in and as a command line tool
	Command line tool
		Download the appropriate version for your system from http://maven.apache.org/download.cgi .
		Add the directory to the executable to your system PATH environment variable. Note that on Windows, and if you are used to launching a command line from a running program (for instance, I personally launch terminals/command lines from FreeCommander using a keyboard shortcut), you will need to relaunch your program (FreeCommander in my case): you don't need (at least on Windows 8.1) to restart your system before the new PATH variable is taken into account. If you add the variable among user variables, I believe you'll need to log out and log back in.
	Maven as a plug-in (m2eclipse)
		You can normally download the m2eclipse from Eclipse Marketplace under the name "Maven Integration for Eclipse".
		Alternatively, add the m2eclipse update site ( http://download.eclipse.org/technology/m2e/releases ) to your list of update sites and install it from Help > Install New Software.

# Getting the source code
The source code can be obtained in two ways: manually cloning the repository (at https://bitbucket.org/mercurialeclipse/main/wiki/Home ) or forking the repository. In case you plan to contribute back to the project, the better option is forking the repository, which will allow you to send pull requests. In all cases, I recommend you prefer forking to cloning.

	Log in to your BitBucket account
	Browse to the MercurialEclipse repository or any page
	Click the button/link for the operation (either "Clone" or "Fork") that you've chosen and proceed to complete the operation.
	Import the cloned/forked repository into Eclipse as projects (previous instructions indicate that you would then get two projects named plugin and feature –, but I have com.vectrace.MercurialEclipse – in folder /plugin – and MercurialEclipseFeature in folder /feature).

# Building dependencies
Dependencies have to be cloned and imported also as Maven projects.
They are:
* https://bitbucket.org/aragost/javahg-parent
* https://bitbucket.org/aragost/javahg 
* https://bitbucket.org/aragost/javahg-ext-mq
* https://bitbucket.org/aragost/javahg-ext-rebase
* https://bitbucket.org/nexj/javahg-ext-largefiles

For each of the JavaHg projects, update to the appropriate Mercurial tag for the version that's needed (indicative values are given in the next paragraph)
	   * See /plug-in/pom.xml dependencies section for the current version

For the most recent MercurialEclipse (2.1.0, changeset 81ff16452347 (i.e. r3245 in my local repository), circa 2015-02-06), the tags/changesets/local revision numbers/timestamps are:
* javahg-parent: 0.6
* javahg: 0.7
* javahg-ext-mq: changeset 333855a25734, r52, 2014-01-12 16:48
* javahg-ext-rebase: 0.7
* javahg-ext-largefiles: changeset b088b9998bd4, r3, 2014-01-12 16:47

Install all five JavaHg dependencies into your local Maven repository:
	Run `mvn install` from the working directory. This runs unit tests before installing into Maven
	You may also install the artifacts from within Eclipse
		On a project, right-click > RunAs > "Maven build..."
		Enter "install" in the "Goals" textfield and tick the "Skip Tests" check (I've had to tick that box because of build failures due to the tests).
	(Option) Run `mvn eclipse:eclipse` from the working directory. This makes it easier to use in Eclipse

# Cause MercurialEclipse to use above build dependencies:
	* If you have m2eclipse installed, in your "Run As..." dropdown there will be a 'MercurialEclipse-MavenInit' launch - select the plug-in project in the navigator view and then run this launch.
	* This will download dependencies and generate .classpath and other files
	* As an alternative to running the launch you can run `mvn clean eclipse:eclipse` on the command line
	* If there are Java errors related to Mylyn install the Mylyn plug-in into your Eclipse

# Troubleshooting
* After running any maven command, (including 'MercurialEclipse-MavenInit') refresh the appropriate project in the navigator view.
* If MercurialEclipse doesn't load when debugging but there are no Errors in the problems view, clean the plug-in project.

# Building releases for distribution
Make sure the Problems view (Window > Show View > Problems) in your installation shows no errors. The error "maven-dependency-plug-in (goals "copy-dependencies", "unpack") is not supported by m2e" has no influence on being able to build a release.

Open the feature.xml file of the "feature" project. In my workspace, the name of this project is "MercurialEclipseFeature".

Click the Plug-ins tab in the editor and make sure that the com.vectrace.MercurialEclipse plug-in is listed in the "Plug-ins and Fragments" list. If it's not listed, click the Add... button and start typing the name of the plug-in; it will show up in the filtered list and you'll need to click the OK button.

Create an update site project (File > New > Project > Plug-in Development > Update Site Project). An editor opens on the site.xml file.
	Select the "mercurialeclipse (x.y.z.qualifier)" (where x, y and z are numbers) and click OK. Note that the icon of this item does NOT bear an arrow overlay in the top left corner. The lack of the overlay indicates that the feature you have selected refers to the MercurialEclipseFeature project in your workspace.
	In case you want to offer your/the MercurialEclipse release in a specific category (such as "Version control systems"), create a category (click the New Category button). You choose the contents of both the ID and the Name fields. "Name" refers to what a user accessing your update site will see, and "ID" (this is a surmise) refers to the handle that Eclipse will use to distinguish features that YOU offer through your update site. YOU make up the ID from whatever pieces of information you want. I guess the only constraint is that the ID be unique on each update site.
	Click the Build All button. 

# Building a release for replacing the MercurialEclipse jar file
In case you simply want to replace a jar file, right-click on the com.vectrace.MercurialEclipse project. Click Export... > Plug-in Development > Deployable Plug-ins and Fragments. Click Next, enter a directory path or browse to a directory, then click Finish.

# Contributing to the upstream project
From Eclipse, push your changes to your forked repository (cloned repositories won't cut it here because BitBucket won't be able to link your repository to the "official" repository. DO NOT clone if you intend to send pull requests).

On BitBucket, visit the page of your forked repository.

Click "Create pull requests" in the sidebar menu and proceed to fill the form. Click the "Create pull request" button when you are done.

# Building a specific version of MercurialEclipse
The process is the same as described above. Provided that you've updated all projects to their respective appropriate tags, you should not run into any problems.
