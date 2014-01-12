// Copyright 2014 NexJ Systems Inc. This software is licensed under the terms of the Eclipse Public License 1.0

package com.nexj.oss.MercurialEclipseMaven;

import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

public class MercurialEclipseMavenPlugin extends AbstractUIPlugin
{

   // The shared instance.
   private static MercurialEclipseMavenPlugin plugin;
   
   @Override
   public void start(BundleContext context) throws Exception
   {
      super.start(context);
      plugin = this;
   }

   /**
    * Returns the shared instance.
    */
   public static MercurialEclipseMavenPlugin getDefault() {
      return plugin;
   }
}
