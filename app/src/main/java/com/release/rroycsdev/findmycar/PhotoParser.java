package com.release.rroycsdev.findmycar;


import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.AsyncTask;
import android.provider.MediaStore;
import android.util.Log;

import com.google.android.gms.maps.model.LatLng;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;


public class PhotoParser{

    private Context context;
    private OCRImagesInterface OCRImagesInterfaceListener;
    private static final int REQUEST_LOAD_IMG = 900;
    private Bitmap selectedImage;
    private static String dataPath = "/storage/emulated/0/DCIM/Camera/";
    private static String TAG = "PhotoParser.java ";



    public PhotoParser(Context context, OCRImagesInterface listener) {
        this.context = context;
        OCRImagesInterfaceListener = listener;
    }

    public PhotoParser(Context context) {
        this.context = context;

    }

    public void makeBitmapListRequest(final int numOfPhotoPathsToGet){
        new BitmapOfImagesToDecipher(this, numOfPhotoPathsToGet).execute(getCameraImagesCursor());
    }

    private void requestGallery(){
        //Intent photoPickerIntent = new Intent(Intent.ACTION_PICK);
        //photoPickerIntent.setDataAndType(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, "image/*");
        //mapActivity.startActivityForResult(photoPickerIntent, REQUEST_LOAD_IMG);

    }


    private double getDegMinSecsFromLatLngString(String latLng){

        String[] latLngNum = latLng.split(",");
        int[] first = new int[2];
        int[] second = new int[2];
        int[] third = new int[2];

        first[0] = Integer.parseInt((latLngNum[0].split("/"))[0]);
        first[1] = Integer.parseInt((latLngNum[0].split("/"))[1]);

        second[0] = Integer.parseInt((latLngNum[1].split("/"))[0]);
        second[1] = Integer.parseInt((latLngNum[1].split("/"))[1]);

        third[0] = Integer.parseInt((latLngNum[2].split("/"))[0]);
        third[1] = Integer.parseInt((latLngNum[2].split("/"))[1]);

        //apply formula
        final double degrees = first[0]/first[1];
        final double minutes = second[0]/second[1];
        final double seconds = third[0]/third[1];

        return (degrees + minutes/60 + seconds/3600);
    }

    public LatLng getLatLng(Uri imageUri, String singlePath, boolean mostRecentPath){
        String imagePath;

        if(mostRecentPath) {
            imagePath = getMostRecentImagePath(getCameraImagesCursor());
        } else if(singlePath != null){
            imagePath = singlePath;
        }else{
            imagePath = getPath(context, imageUri);
        }

        if(imagePath == null) return new LatLng(0, 0);

        /*
        String imagePath = "";
        try {
            final InputStream imageStream = context.getContentResolver().openInputStream(imageUri);
            try {
                createTemporalFileFrom(imageStream);
                imagePath = getPath(context, imageUri);
            } catch (IOException e) {
                e.printStackTrace();
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        */

        double lat = 0.0, lng = 0.0;

        try {
            ExifInterface exifInterface = new ExifInterface(imagePath);

            if(exifInterface.getAttribute(ExifInterface.TAG_GPS_LATITUDE) != null &&
                    exifInterface.getAttribute(ExifInterface.TAG_GPS_LONGITUDE) != null) {

                if(exifInterface.getAttribute(ExifInterface.TAG_GPS_LATITUDE_REF).equals("N")){
                    lat = getDegMinSecsFromLatLngString(exifInterface.getAttribute(ExifInterface.TAG_GPS_LATITUDE));
                }else{
                    lat = 0 - getDegMinSecsFromLatLngString(exifInterface.getAttribute(ExifInterface.TAG_GPS_LATITUDE));
                }

                if(exifInterface.getAttribute(ExifInterface.TAG_GPS_LONGITUDE_REF).equals("E")){
                    lng = getDegMinSecsFromLatLngString(exifInterface.getAttribute(ExifInterface.TAG_GPS_LONGITUDE));
                }else {
                    lng = 0 - getDegMinSecsFromLatLngString(exifInterface.getAttribute(ExifInterface.TAG_GPS_LONGITUDE));
                }

            }else {
                return new LatLng(0, 0);
            }

        } catch (IOException e) {
            e.printStackTrace();
        }

        return new LatLng(lat,lng);
    }

    private String getPath(Context context, Uri uri) {
        String result = null;
        String[] proj = {MediaStore.Images.Media.DATA};
        Cursor cursor = context.getContentResolver().query(uri, proj, null, null, null );
        if(cursor != null){
            if (cursor.moveToFirst()) {
                int column_index = cursor.getColumnIndexOrThrow(proj[0]);
                result = cursor.getString(column_index);
            }
            cursor.close();
        }
        if(result == null) {
            result = "Not found";
        }
        return result;

    }

    public void setSelectedImage(Bitmap selectedImage) {
        this.selectedImage = selectedImage;
    }

    public String getImagePathFromInputStreamUri(Uri uri) {
        InputStream inputStream = null;
        String filePath = null;

        if (uri.getAuthority() != null) {
            try {
                inputStream = context. getContentResolver().openInputStream(uri); // context needed
                File photoFile = createTemporalFileFrom(inputStream);

                filePath = photoFile.getPath();

            } catch (FileNotFoundException e) {
                // log
            } catch (IOException e) {
                // log
            }finally {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        return filePath;
    }

    private File createTemporalFileFrom(InputStream inputStream) throws IOException {
        File targetFile = null;

        if (inputStream != null) {
            int read;
            byte[] buffer = new byte[8 * 1024];

            targetFile = createTempFile();
            OutputStream outputStream = new FileOutputStream(targetFile);

            /*
            Bitmap selectedImage = BitmapFactory.decodeStream(inputStream);
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            selectedImage.compress(Bitmap.CompressFormat.JPEG, 40, bytes);
            outputStream.write(bytes.toByteArray());
            */

            while ((read = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, read);
            }
            outputStream.flush();

            try {
                outputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return targetFile;
    }


    private File createTempFile(){
        return new File(context.getExternalCacheDir(), "tempFile.jpg");
    }


    static class BitmapOfImagesToDecipher extends AsyncTask<Cursor, Void,  List<ImageDataClass>>{

        List<ImageDataClass> returnList = new ArrayList<>();
        WeakReference<PhotoParser> weakReference;
        int numOfPhotoPathsToGet;

        BitmapOfImagesToDecipher(PhotoParser photoParser, final int numOfPhotoPathsToGet){
            weakReference = new WeakReference<>(photoParser);
            this.numOfPhotoPathsToGet = numOfPhotoPathsToGet;
        }

        @Override
        protected  List<ImageDataClass> doInBackground(Cursor... cursors) {
            Cursor cursor = cursors[0];

            if(cursor != null){
                while (cursor.moveToNext()){
                    String path = cursor.getString(cursor.getColumnIndex(MediaStore.Images.Media.DATA));

                    if (path.toUpperCase().contains("DCIM/CAMERA") ||
                            path.toUpperCase().contains("DCIM/100ANDRO") ||
                            path.toUpperCase().contains("DCIM/100MEDIA")) {

                        //ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                        //BitmapFactory.decodeFile(path).compress(Bitmap.CompressFormat.PNG,40, byteArrayOutputStream);
                        //Bitmap bitmap = BitmapFactory.decodeByteArray(byteArrayOutputStream.toByteArray(), 0, byteArrayOutputStream.size());
                        Bitmap bitmap = BitmapFactory.decodeFile(path);

                        Bitmap compressedBitmap = null;
                        try {
                            compressedBitmap = decodeAndCompressImage(path);
                        } catch (FileNotFoundException e) {
                            e.printStackTrace();
                        }

                        ImageDataClass imageDataClass = new ImageDataClass(path, bitmap);
                        imageDataClass.setCompressedBitmap(compressedBitmap);
                        returnList.add(imageDataClass);
                        if(returnList.size() == numOfPhotoPathsToGet) break;
                    }
                }
                cursor.close();
            }
            return returnList;
        }

        @Override
        protected void onPostExecute(List<ImageDataClass> myList) {
            super.onPostExecute(myList);
            weakReference.get().OCRImagesInterfaceListener.findImageWithText(myList);
        }

        private Bitmap decodeAndCompressImage(String path) throws FileNotFoundException {
            Bitmap b = null;
            final int IMAGE_MAX_SIZE_HEIGHT = 150;
            final int IMAGE_MAX_SIZE_WIDTH = 100;
            //Decode image size
            BitmapFactory.Options o = new BitmapFactory.Options();
            o.inJustDecodeBounds = true;

            FileInputStream fis = new FileInputStream(path);
            BitmapFactory.decodeStream(fis, null, o);
            try {
                fis.close();
            } catch (IOException e) {
                e.printStackTrace();
            }

            int scale = 1;
            while(o.outWidth/scale/2 >= IMAGE_MAX_SIZE_HEIGHT && o.outHeight/scale/2 >= IMAGE_MAX_SIZE_WIDTH)
                scale*=2;

            //Decode with inSampleSize
            BitmapFactory.Options o2 = new BitmapFactory.Options();
            o2.inSampleSize = scale;
            fis = new FileInputStream(path);
            b = BitmapFactory.decodeStream(fis, null, o2);
            try {
                fis.close();
            } catch (IOException e) {
                e.printStackTrace();
            }

            return b;
        }
    }



    private Cursor getCameraImagesCursor(){
         /*
        File[] files = new File(dataPath).listFiles(new ImageFileFilter());
        for(int i = files.length-1; i >=0; i--) {
            if(files[i].getName().endsWith(".jpg") || files[i].getName().endsWith(".png")) {
                return files[i];
            }
        }
        */
        String[] projection = new String[]{
                MediaStore.Images.ImageColumns._ID,
                MediaStore.Images.ImageColumns.DATA,
                MediaStore.Images.ImageColumns.BUCKET_DISPLAY_NAME,
                MediaStore.Images.ImageColumns.DATE_TAKEN,
                MediaStore.Images.ImageColumns.MIME_TYPE};

        Cursor cursor = context.getContentResolver().query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                projection,
                null,
                null,
                MediaStore.Images.ImageColumns.DATE_MODIFIED + " DESC");

        return cursor;
    }

    private String getMostRecentImagePath(Cursor cursor) {

        if (cursor != null) {

            while (cursor.moveToNext()) {
                String path = cursor.getString(cursor.getColumnIndex(MediaStore.Images.Media.DATA));

                if (path.toUpperCase().contains("DCIM/CAMERA") ||
                        path.toUpperCase().contains("DCIM/100ANDRO") ||
                        path.toUpperCase().contains("DCIM/100MEDIA")) {
                    cursor.close();
                    return path;
                }
            }
            cursor.close();
            return null;
        }
        return null;
    }

    private class ImageFileFilter implements FileFilter {

        @Override
        public boolean accept(File file) {
            if (file.isDirectory()) {
                return true;
            }
            else if (isImageFile(file.getAbsolutePath())) {
                return true;
            }
            return false;
        }
    }

    private boolean isImageFile(String filePath) {
        if (filePath.endsWith(".jpg") || filePath.endsWith(".png"))
        // Add other formats as desired
        {
            return true;
        }
        return false;
    }

}
