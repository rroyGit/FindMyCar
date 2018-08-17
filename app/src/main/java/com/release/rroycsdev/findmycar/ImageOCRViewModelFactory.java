package com.release.rroycsdev.findmycar;


import android.arch.lifecycle.ViewModel;
import android.arch.lifecycle.ViewModelProvider;
import android.content.Context;
import android.support.annotation.NonNull;

public class ImageOCRViewModelFactory implements ViewModelProvider.Factory {

    Context context;
    ImageOCRViewModelFactory(Context context){
        this.context = context;
    }

    @NonNull
    @Override
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
           return (T) new ImageOCRViewModel(context);
    }
}
