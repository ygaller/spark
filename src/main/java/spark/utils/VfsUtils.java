/*
 * Copyright 2002-2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package spark.utils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URI;
import java.net.URL;

/**
 * Utility for detecting and accessing JBoss VFS in the classpath.
 * As of Spring 4.0, supports VFS 3.x on JBoss AS 6+ (package {@code org.jboss.vfs})
 * and is in particular compatible with JBoss AS 7 and WildFly 8.
 * <p>Thanks go to Marius Bogoevici for the initial patch.
 * <b>Note:</b> This is an internal class and should not be used outside the framework.
 *
 * @author Costin Leau
 * @author Juergen Hoeller
 * @since 3.0.3
 */
public abstract class VfsUtils {

    private static final String VFS3_PKG = "org.jboss.vfs.";
    private static final String VFS_NAME = "VFS";

    private static Method VFS_METHOD_GET_ROOT_URL = null;
    private static Method VFS_METHOD_GET_ROOT_URI = null;

    private static Method VIRTUAL_FILE_METHOD_EXISTS = null;
    private static Method VIRTUAL_FILE_METHOD_GET_INPUT_STREAM;
    private static Method VIRTUAL_FILE_METHOD_GET_SIZE;
    private static Method VIRTUAL_FILE_METHOD_GET_LAST_MODIFIED;
    private static Method VIRTUAL_FILE_METHOD_TO_URL;
    private static Method VIRTUAL_FILE_METHOD_TO_URI;
    private static Method VIRTUAL_FILE_METHOD_GET_NAME;
    private static Method VIRTUAL_FILE_METHOD_GET_PATH_NAME;
    private static Method VIRTUAL_FILE_METHOD_GET_CHILD;

    public static Class<?> VIRTUAL_FILE_VISITOR_INTERFACE;
    public static Method VIRTUAL_FILE_METHOD_VISIT;

    private static Field VISITOR_ATTRIBUTES_FIELD_RECURSE = null;
    private static Method GET_PHYSICAL_FILE = null;

    static {
        ClassLoader loader = VfsUtils.class.getClassLoader();
        try {
            Class<?> vfsClass = loader.loadClass(VFS3_PKG + VFS_NAME);
            VFS_METHOD_GET_ROOT_URL = ReflectionUtils.findMethod(vfsClass, "getChild", URL.class);
            VFS_METHOD_GET_ROOT_URI = ReflectionUtils.findMethod(vfsClass, "getChild", URI.class);

            Class<?> virtualFile = loader.loadClass(VFS3_PKG + "VirtualFile");
            VIRTUAL_FILE_METHOD_EXISTS = ReflectionUtils.findMethod(virtualFile, "exists");
            VIRTUAL_FILE_METHOD_GET_INPUT_STREAM = ReflectionUtils.findMethod(virtualFile, "openStream");
            VIRTUAL_FILE_METHOD_GET_SIZE = ReflectionUtils.findMethod(virtualFile, "getSize");
            VIRTUAL_FILE_METHOD_GET_LAST_MODIFIED = ReflectionUtils.findMethod(virtualFile, "getLastModified");
            VIRTUAL_FILE_METHOD_TO_URI = ReflectionUtils.findMethod(virtualFile, "toURI");
            VIRTUAL_FILE_METHOD_TO_URL = ReflectionUtils.findMethod(virtualFile, "toURL");
            VIRTUAL_FILE_METHOD_GET_NAME = ReflectionUtils.findMethod(virtualFile, "getName");
            VIRTUAL_FILE_METHOD_GET_PATH_NAME = ReflectionUtils.findMethod(virtualFile, "getPathName");
            GET_PHYSICAL_FILE = ReflectionUtils.findMethod(virtualFile, "getPhysicalFile");
            VIRTUAL_FILE_METHOD_GET_CHILD = ReflectionUtils.findMethod(virtualFile, "getChild", String.class);

            VIRTUAL_FILE_VISITOR_INTERFACE = loader.loadClass(VFS3_PKG + "VirtualFileVisitor");
            VIRTUAL_FILE_METHOD_VISIT = ReflectionUtils.findMethod(virtualFile, "visit", VIRTUAL_FILE_VISITOR_INTERFACE);

            Class<?> visitorAttributesClass = loader.loadClass(VFS3_PKG + "VisitorAttributes");
            VISITOR_ATTRIBUTES_FIELD_RECURSE = ReflectionUtils.findField(visitorAttributesClass, "RECURSE");
        } catch (ClassNotFoundException ex) {
            throw new IllegalStateException("Could not detect JBoss VFS infrastructure", ex);
        }
    }

    public static Object invokeVfsMethod(Method method, Object target, Object... args) throws IOException {
        try {
            return method.invoke(target, args);
        } catch (InvocationTargetException ex) {
            Throwable targetEx = ex.getTargetException();
            if (targetEx instanceof IOException) {
                throw (IOException) targetEx;
            }
            ReflectionUtils.handleInvocationTargetException(ex);
        } catch (Exception ex) {
            ReflectionUtils.handleReflectionException(ex);
        }

        throw new IllegalStateException("Invalid code path reached");
    }

    public static boolean exists(Object vfsResource) {
        try {
            return (Boolean) invokeVfsMethod(VIRTUAL_FILE_METHOD_EXISTS, vfsResource);
        } catch (IOException ex) {
            return false;
        }
    }

    public static boolean isReadable(Object vfsResource) {
        try {
            return ((Long) invokeVfsMethod(VIRTUAL_FILE_METHOD_GET_SIZE, vfsResource) > 0);
        } catch (IOException ex) {
            return false;
        }
    }

    public static long getSize(Object vfsResource) throws IOException {
        return (Long) invokeVfsMethod(VIRTUAL_FILE_METHOD_GET_SIZE, vfsResource);
    }

    public static long getLastModified(Object vfsResource) throws IOException {
        return (Long) invokeVfsMethod(VIRTUAL_FILE_METHOD_GET_LAST_MODIFIED, vfsResource);
    }

    public static InputStream getInputStream(Object vfsResource) throws IOException {
        return (InputStream) invokeVfsMethod(VIRTUAL_FILE_METHOD_GET_INPUT_STREAM, vfsResource);
    }

    public static URL getURL(Object vfsResource) throws IOException {
        return (URL) invokeVfsMethod(VIRTUAL_FILE_METHOD_TO_URL, vfsResource);
    }

    public static URI getURI(Object vfsResource) throws IOException {
        return (URI) invokeVfsMethod(VIRTUAL_FILE_METHOD_TO_URI, vfsResource);
    }

    public static String getName(Object vfsResource) {
        try {
            return (String) invokeVfsMethod(VIRTUAL_FILE_METHOD_GET_NAME, vfsResource);
        } catch (IOException ex) {
            throw new IllegalStateException("Cannot get resource name", ex);
        }
    }

    public static Object getRelative(URL url) throws IOException {
        return invokeVfsMethod(VFS_METHOD_GET_ROOT_URL, null, url);
    }

    public static Object getChild(Object vfsResource, String path) throws IOException {
        return invokeVfsMethod(VIRTUAL_FILE_METHOD_GET_CHILD, vfsResource, path);
    }

    public static File getFile(Object vfsResource) throws IOException {
        return (File) invokeVfsMethod(GET_PHYSICAL_FILE, vfsResource);
    }

    public static Object getRoot(URI url) throws IOException {
        return invokeVfsMethod(VFS_METHOD_GET_ROOT_URI, null, url);
    }

    // public methods used by the support sub-package

    public static Object getRoot(URL url) throws IOException {
        return invokeVfsMethod(VFS_METHOD_GET_ROOT_URL, null, url);
    }

    public static Object doGetVisitorAttribute() {
        return ReflectionUtils.getField(VISITOR_ATTRIBUTES_FIELD_RECURSE, null);
    }

    public static String doGetPath(Object resource) {
        return (String) ReflectionUtils.invokeMethod(VIRTUAL_FILE_METHOD_GET_PATH_NAME, resource);
    }

}
