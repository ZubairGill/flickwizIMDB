package org.xululabs.flickwiz.service;

import org.fusesource.hawtjni.runtime.Library;

public class Loader {
	public static String version = "2.4.11"; 
	private static Object initializeLock = new Object();
	private static boolean initialized = false;

	public static void init() {
		init(Loader.class.getClassLoader());
	}

	public static void init(ClassLoader loader) {
		synchronized (initializeLock) {
			if (!initialized) {
				Library library = new Library("opencv_java", version, loader);
				library.load();

				initialized = true;
			}
		}
	}
}
