package com.rnpro.webview;

import com.facebook.react.ReactPackage;
import com.facebook.react.bridge.JavaScriptModule;
import com.facebook.react.bridge.NativeModule;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.uimanager.ViewManager;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Created by cyy
 * on 18-3-22.
 */

public class WebViewReactPackage implements ReactPackage {


    public List<Class<? extends JavaScriptModule>> createJSModules() {
        return Collections.emptyList();
    }

    @Override
    public List<ViewManager> createViewManagers(ReactApplicationContext reactApplicationContext) {
        return Arrays.<ViewManager>asList(
                new ReactWebViewManager(reactApplicationContext)
        );
    }

    @Override
    public List<NativeModule> createNativeModules(
            ReactApplicationContext reactApplicationContext) {
        return Collections.emptyList();
    }
}

