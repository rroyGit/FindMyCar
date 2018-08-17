package com.release.rroycsdev.findmycar;

import android.content.Context;

import dagger.Module;
import dagger.Provides;

@Module
public class ContextModule {
    private final Context context;

    ContextModule(Context context){
        this.context = context;
    }

    @Provides
    Context context(){
        return context;
    }
}
