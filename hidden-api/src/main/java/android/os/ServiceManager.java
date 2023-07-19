/*
 * Copyright (C) 2007 The Android Open Source Project
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
package android.os;

import java.util.Map;

/**
 * @hide
 */
public final class ServiceManager {
    public static IBinder getService(String name) {
        throw new RuntimeException("STUB");
    }

    /**
     * Returns a reference to a service with the given name, or throws
     * {@link NullPointerException} if none is found.
     *
     * @hide
     */
    public static IBinder getServiceOrThrow(String name) throws ServiceNotFoundException {
        throw new RuntimeException("STUB");
    }

    /**
     * Place a new @a service called @a name into the service
     * manager.
     *
     * @param name    the name of the new service
     * @param service the service object
     */
    public static void addService(String name, IBinder service) {
        throw new RuntimeException("STUB");
    }

    /**
     * Place a new @a service called @a name into the service
     * manager.
     *
     * @param name          the name of the new service
     * @param service       the service object
     * @param allowIsolated set to true to allow isolated sandboxed processes
     *                      to access this service
     */
    public static void addService(String name, IBinder service, boolean allowIsolated) {
        throw new RuntimeException("STUB");
    }

    /**
     * Retrieve an existing service called @a name from the
     * service manager.  Non-blocking.
     */
    public static IBinder checkService(String name) {
        throw new RuntimeException("STUB");
    }

    /**
     * Return a list of all currently running services.
     *
     * @return an array of all currently running services, or <code>null</code> in
     * case of an exception
     */
    public static String[] listServices() {
        throw new RuntimeException("STUB");
    }

    /**
     * This is only intended to be called when the process is first being brought
     * up and bound by the activity manager. There is only one thread in the process
     * at that time, so no locking is done.
     *
     * @param cache the cache of service references
     * @hide
     */
    public static void initServiceCache(Map<String, IBinder> cache) {
        throw new RuntimeException("STUB");
    }

    /**
     * Exception thrown when no service published for given name. This might be
     * thrown early during boot before certain services have published
     * themselves.
     *
     * @hide
     */
    public static class ServiceNotFoundException extends Exception {
        public ServiceNotFoundException(String name) {
            super("No service published for: " + name);
        }
    }
}