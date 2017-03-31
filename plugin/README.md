# Purpose
This document explains how to set up a development environment, how to build releases of MercurialEclipse whether for creating a custom version or for contributing to the project, and how to debug the source code.
The document might seem long but its apparent length is only due to the level of details in the instructions. You should be up and running in about 60 minutes.

File info:

* Author: Amenel Voglozin
* Created: 2015-02-12
* Last revision: 2017-03-25

# Setting up a development environment
The environment consists of Eclipse and Maven. Steps for both are detailed right below.
(Last tested on 2016-06-25 with Eclipse Neon – first release, dated 2016-06-24)

You need:

* an Eclipse instance,
* Maven,
* the current version of MercurialEclipse.

## Eclipse
Update your Eclipse instance with the **Eclipse Platform SDK** and **Eclipse SDK** plug-ins. On my installation for MercurialEclipse development, these plug-ins are available on the [update site](http://download.eclipse.org/eclipse/updates/4.6). I recommend you associate this Eclipse instance with a specific workspace.

## Maven
You also need Maven, either as an Eclipse plug-in or as a command line tool (they are not mutually exclusive).

### Maven as a command line tool

Download the appropriate version for your system from [the Maven download page](http://maven.apache.org/download.cgi]).

Add the directory to the Maven executable to your system PATH environment variable. Note that on Windows, and if you are launching a command line from a running program (for instance, I personally launch terminals/command lines from FreeCommander using a keyboard shortcut), you will need to close and launch again your program (FreeCommander in my case): you don't need (at least on Windows 8.1) to restart your system before the new PATH variable is taken into account.

### Maven as a plug-in

Note: Depending on your version, m2eclipse (a.k.a. m2e) may be already embedded in your Eclipse instance (e.g. Luna 4.4.1 – Eclipse IDE for Java developers).

You can normally download the m2eclipse from Eclipse Marketplace under the name "Maven Integration for Eclipse".

Alternatively, you can add the m2eclipse update site <http://download.eclipse.org/technology/m2e/releases> to your list of update sites and install it from Help > Install New Software.


# Getting the source code
The source code can be obtained in two ways: 

* manually cloning the repository
* or forking the repository.

In case you plan to contribute back to the project, **the better option is forking** the repository, which will allow you to send pull requests. In all cases, I recommend you prefer forking to cloning.

Steps:

* Log in to your BitBucket account
* Browse to the [MercurialEclipse repository](https://bitbucket.org/mercurialeclipse/main/wiki/Home)
* Click the button/link for the operation (either "Clone" or "Fork") that you've chosen and proceed to complete the operation.
* In the **Mercurial Repository Exploring** perspective, add your cloned/forked repository.
* Import the cloned/forked repository into Eclipse (previous instructions indicate that you would then get two projects named `plugin` and `feature` –, but I have `com.vectrace.MercurialEclipse` – in folder /plugin – and `MercurialEclipseFeature` in folder /feature).

Once the import is over, you get 4 projects:

* `plugin` aka com.vectrace.MercurialEclipse (see the note below)
* `MercurialEclipseFeature`
* `MercurialEclipseMaven-feature`
* `MercurialEclipseMaven-plugin`

**NOTE**: Because `plugin` is actually a Maven project, it will only read `com.vectrace.MercurialEclipse` when imported as a Maven project. Therefore, once the import is over and you have the 4 projects listed above, you need to delete the `plugin` project (make sure you do NOT tick the **Delete project contents on disk** checkbox) and reimport the project you've just checked out (right-click in the Package Explorer view > **Import** > **Maven** > **Existing Maven Projects**)

# Building dependencies

## Getting the dependencies
Dependencies also have to be cloned (or forked, if you intend to contribute to the projects) **and imported** as Maven projects (see the note above).
They are:

* <https://bitbucket.org/aragost/javahg-parent>
* <https://bitbucket.org/aragost/javahg>
* <https://bitbucket.org/aragost/javahg-ext-mq>
* <https://bitbucket.org/aragost/javahg-ext-rebase>
* <https://bitbucket.org/nexj/javahg-ext-largefiles>

## Installing the dependencies

### Guidelines
Due to the dependency links and some decisions made by our forerunners, building and installing the dependencies may feel tedious: you have to inspect pom.xml files in a top-down fashion and switch the appropriate project to the appropriate tag before you can build it.

For each of the JavaHg projects, update to the appropriate Mercurial tag for the version that's needed (indicative values are given in the next paragraph). See /plugin/pom.xml dependencies section for the appropriate version numbers. 

Start with `com.vectrace.MercurialEclipse`: determine the required javahg version. Switch javahg to the appropriate tag, then build and install it (see the previous section). If it can't be built, look at the required dependencies and build them first.

For example, for MercurialEclipse 2.1.0 (changeset 81ff16452347 – i.e. r3245 in my local repository –, circa 2015-02-06), the tags/changesets/local revision numbers/timestamps are:

* javahg-parent: 0.6
* javahg: 0.7
* javahg-ext-mq: changeset 333855a25734, r52, 2014-01-12 16:48
* javahg-ext-rebase: 0.7
* javahg-ext-largefiles: changeset b088b9998bd4, r3, 2014-01-12 16:47

As of December 11, 2016, here is the inventory of versions that need to be built and installed: 
* javahg-parent: 0.6, 0.7-snapshot
* javahg: 0.7 (required by javahg extensions), 0.8-snapshot
* javahg-ext-mq: 0.7-snapshot; changeset 333855a25734, r52, 2014-01-12 16:48
* javahg-ext-rebase: 0.7
* javahg-ext-largefiles: 0.1-snapshot, changeset b088b9998bd4, r3, 2014-01-12 16:47

### Build and installation options
Install all five JavaHg dependencies into your local Maven repository (javahg-parent first, then javahg, then the others).

Run `mvn install` from the working directory. This runs unit tests before installing the artifacts into the Maven repository.

You may also install the artifacts from within Eclipse
* On a project, right-click > **Run As** > **Maven build...**
* Enter "install" in the **Goals** textfield and tick the **Skip Tests** checkbox (I've had to tick that box because of build failures due to the tests). 

NOTE: **Starting from JavaHg 0.8, it is no longer necessary to skip tests** for JavaHg tests have been fixed.

Optionally, run `mvn eclipse:eclipse` from the working directory. This makes it easier to use in Eclipse.

# Cause MercurialEclipse to use the dependencies

If you have m2eclipse installed, in your "Run As..." dropdown there will be a **MercurialEclipse-MavenInit** launch - select the plug-in project in the navigator view and then run this launch.

NOTE 1: **If the launch entry is missing from the dropdown menu** (this is for instance the case in Luna 4.4.1 with m2e 1.5.0), select "Organize Favorites..." from the dropdown menu, select 'MercurialEclipse-MavenInit' and click OK. This will download dependencies and generate .classpath and other files.

NOTE 2: **If the launch entry is also missing from the Organize Favorites window** (this is for instance the case in Neon 4.6.2 with m2e 1.7.0), check the Run As dropdown.

As an alternative to running the launch, you can run `mvn clean eclipse:eclipse` on the command line.

If there are Java errors related to Mylyn, install the Mylyn plug-in into your Eclipse instance.

# Building releases for distribution

Make sure the Problems view (Window > Show View > Problems) in your installation shows no errors. Note however that the following error has no influence on being able to build a release:

> maven-dependency-plugin (goals "copy-dependencies", "unpack") is not supported by m2e 

Open the feature.xml file of the "feature" project. In my workspace, the name of this project is "MercurialEclipseFeature".

Click the **Plug-ins** (or **Included Plug-ins**) tab in the editor and make sure that `com.vectrace.MercurialEclipse` is listed in the "Plug-ins and Fragments" list. If it's not listed, click **Add...** and start typing the name of the plug-in; it will show up in the filtered list and you'll need to click **OK**.

Create an update site project (File > New > Project > Plug-in Development > Update Site Project). An editor opens on the site.xml file, with **Site Map** as the active tab.

Select **mercurialeclipse (x.y.z.qualifier)** (where x, y and z are numbers) and click **OK**. Note that the icon of this item must *NOT* bear an arrow overlay in the top left corner. The absence of the overlay indicates that the feature you have selected refers to the MercurialEclipseFeature project in your workspace. The presence of the overlay indicates that the feature refers to the MercurialEclipse feature *installed* in your Eclipse. We want the former.

In case you want to offer your MercurialEclipse release in a specific category (say "Version control systems"), create a category (click **New Category**). You choose the contents of both the **ID** and the **Name** fields. "Name" refers to what a user accessing your update site will see, and "ID" (this is pure surmise) refers to the handle that Eclipse will use to distinguish features that YOU offer through your update site. YOU make up the ID from whatever pieces of information you want or see fit. I guess the only constraint is that the ID be unique on each update site.

Click the **Build All** button.

Once the **Build Site** dialog closes, you can upload the (entire) update site project to a server that suits your distribution needs.

Word of advice: I've found updating/cleaning/refreshing an existing update site project being a tricky task. I always delete the following content when I reuse an existing update site project (the only file left is then `site.xml`):
* content.jar
* features.jar
* all folders.

# Building a release for replacing the MercurialEclipse jar file (Not recommended. Don't do that.)
In case you simply want to replace a jar file, right-click on the com.vectrace.MercurialEclipse project. Click **Export...** > **Plug-in Development** > **Deployable Plug-ins and Fragments**. Click **Next**, enter a directory path or browse to a directory, then click **Finish**. The file will be built and exported to the path and name you've given.

Then, refer to the Eclipse documentation for instructions on how to install the file.

# Contributing to the upstream project (recommended)
From Eclipse, push your changes to your forked repository (cloned repositories won't cut it here because BitBucket won't be able to link your repository to the "official" repository. DO NOT clone if you intend to send pull requests).

On BitBucket, visit the page of your forked repository.

Click **Create pull requests** in the sidebar menu and proceed to fill the form. Check that you can see the official MercurialEclipse repository in a dropdown list; it will be there if you have correctly forked, and not cloned, the source repository. Click the **Create pull request** button when you are done.

# Building a specific version of MercurialEclipse
The process is the same as described earlier in this document for building releases for distribution. Provided that you've updated all projects to their respective appropriate tags, you should not run into any problems.

# Debugging modifications to the source code
This supposes that you have properly set up all projects (the plug-in and its dependencies). For instance, you should have linked files to javahg dependencies appearing at the root of project com.vectrace.MercurialEclipse.

Open the **Debug As...** dropdown menu from the toolbar.

Click **Debug Configurations...**.

In the left panel, select **Eclipse Application** and press the **New** button.

Give the configuration a name so that you can identify it in the **Debug As** or **Run As** dropdown menus.

Fill the fields as you wish (NOTE: in Luna 4.4.1, all default values are adequate ones). Important values are:
	In tab **Main**: **Program to Run** is set to **Run a product** > **org.eclipse.sdk.ide**
	In tab Plug-ins: **Launch with** is set to **all workspace and enabled target plug-ins**

Click the **Debug** button. This launches an additional instance (which we'll refer to as "the debug instance") of Eclipse, but in this instance, the MercurialEclipse plug-in that is installed in your development instance is replaced with the code that you are editing.

Optionally, rename the Java perspective to something more meaningful to you.

Set your breakpoints as you would normally do when debugging any Java application. Any execution in the debug instance of a line of code on which you have set a breakpoint will trigger that breakpoint.

# Troubleshooting

After running any Maven command, (including 'MercurialEclipse-MavenInit') refresh the appropriate project in the navigator view.

## MercurialEclipse doesn't load
If MercurialEclipse doesn't load when debugging but there are no Errors in the problems view, clean the plug-in project.

## Missing tests artifact

NOTE: I recommend to always skip tests when installing dependencies of MercurialEclipse. The reason is that I believe a single failed test prevents Maven from building the correct -tests artifact and I believe some tests are based on parsing of the output of the hg executable. In case the syntax of a parsed string has changed in Mercurial over time, the tests run today are bound to fail. Unless you find out which version of Mercurial was the current one at the time the tag you switched to was released, and install that version, there's no other simple way of installing the artifacts you need now.

If you get an error about a missing tests artifact (this error occurred in my case when switching javahg-ext-rebase to tag 0.5), you'll have to dig into your Maven local repository and fool Maven into thinking that the artifact exists:

### Simpler, faster but dirty solution

* Locate the folder of the missing artifact (e.g. for javahg-ext-rebase:0.5, the missing artifact is javahg-0.5-tests.jar so the folder is <maven repo>/com/aragost/javahg/javahg/0.5) 
* Open that folder
* Copy javahg-0.5.jar to javahg-0.5-tests.jar (Note that tampering with a repository is plain wrong in general but we need to do that in this specific case, **until javahg tests pass**)

### Recommended solution

* Retrieve the artifact ID and version of the missing test jar file from the console message
* On the project that matches the artifact, right-click then select **Run As** > **Maven Build...**
* Enter "jar:test-jar install" in the Goals fields
* DO **NOT** tick "Skip Tests" or the artifact won't be built
* Copy the tests jar into the appropriate folder. This file is normally installed when an "install" goal is specified, but only when tests are not skipped. As explained previously, tests will fail so the tests jar won't be built, etc.

## Build failures

### Folders cannot be deleted
If, when running the MercurialEclipse-MavenInit entry in the **Run Configuration** menu, you get a build failure due to folders that cannot be deleted, chances are that you have an Eclipse Application running. Exit it.

### Javadoc erros
Use `-Dmaven.javadoc.skip=true` when on the command line, or edit the run configuration and add `maven.javadoc.skip=true` in the **VM arguments** textbox of the **JRE** tab.

### Path Must Include project and resource name
This error occurs on the com.vectrace.MercurialEclipse project up until the project starts using its dependencies. Launch the MercurialEclipse-MavenInit run configuration again after refreshing the project. On the second or third build, the error will be removed from the **Problems** view and the project will be clean of errors.

## Class-related exceptions
`Class not found` or `Cannot instantiate class` exceptions when running an Eclipse Application. You need to:

* exit the running debug instance,
* run MercurialEclipse-MavenInit in the development instance,
* manually remove the linked files to javahg jars of irrelevant versions. For some reason (maybe m2e not supporting some maven-dependency-plugin goals), nothing else can rid you of these remnants of previous builds. Unfortunately, the remnants WILL cause class loading problems. You can determine which versions are relevant (and which aren't) by enumerating the tags of the 4 javahg dependencies that you had to switch to.
