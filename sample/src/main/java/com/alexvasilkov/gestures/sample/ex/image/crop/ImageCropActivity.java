package com.alexvasilkov.gestures.sample.ex.image.crop;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Matrix;
import androidx.exifinterface.media.ExifInterface;
import android.net.Uri;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.loader.content.CursorLoader;

import com.alexvasilkov.gestures.GestureController;
import com.alexvasilkov.gestures.State;
import com.alexvasilkov.gestures.commons.CropAreaView;
import com.alexvasilkov.gestures.sample.R;
import com.alexvasilkov.gestures.sample.base.BaseActivity;
import com.alexvasilkov.gestures.views.GestureImageView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * This example demonstrates image cropping using {@link CropAreaView} as overlay.
 */
public class ImageCropActivity extends BaseActivity {

    private static final int PAINTING_ID = 1;
    private static final int MAX_GRID_RULES = 5;
    private static final int PICK_IMAGE = 100;

    private GestureImageView imageView;
    private CropAreaView cropView;
    private GestureImageView resultView;
    private TextView btnLoad;
    private TextView btnSave;

    private int gridRulesCount = 2;
    private boolean isDecor = false;

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        CountDownTimer timer = new CountDownTimer(1000, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {

            }

            @Override
            public void onFinish() {
                cropView.setRulesCount(0, 0);
                cropView.setBackColor(Color.WHITE);
            }
        };

        setContentView(R.layout.image_crop_screen);
        getSupportActionBarNotNull().setDisplayHomeAsUpEnabled(true);

        btnLoad = findViewById(R.id.btnLoad);
        btnSave = findViewById(R.id.btnSave);
        imageView = findViewById(R.id.image_crop_viewer);
        imageView.getController().getSettings().setOverzoomFactor(1.0f);
        cropView = findViewById(R.id.image_crop_area);
        cropView.setImageView(imageView);
        cropView.setAspect((float) 9 / 16);
        cropView.setRulesCount(gridRulesCount, gridRulesCount);
        imageView.getController().getSettings().setFlingEnabled(true);
        resultView = findViewById(R.id.image_crop_result);

        initCropOptions();
        imageView.setImageResource(R.drawable.painting_01);

        btnLoad.setOnClickListener(v -> {
            Intent gallery = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.INTERNAL_CONTENT_URI);
            startActivityForResult(gallery, PICK_IMAGE);
        });

        btnSave.setOnClickListener(v -> {
            savePhoto();
        });

        imageView.getController().addOnStateChangeListener(new GestureController.OnStateChangeListener() {
            @Override
            public void onStateChanged(State state) {
                cropView.setRulesCount(2, 2);
                cropView.setBackColor(Color.argb(160, 0, 0, 0));
                Matrix matrix = new Matrix();
                state.get(matrix);
                Log.d("XXX: ", "" + matrix);
                timer.start();
            }

            @Override
            public void onStateReset(State oldState, State newState) {

            }
        });

//        final Painting painting = Painting.list(getResources())[PAINTING_ID];
//        GlideHelper.loadFull(imageView, painting.imageId, painting.thumbId);
    }

    @Override
    public void onBackPressed() {
        if (resultView.getVisibility() == View.VISIBLE) {
            // Return back to crop mode
            imageView.getController().resetState();

            resultView.setImageDrawable(null);
            resultView.setVisibility(View.GONE);
            invalidateOptionsMenu();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);

        if (resultView.getVisibility() != View.VISIBLE) {
            MenuItem crop = menu.add(Menu.NONE, R.id.menu_crop, 0, R.string.menu_crop);
            crop.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
            crop.setIcon(R.drawable.ic_check_white_24dp);
        }

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.menu_crop) {
            // Cropping image within selected area
            Bitmap cropped = imageView.crop();
            if (cropped != null) {
                // Here you can spin off background thread (e.g. AsyncTask) and save cropped bitmap:
                // cropped.compress(Bitmap.CompressFormat.JPEG, 80, outputStream);

                // We'll just show cropped bitmap on the same screen
                resultView.setImageBitmap(cropped);
                resultView.setVisibility(View.VISIBLE);
                invalidateOptionsMenu();
            }
            return true;
        } else {
            return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == PICK_IMAGE) {
                if (data != null) {
                    Uri imageUri = data.getData();
                    try {
                        InputStream inputStream = null;
                        if (imageUri != null) {
                            inputStream = getContentResolver().openInputStream(imageUri);
                        }
                        Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
                        Bitmap rotatedBitmap = getRotateImageIfRequired(bitmap, imageUri);
                        imageView.setImageBitmap(rotatedBitmap);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    private Bitmap rotateImage(Bitmap bitmap, int degree) {
        Matrix matrix = new Matrix();
        matrix.postRotate((float) degree);
        Bitmap rotatedImg = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
        bitmap.recycle();
        return rotatedImg;
    }

    private Bitmap getRotateImageIfRequired(Bitmap bitmap, Uri selectedImageUri) {
        ExifInterface ei = null;
        try {
            ei = new ExifInterface(getPathFormUri(selectedImageUri));
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (ei != null) {
            switch (ei.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
                case ExifInterface.ORIENTATION_ROTATE_90: {
                    return rotateImage(bitmap, 90);
                }
                case ExifInterface.ORIENTATION_ROTATE_180: {
                    return rotateImage(bitmap, 180);
                }
                case ExifInterface.ORIENTATION_ROTATE_270: {
                    return rotateImage(bitmap, 270);
                }
                default: {
                    return bitmap;
                }
            }
        }
        return null;
    }

    private String getPathFormUri(Uri contentUri) {
        String result = "";
        String[] proj = {MediaStore.Images.Media.DATA};
        CursorLoader loader = new CursorLoader(getApplicationContext(), contentUri, proj, null, null, null);
        Cursor cursor = loader.loadInBackground();
        int column_index = 0;
        if (cursor != null) {
            column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
            cursor.moveToFirst();
            result = cursor.getString(column_index);
            cursor.close();
        }
        return result;
    }

    private void savePhoto() {
        Bitmap cropped = imageView.crop();
        if (cropped != null) {
            String root = Environment.getExternalStorageDirectory().getAbsolutePath();
            File myDir = new File(root + "/CropImage");
            myDir.mkdirs();

            String fileName = "Photo.jpg";
            File file = new File(myDir, fileName);
            if (file.exists()) {
                file.delete();
            }
            try {
                FileOutputStream out = new FileOutputStream(file);
                cropped.compress(Bitmap.CompressFormat.JPEG, 100, out);
                out.flush();
                out.close();
                Toast.makeText(getApplicationContext(), "Saved", Toast.LENGTH_SHORT).show();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void initCropOptions() {
//        findViewById(R.id.crop_16_9).setOnClickListener(v -> {
//            cropView.setAspect(16f / 9f);
//            cropView.setRounded(false);
//            cropView.update(true);
//        });
//        findViewById(R.id.crop_1_1).setOnClickListener(v -> {
//            cropView.setAspect(1f);
//            cropView.setRounded(false);
//            cropView.update(true);
//        });
//        findViewById(R.id.crop_orig).setOnClickListener(v -> {
//            cropView.setAspect(CropAreaView.ORIGINAL_ASPECT);
//            cropView.setRounded(false);
//            cropView.update(true);
//        });
//        findViewById(R.id.crop_circle).setOnClickListener(v -> {
//            cropView.setAspect(1f);
//            cropView.setRounded(true);
//            cropView.update(true);
//        });

        findViewById(R.id.crop_add_rules).setOnClickListener(v -> {
            gridRulesCount = (gridRulesCount + 1) % (MAX_GRID_RULES + 1);
            cropView.setRulesCount(gridRulesCount, gridRulesCount);
        });
        findViewById(R.id.crop_reset).setOnClickListener(v ->
                imageView.getController().resetState());
    }

}
