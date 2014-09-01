package org.apache.mesos;

/**
 * Created by flaviusi on 28.08.2014.
 */
public class MesosNativeLibrary {
    /**
     * Attempts to load the native library (if it was not previously loaded)
     * from the given path. If the path is null 'java.library.path' is used to
     * load the library.
     */
    public static void load(String path) {
        // Our JNI library will actually set 'loaded' to true once it is
        // loaded, that way the library can get loaded by a user via
        // 'System.load' in the event that they want to specify an
        // absolute path and we won't try and reload the library ourselves
        // (which would probably fail because 'java.library.path' might
        // not be set).
        if (loaded) {
            return;
        }

        // In some circumstances, such as when sandboxed class loaders are used,
        // the current thread's context class loader will not be able to see
        // MesosNativeLibrary (even when executing this code!).
        // We therefore, temporarily swap the thread's context class loader with
        // the class loader that loaded this class, for the duration of the native
        // library load.
        ClassLoader contextClassLoader =
                Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(
                MesosNativeLibrary.class.getClassLoader());
        try {
            if (path != null) {
                System.load(path);
            } else {
                // TODO(tillt): Change the default fallback to JNI specific library
                // once libmesos has been split.
                System.loadLibrary("mesos");
            }
        } catch (UnsatisfiedLinkError error) {
            System.out.println("[WARN] Failed to load native Mesos library from " +
                    (path != null ? path : System.getProperty("java.library.path")));
            throw error;
        } finally {
            Thread.currentThread().setContextClassLoader(contextClassLoader);
        }
    }

    public static void load() {
        // Try to get the JNI specific library path from the environment.
        String path = System.getenv("MESOS_NATIVE_JAVA_LIBRARY");

        // As a fallback, use deprecated environment variable to extract that path.
        if (path == null) {
            path = System.getenv("MESOS_NATIVE_LIBRARY");
            if (path != null) {
                System.out.println("Warning: MESOS_NATIVE_LIBRARY is deprecated, " +
                        "use MESOS_NATIVE_JAVA_LIBRARY instead. Future releases will " +
                        "not support JNI bindings via MESOS_NATIVE_LIBRARY.");
            }
        }

        load(path);
    }

    public static final String VERSION = "0.19.0";

    private static boolean loaded = false;
}
