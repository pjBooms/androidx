/*
 * Copyright 2024 The Android Open Source Project
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
// @exportToFramework:skipFile()
package androidx.appsearch.app;

import android.content.Context;
import android.os.UserHandle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;

import java.io.File;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Contains utility methods for Framework implementation of AppSearch.
 *
 * @exportToFramework:hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class JetpackAppSearchEnvironment implements AppSearchEnvironment {

    /**
     * Returns AppSearch directory in the credential encrypted system directory for the given user.
     *
     * <p>This folder should only be accessed after unlock.
     */
    @Override
    @NonNull
    public File getAppSearchDir(@NonNull Context context, @Nullable UserHandle unused) {
        return new File(context.getFilesDir(), "appsearch");
    }

    /** Creates context for the user based on the userHandle. */
    @Override
    @NonNull
    public Context createContextAsUser(@NonNull Context context, @NonNull UserHandle userHandle) {
        return context;
    }

    /** Creates and returns a ThreadPoolExecutor for given parameters. */
    @Override
    @NonNull
    public ExecutorService createExecutorService(
            int corePoolSize,
            int maxConcurrency,
            long keepAliveTime,
            @NonNull TimeUnit unit,
            @NonNull BlockingQueue<Runnable> workQueue,
            int priority) {
        return new ThreadPoolExecutor(
                corePoolSize,
                maxConcurrency,
                keepAliveTime,
                unit,
                workQueue);
    }

    /** Creates and returns an ExecutorService with a single thread. */
    @Override
    @NonNull
    public ExecutorService createSingleThreadExecutor() {
        return Executors.newSingleThreadExecutor();
    }

    /** Creates and returns an Executor with cached thread pools. */
    @Override
    @NonNull
    public ExecutorService createCachedThreadPoolExecutor() {
        return Executors.newCachedThreadPool();
    }

    /**
     * Returns a cache directory for creating temporary files like in case of migrating documents.
     */
    @Override
    @Nullable
    public File getCacheDir(@NonNull Context context) {
        return context.getCacheDir();
    }

    @Override
    public boolean isInfoLoggingEnabled() {
        // INFO logging is enabled by default in Jetpack AppSearch.
        return true;
    }
}
