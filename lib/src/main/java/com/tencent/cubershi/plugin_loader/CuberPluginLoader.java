package com.tencent.cubershi.plugin_loader;

import android.content.Context;

import com.tencent.cubershi.plugin_loader.blocs.CreateApplicationBloc;
import com.tencent.cubershi.plugin_loader.blocs.LoadApkBloc;
import com.tencent.cubershi.plugin_loader.blocs.ParsePluginApkBloc;
import com.tencent.cubershi.plugin_loader.infos.ApkInfo;
import com.tencent.cubershi.plugin_loader.mocks.MockApplication;
import com.tencent.cubershi.plugin_loader.test.FakeRunningPlugin;
import com.tencent.hydevteam.common.progress.ProgressFuture;
import com.tencent.hydevteam.common.progress.ProgressFutureImpl;
import com.tencent.hydevteam.pluginframework.installedplugin.InstalledPlugin;
import com.tencent.hydevteam.pluginframework.plugincontainer.DelegateProvider;
import com.tencent.hydevteam.pluginframework.plugincontainer.HostActivityDelegate;
import com.tencent.hydevteam.pluginframework.plugincontainer.HostActivityDelegator;
import com.tencent.hydevteam.pluginframework.plugincontainer.HostServiceDelegate;
import com.tencent.hydevteam.pluginframework.plugincontainer.HostServiceDelegator;
import com.tencent.hydevteam.pluginframework.pluginloader.LoadPluginException;
import com.tencent.hydevteam.pluginframework.pluginloader.PluginLoader;
import com.tencent.hydevteam.pluginframework.pluginloader.RunningPlugin;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class CuberPluginLoader implements PluginLoader, DelegateProvider {
    private static final Logger mLogger = LoggerFactory.getLogger(CuberPluginLoader.class);

    private ExecutorService mExecutorService = Executors.newSingleThreadExecutor();

    @Override
    public ProgressFuture<RunningPlugin> loadPlugin(final Context context, final InstalledPlugin installedPlugin) throws LoadPluginException {
        if (mLogger.isInfoEnabled()) {
            mLogger.info("loadPlugin installedPlugin=={}", installedPlugin);
        }
        if (installedPlugin.pluginFile != null && installedPlugin.pluginFile.exists()) {
            final Future<RunningPlugin> submit = mExecutorService.submit(new Callable<RunningPlugin>() {
                @Override
                public RunningPlugin call() throws Exception {
                    final ApkInfo apkInfo = ParsePluginApkBloc.parse(installedPlugin.pluginFile);
                    final ClassLoader pluginClassLoader = LoadApkBloc.load(context.getClass().getClassLoader(), installedPlugin.pluginFile);
                    final MockApplication mockApplication = CreateApplicationBloc.callPluginApplicationOnCreate(pluginClassLoader, apkInfo.getApplicationClassName());
                    return new FakeRunningPlugin(mockApplication, installedPlugin);
                }
            });
            return new ProgressFutureImpl<>(submit, null);
        } else if (installedPlugin.pluginFile != null)
            throw new LoadPluginException("插件文件不存在.pluginFile==" + installedPlugin.pluginFile.getAbsolutePath());
        else throw new LoadPluginException("pluginFile==null");

    }

    @Override
    public boolean setPluginDisabled(InstalledPlugin installedPlugin) {
        return false;
    }

    @Override
    public HostActivityDelegate getHostActivityDelegate(Class<? extends HostActivityDelegator> aClass) {
        return null;
    }

    @Override
    public HostServiceDelegate getHostServiceDelegate(Class<? extends HostServiceDelegator> aClass) {
        return null;
    }
}