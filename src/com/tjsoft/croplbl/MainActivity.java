package com.tjsoft.croplbl;

import android.app.Activity;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.text.method.DateTimeKeyListener;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.FrameLayout;
import android.widget.Toast;

import com.tjsoft.croplbl.CropperView;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class MainActivity extends Activity {

    private static final int REQUEST_CODE_READ_PERMISSION = 22;
    private static final int REQUEST_GALLERY = 21;
    private static final String TAG = "MainActivity";

    @Bind(R.id.imageview)
    CropperView mImageView;

    private Bitmap mBitmap;
    private boolean isSnappedToCenter = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
            setContentView(R.layout.activity_main_portrait);
        } else {
            Log.i(TAG, "Set landscape mode");
            setContentView(R.layout.activity_main_landscape);
        }
        //ButterKnife.bind(this);
        View root = getWindow().getDecorView().findViewById(R.id.imageview);
        mImageView =  (CropperView) root;//new CropperView(MainActivity.this);
        mImageView.setDebug(true);
    }
    
    public void onClick(View v)
    {
    	switch (v.getId()) {
		case R.id.image_button:
			startGalleryIntent();
			break;
		case R.id.crop_button:
			cropImage();
			break;
		case R.id.rotate_button:
			rotateImage();
			break;
		case R.id.snap_button:
			snapImage();
			break;
		case R.id.gesture_button:
			toggleGestures();
			break;
		case R.id.replace_button:
			replaceImage();
			break;
		}
    }
/*
    @OnClick(R.id.image_button)
    public void onImageButtonClicked() {
        startGalleryIntent();
    }

    @OnClick(R.id.crop_button)
    public void onImageCropClicked() {
        cropImage();
    }

    @OnClick(R.id.rotate_button)
    public void onImageRotateClicked() {
        rotateImage();
    }

    @OnClick(R.id.snap_button)
    public void onImageSnapClicked() {
        snapImage();
    }
*/
    //@OnClick(R.id.gesture_button)
    public void toggleGestures() {
        boolean enabled = mImageView.isGestureEnabled();
        enabled = !enabled;
        mImageView.setGestureEnabled(enabled);
        Toast.makeText(this, "Gesture " + (enabled ? "Enabled" : "Disabled"), Toast.LENGTH_SHORT).show();
    }

    //@OnClick(R.id.replace_button)
    public void replaceImage() {
        if (mBitmap != null) {
            mBitmap = BitmapUtils.rotateBitmap(mBitmap, 180);
            mImageView.replaceBitmap(mBitmap);
        }
    }

    private void loadNewImage(String filePath) {
        Log.i(TAG, "load image: " + filePath);
        mBitmap = BitmapFactory.decodeFile(filePath);
        //mBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.background);
        Log.i(TAG, "bitmap: " + mBitmap.getWidth() + " " + mBitmap.getHeight());

        int maxP = Math.max(mBitmap.getWidth(), mBitmap.getHeight());
        float scale1280 = (float)maxP / 1280;

        if (mImageView.getWidth() != 0) {
            mImageView.setMaxZoom(mImageView.getWidth() * 2 / 1280f);
        } else {

            ViewTreeObserver vto = mImageView.getViewTreeObserver();
            vto.addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
                @Override
                public boolean onPreDraw() {
                    mImageView.getViewTreeObserver().removeOnPreDrawListener(this);
                    mImageView.setMaxZoom(mImageView.getWidth() * 2 / 1280f);
                    return true;
                }
            });

        }

        mBitmap = Bitmap.createScaledBitmap(mBitmap, (int)(mBitmap.getWidth()/scale1280),
                (int)(mBitmap.getHeight()/scale1280), true);
        mImageView.setImageBitmap(mBitmap);
    }

    private void startGalleryIntent() {

        if (!hasGalleryPermission()) {
            askForGalleryPermission();
            return;
        }

        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, REQUEST_GALLERY);
    }

	private boolean hasGalleryPermission() {
        return ActivityCompat.checkSelfPermission(this,
                android.Manifest.permission.READ_EXTERNAL_STORAGE)
                == PackageManager.PERMISSION_GRANTED;
    }

    private void askForGalleryPermission() {
        ActivityCompat.requestPermissions(this,
                new String[]{android.Manifest.permission.READ_EXTERNAL_STORAGE},
                REQUEST_CODE_READ_PERMISSION);
    }

    @Override
    public void onActivityResult(int requestCode, int responseCode, Intent resultIntent) {
        super.onActivityResult(requestCode, responseCode, resultIntent);

        if (responseCode == RESULT_OK) {
            String absPath = BitmapUtils.getFilePathFromUri(this, resultIntent.getData());
            loadNewImage(absPath);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if (requestCode == REQUEST_CODE_READ_PERMISSION) {
            if (grantResults.length == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startGalleryIntent();
                return;
            }
        }

        Toast.makeText(this, "Gallery permission not granted", Toast.LENGTH_SHORT).show();
    }

    private void cropImage() {

        Bitmap bitmap = mImageView.getCroppedBitmap();

        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddhhmmss");
        Date date = new Date();
        String fecha = sdf.format(date);
        
        if (bitmap != null) {

            try {
                BitmapUtils.writeBitmapToFile(bitmap, new File(Environment.getExternalStorageDirectory() + "/crop_test"+fecha+".jpg"), 90);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void rotateImage() {
        if (mBitmap == null) {
            Log.e(TAG, "bitmap is not loaded yet");
            return;
        }

        mBitmap = BitmapUtils.rotateBitmap(mBitmap, 90);
        mImageView.setImageBitmap(mBitmap);
    }

    private void snapImage() {
        if (isSnappedToCenter) {
            mImageView.cropToCenter();
        } else {
            mImageView.fitToCenter();
        }

        isSnappedToCenter = !isSnappedToCenter;
    }
}