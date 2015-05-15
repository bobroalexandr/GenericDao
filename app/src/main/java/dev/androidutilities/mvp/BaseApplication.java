/**
 * Copyright (C) 2015 android10.org. All rights reserved.
 * @author Fernando Cejas (the android10 coder)
 */
package dev.androidutilities.mvp;

import android.app.Application;
import android.util.Log;

import alex.bobro.genericdao.ContextContentProvider;
import alex.bobro.genericdao.GenericDao;
import alex.bobro.genericdao.Scheme;

import dev.androidutilities.model.TestChild1;
import dev.androidutilities.model.TestChild2;
/**
 * Android Main Application
 */
public class BaseApplication extends Application {

    private static BaseApplication instance;

    public static BaseApplication getInstance() {
        return instance;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        Log.i("test!", "BaseApplication");
        Scheme.init(TestChild1.class, TestChild2.class);
        ContextContentProvider testContentProvider = new ContextContentProvider(this);
        GenericDao.init(testContentProvider);
    }

}
