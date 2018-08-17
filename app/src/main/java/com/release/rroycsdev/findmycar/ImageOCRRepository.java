package com.release.rroycsdev.findmycar;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.util.Log;
import android.util.SparseArray;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.vision.Frame;
import com.google.android.gms.vision.text.TextBlock;
import com.google.android.gms.vision.text.TextRecognizer;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import dagger.Provides;

public class ImageOCRRepository implements OCRImagesInterface {

    private static int NUM_OF_PHOTO_PATHS = 10;
    private static int NUM_OF_OCR_DETECT_PER_THREAD = 3;

    private ImageViewModelPresenter imageViewModelPresenterListener;
    private PhotoParser photoParser;
    private Context context;
    private static ExecutorService executorService;
    private FindImageWithRequestedText findImageWithRequestedText;

    ImageOCRRepository(ImageViewModelPresenter imageViewModelPresenterListener, Context context){
        this.imageViewModelPresenterListener = imageViewModelPresenterListener;
        this.context = context;

        this.photoParser = new PhotoParser(context, this);
        photoParser.makeBitmapListRequest(NUM_OF_PHOTO_PATHS);
    }

    public PhotoParser getPhotoParser(){
        return photoParser;
    }

    @Override
    public void findImageWithText(final List<ImageDataClass> myList) {
        NUM_OF_PHOTO_PATHS = myList.size();
        if(NUM_OF_PHOTO_PATHS <= NUM_OF_OCR_DETECT_PER_THREAD) NUM_OF_OCR_DETECT_PER_THREAD = NUM_OF_PHOTO_PATHS;
        TextRecognizer textRecognizer = new TextRecognizer.Builder(context).build();

        findImageWithRequestedText = new FindImageWithRequestedText(this, textRecognizer);
        //noinspection unchecked
        findImageWithRequestedText.execute(myList);
    }


    static class FindImageWithRequestedText extends AsyncTask<List<ImageDataClass>, Void, List<ImageDataClass>> {

        TextRecognizer textRecognizer;
        WeakReference<ImageOCRRepository> weakReference;

        FindImageWithRequestedText(ImageOCRRepository context, TextRecognizer textRecognizer){
            this.textRecognizer = textRecognizer;
            weakReference = new WeakReference<>(context);
        }

        @SafeVarargs
        @Override
        protected final List<ImageDataClass> doInBackground(List<ImageDataClass>... myLists) {
            List<ImageDataClass> myLocalImageList = myLists[0];

            int numOfThreads, remainderImagesToDecipher;
            if(NUM_OF_PHOTO_PATHS % NUM_OF_OCR_DETECT_PER_THREAD == 0){
                numOfThreads = NUM_OF_PHOTO_PATHS / NUM_OF_OCR_DETECT_PER_THREAD;
                remainderImagesToDecipher = 0;
            }else{
                numOfThreads = (NUM_OF_PHOTO_PATHS / NUM_OF_OCR_DETECT_PER_THREAD)+1;
                remainderImagesToDecipher = NUM_OF_PHOTO_PATHS % NUM_OF_OCR_DETECT_PER_THREAD;
            }

            //executorService = Executors.newFixedThreadPool(numOfThreads);
            executorService = Executors.newCachedThreadPool();

            int time[] = new int[numOfThreads];
            for(int i = 0; i < numOfThreads; i++){
                time[i] = i * NUM_OF_OCR_DETECT_PER_THREAD;
            }

            int start, end;
            for (int i = 0; i < numOfThreads; i++) {
                if(remainderImagesToDecipher > 0 && i == numOfThreads-1) {
                    start = time[i];
                    end = time[i] + remainderImagesToDecipher;
                }else{
                    start = time[i];
                    end = time[i] + NUM_OF_OCR_DETECT_PER_THREAD;
                }

                Runnable worker = new TextDetectWorkerThread(myLocalImageList,
                        textRecognizer, start, end);
                executorService.execute(worker);
            }

            try {
                //must shutdown service after execute and before await
                executorService.shutdown();
                executorService.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
            } catch (InterruptedException e) {
                Log.d("CameraFragment: ", "Error threads joining");
            }finally {
                textRecognizer.release();
            }
            return myLocalImageList;
        }

        @Override
        protected void onPostExecute(List<ImageDataClass> myReturnedList) {
            super.onPostExecute(myReturnedList);
            String stringToFind = weakReference.get().getSavedCarImageText();

            int sizeOfStringToFind = stringToFind.length();
            List<ImageDataClass> possibleImages = new ArrayList<>();
            ArrayList<String> mySets = new ArrayList<>();

            for(int i = 0; i < sizeOfStringToFind; i++){
                for(int j = i; j < sizeOfStringToFind; j++){
                    mySets.add(stringToFind.substring(i, j+1));
                }
            }

            try {
                for (ImageDataClass item : myReturnedList) {
                    if (item.getText().contains(stringToFind)) {
                        possibleImages.add(0, item);
                        continue;
                    }
                    for (int i = 0; i < mySets.size(); i++) {
                        if (mySets.get(i).length() > 1 && item.getText().toUpperCase().contains(mySets.get(i).toUpperCase())) {
                            if (!possibleImages.contains(item)) possibleImages.add(item);
                        }
                    }
                }
            }catch (Exception exception ){
                exception.printStackTrace();
            }

            weakReference.get().insertLatLngFromPath(possibleImages);
        }
    }

    private String getSavedCarImageText(){
        SharedPreferences sharedPreferences = context.getSharedPreferences("CarImageText", Context.MODE_PRIVATE);
       return sharedPreferences.getString("myText", "");
    }


    private void insertLatLngFromPath(List<ImageDataClass> imageDataClassList){
        for(ImageDataClass item: imageDataClassList){
           item.setLatLng(photoParser.getLatLng(null,item.getPath(),false));
        }
        imageViewModelPresenterListener.getImageInfoList(imageDataClassList);
    }

    private static class TextDetectWorkerThread implements Runnable{

        List<ImageDataClass> list;
        TextRecognizer textRecognizer;
        int starting, ending;

        public TextDetectWorkerThread(List<ImageDataClass> list, TextRecognizer textRecognizer,
                                      final int starting, final int ending) {
            this.list = list;
            this.textRecognizer = textRecognizer;
            this.starting = starting;
            this.ending = ending;
        }

        @Override
        public void run() {
            for(int i = starting; i < ending; i++){
                Frame outputFrame = new Frame.Builder().setBitmap(list.get(i).getBitmap()).build();
                final SparseArray<TextBlock> myTexts = textRecognizer.detect(outputFrame);
                final int myTextSize = myTexts.size();

                final StringBuilder textOfImageStringBuilder = new StringBuilder();

                for(int j = 0; j < myTextSize; j++){
                    TextBlock block = myTexts.get(j);
                    if(block != null){
                        textOfImageStringBuilder.append(block.getValue()).append('\n');
                    }
                }
                textOfImageStringBuilder.append("");
                list.get(i).setText(textOfImageStringBuilder.toString());
            }
        }
    }

    public void destroyActiveWorkers(){
        if(findImageWithRequestedText!= null && !findImageWithRequestedText.isCancelled())
            findImageWithRequestedText.cancel(true);
        if(executorService != null) {
            executorService.shutdownNow();
        }
    }
}
