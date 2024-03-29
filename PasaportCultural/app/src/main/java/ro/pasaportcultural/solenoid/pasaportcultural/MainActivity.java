package ro.pasaportcultural.solenoid.pasaportcultural;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.text.InputType;
import android.util.Log;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import ro.pasaportcultural.solenoid.pasaportcultural.GalleryActivity;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_LOAD_PHOTO = 1;
    private static final int PIC_CROP = 2;
    private Button mButton;
    private Button saveButton;
    private ImageView mImage;
    private ImageView albumImage;
    private int share = 0;
    private Bitmap shareable;
    private Bitmap selectedImage = null;
    private String descriptionText = "";



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        mButton = (Button) findViewById(R.id.button_add);
        mButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // Code here executes on main thread after user presses button
                if (share == 1) {
                    dispatchShare();
                } else {
                    share = 1;
                    dispatchLoadPhoto();

                }
            }
        });

        mImage = (ImageView) findViewById(R.id.image_view);
        mImage.setOnTouchListener(new View.OnTouchListener() {
            public boolean onTouch(View v, MotionEvent event) {
                final Bitmap overlay = BitmapFactory.decodeResource(getApplicationContext().getResources(),
                        R.mipmap.ic_pc);

                Bitmap bg = Bitmap.createBitmap(selectedImage.getWidth(), selectedImage.getHeight(), selectedImage.getConfig());
                Canvas canvas = new Canvas(bg);
                canvas.drawBitmap(selectedImage, new Matrix(), null);
                int minDimension = Math.min(selectedImage.getWidth(), selectedImage.getHeight()) / 3;
                canvas.drawBitmap(overlay, null, new Rect((int) event.getX(), (int) event.getY(), (int) event.getX() + minDimension, (int) event.getY() + minDimension), null);

                shareable = bg;
                mImage.setImageBitmap(bg);
                return true;
            }
        });

        saveButton = (Button) findViewById(R.id.button_save);
        saveButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // Code here executes on main thread after user presses button
                saveImageToExternalStorage(shareable);
            }
        });

        albumImage = (ImageView) findViewById(R.id.image_album);
        albumImage.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // Code here executes on main thread after user presses button
                Intent intent = new Intent(MainActivity.this, GalleryActivity.class);
                startActivity(intent);
            }
        });
    }

    protected void onActivityResult(int reqCode, int resultCode, Intent data) {
        super.onActivityResult(reqCode, resultCode, data);

        if (reqCode == REQUEST_LOAD_PHOTO) {
            if (data != null) {
                dispatchCrop(data);
                mButton.setText("Trimite poza");
            }
            else {
                mButton.setText("Alege o poza");
                share = 0;
            }
        } else {
            try {
                dispatchPhotoRetrieval(data);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        }
    }

    public boolean saveImageToExternalStorage(Bitmap image) {
        getDescription();
        String fullPath = Environment.getExternalStorageDirectory().getAbsolutePath() + "/PasaportCultural";
        try {
            File dir = new File(fullPath);
            if (!dir.exists()) {
                dir.mkdirs();
            }

            OutputStream fOut = null;
            File file = new File(fullPath, "Image" + DateFormat.getDateTimeInstance().format(new Date()) + ".png");
            file.createNewFile();
            fOut = new FileOutputStream(file);

            // 100 means no compression, the lower you go, the stronger the compression
            image.compress(Bitmap.CompressFormat.PNG, 100, fOut);
            fOut.flush();
            fOut.close();

            ExifInterface exif = new ExifInterface(file.getAbsolutePath());
            exif.setAttribute(ExifInterface.TAG_IMAGE_DESCRIPTION, descriptionText);
            exif.saveAttributes();

            MediaStore.Images.Media.insertImage(this.getContentResolver(), file.getAbsolutePath(), file.getName(), file.getName());
            return true;

        } catch (Exception e) {
            Log.e("saveToExternalStorage()", e.getMessage());
            return false;
        }
    }

    public void dispatchCrop(Intent data) {
        if(data != null) {
            //call the standard crop action intent (the user device may not support it)
            Intent cropIntent = new Intent("com.android.camera.action.CROP");
            //indicate image type and Uri
            cropIntent.setDataAndType(data.getData(), "image/*");
            //set crop properties
            cropIntent.putExtra("crop", "true");
            //retrieve data on return
            cropIntent.putExtra("return-data", true);
            //start the activity - we handle returning in onActivityResult
            startActivityForResult(cropIntent, PIC_CROP);
        }
    }

    public void dispatchPhotoRetrieval(Intent data) throws FileNotFoundException {
        if (data != null) {
            final Uri imageUri = data.getData();
            final InputStream imageStream = getApplicationContext().getContentResolver().openInputStream(imageUri);
            selectedImage = BitmapFactory.decodeStream(imageStream);
            final Bitmap overlay = BitmapFactory.decodeResource(this.getResources(),
                    R.mipmap.ic_pc);

            Bitmap bg = Bitmap.createBitmap(selectedImage.getWidth(), selectedImage.getHeight(), selectedImage.getConfig());
            Canvas canvas = new Canvas(bg);
            canvas.drawBitmap(selectedImage, new Matrix(), null);
            int minDimension = Math.min(selectedImage.getWidth(), selectedImage.getHeight()) / 3;
            canvas.drawBitmap(overlay, null, new Rect(0, 0, minDimension, minDimension), null);

            mImage.setImageBitmap(bg);
            shareable = bg;
            saveButton.setVisibility(View.VISIBLE);

        }
    }

    public void dispatchShare() {
        String bitmapPath = MediaStore.Images.Media.insertImage(getContentResolver(), shareable, "title", null);
        if(bitmapPath != null) {
            Uri bitmapUri = Uri.parse(bitmapPath);
            Intent sendIntent = new Intent();
            sendIntent.setAction(Intent.ACTION_SEND);
            sendIntent.putExtra(Intent.EXTRA_STREAM, bitmapUri);
            sendIntent.setType("text/plain");
            startActivity(sendIntent);
        }
        mButton.setText("Alege o poza");
        share = 0;
    }

    public void dispatchLoadPhoto() {
        Intent photoPickerIntent = new Intent(Intent.ACTION_PICK);
        photoPickerIntent.setType("image/*");
        startActivityForResult(photoPickerIntent, REQUEST_LOAD_PHOTO);
    }

    public void getDescription()
    {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Adauga o descriere imaginii");

        // Set up the input
        final EditText input = new EditText(this);
        // Specify the type of input expected; this, for example, sets the input as a password, and will mask the text
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        builder.setView(input);

        // Set up the buttons
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                descriptionText = input.getText().toString();
               }
        });
        builder.setNegativeButton("Anuleaza", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });
        builder.show();
    }
}