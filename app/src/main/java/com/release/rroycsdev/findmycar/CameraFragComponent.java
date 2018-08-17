package com.release.rroycsdev.findmycar;

import android.content.Context;

import dagger.Component;

@Component (modules = ContextModule.class)
public interface CameraFragComponent {

    Context getContext();

}
