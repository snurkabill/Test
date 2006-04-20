package com.vectrace.MercurialEclipse;

import org.eclipse.ui.plugin.*;
import org.eclipse.jface.resource.ImageDescriptor;
import org.osgi.framework.BundleContext;

/**
 * The main plugin class to be used in the desktop.
 */
public class MercurialEclipsePlugin extends AbstractUIPlugin {

	//The shared instance.
	private static MercurialEclipsePlugin plugin;
	
	/**
	 * The constructor.
	 */
	public MercurialEclipsePlugin() {
		plugin = this;
//		System.out.println("MercurialEclipsePlugin.MercurialEclipsePlugin()");
	}

	/**
	 * This method is called upon plug-in activation
	 */
	public void start(BundleContext context) throws Exception {
		try {
//			System.out.println("MercurialEclipsePlugin.start()");
			super.start(context);			
		} catch (Exception e) {
			// TODO: handle exception
//			System.out.println("MercurialEclipsePlugin.start() got execption");
			throw e;
		}
	}

	/**
	 * This method is called when the plug-in is stopped
	 */
	public void stop(BundleContext context) throws Exception {
		super.stop(context);
		plugin = null;
	}

	/**
	 * Returns the shared instance.
	 */
	public static MercurialEclipsePlugin getDefault() {
		return plugin;
	}

	/**
	 * Returns an image descriptor for the image file at the given
	 * plug-in relative path.
	 *
	 * @param path the path
	 * @return the image descriptor
	 */
	public static ImageDescriptor getImageDescriptor(String path) {
		return AbstractUIPlugin.imageDescriptorFromPlugin("com.vectrace.MercurialEclipse", path);
	}
}
