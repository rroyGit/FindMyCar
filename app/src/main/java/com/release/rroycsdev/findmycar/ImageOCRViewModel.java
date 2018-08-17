package com.release.rroycsdev.findmycar;


import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MutableLiveData;
import android.arch.lifecycle.ViewModel;
import android.content.Context;


import java.util.List;

public class ImageOCRViewModel extends ViewModel implements ImageViewModelPresenter {

    private MutableLiveData<List<ImageDataClass>> imageDataClassList;
    private ImageOCRRepository imageOCRRepository;
    private PhotoParser photoParser;



    public ImageOCRViewModel(Context context) {
        imageDataClassList = new MutableLiveData<>();

        imageOCRRepository = new ImageOCRRepository(this, context);
        this.photoParser = imageOCRRepository.getPhotoParser();
    }

    LiveData<List<ImageDataClass>> getOCRImagesDataClass() {
        return imageDataClassList;
    }


    @Override
    public void getImageInfoList(List<ImageDataClass> imageDataClassList) {
        this.imageDataClassList.setValue(imageDataClassList);

    }

    //explicitly call observer's onchange
    public void setLiveDataClassListExplicitly(){
        if(imageDataClassList.getValue()!= null && !imageDataClassList.getValue().isEmpty())
            imageDataClassList.setValue(imageDataClassList.getValue());
    }

    public void callShutdownActiveWorkers(){
        if(imageOCRRepository != null) imageOCRRepository.destroyActiveWorkers();
    }

}
