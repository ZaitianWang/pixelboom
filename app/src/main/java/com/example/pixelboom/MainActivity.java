package com.example.pixelboom;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.LayerDrawable;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.FileUtils;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.webkit.MimeTypeMap;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.pixelboom.databinding.ActivityMainBinding;

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.JsonHttpResponseHandler;
import com.loopj.android.http.RequestParams;
import com.squareup.picasso.Picasso;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;

import cz.msebera.android.httpclient.Header;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;
    // mode0: upscale; mode1: colorization
    private final String[] url = {"https://api.deepai.org/api/torch-srgan",
            "https://api.deepai.org/api/colorizer"};
    private final String key = "b439aaca-965f-4372-b29d-4192684ee7eb";

    private String originPath;
    private Bitmap originBmp = null;
    private Bitmap currentBmp = null;

    @SuppressLint({"ResourceAsColor", "ClickableViewAccessibility"})
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setSupportActionBar(binding.toolbar);

        // set status bar color
        getWindow().setStatusBarColor(getResources().getColor(R.color.status_bar));
        // set status bar foreground
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        // set nav bar color
        getWindow().setNavigationBarColor(getResources().getColor(R.color.background));
        // set menu bar foreground
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
        }

        // init buttons and view status
        binding.btnUpscale.setEnabled(false);
        binding.btnColorize.setEnabled(false);
        binding.btnSave.setEnabled(false);
        binding.btnSave.hide();
        binding.imageView.setEnabled(false);

        binding.btnAlbum.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // request permission
                // >= Android 11
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    if ((ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED)
                            || (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED)
                            || (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.INTERNET) != PackageManager.PERMISSION_GRANTED)
                            || (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.MANAGE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED)) {
                        ActivityCompat.requestPermissions(MainActivity.this, new String[]{
                                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                                Manifest.permission.READ_EXTERNAL_STORAGE,
                                Manifest.permission.INTERNET,
                                Manifest.permission.MANAGE_EXTERNAL_STORAGE}, 1);
                    }
                    else
                        openAlbum();
                } else {
                    if ((ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED)
                            || (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED)
                            || (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.INTERNET) != PackageManager.PERMISSION_GRANTED)) {
                        ActivityCompat.requestPermissions(MainActivity.this, new String[]{
                                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                                Manifest.permission.READ_EXTERNAL_STORAGE,
                                Manifest.permission.INTERNET}, 1);
                    }
                    else
                        openAlbum();
                }
            }
        });

        binding.btnUpscale.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (currentBmp == null)
                    Toast.makeText(MainActivity.this, "Please select a photo first", Toast.LENGTH_SHORT).show();
                else {
                    // set current bitmap;
                    currentBmp = imageViewToBmp(binding.imageView);
                    disableButtons();
                    boom(currentBmp, 0);
                }
            }
        });

        binding.btnColorize.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (currentBmp == null)
                    Toast.makeText(MainActivity.this, "Please select a photo first", Toast.LENGTH_SHORT).show();
                else {
                    // set current bitmap;
                    currentBmp = imageViewToBmp(binding.imageView);
                    disableButtons();
                    boom(currentBmp, 1);
                }
            }
        });

        binding.btnSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (currentBmp == null)
                    Toast.makeText(MainActivity.this, "Please select a photo first", Toast.LENGTH_SHORT).show();
                else {
                    // set current bitmap;
                    currentBmp = imageViewToBmp(binding.imageView);
                    // save to drive
                    saveToGallery(currentBmp);
                    Toast.makeText(MainActivity.this, "Saved", Toast.LENGTH_SHORT).show();
                }
            }
        });

        binding.imageView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                switch (motionEvent.getAction()) {
                    case MotionEvent.ACTION_DOWN: {
                        currentBmp = imageViewToBmp(binding.imageView);
                        binding.imageView.setImageBitmap(originBmp);
                        break;
                    }
                    case MotionEvent.ACTION_MOVE: {
                        break;
                    }
                    case MotionEvent.ACTION_UP: {
                        binding.imageView.setImageBitmap(currentBmp);
                        break;
                    }
                    default:
                        break;
                }
                return true;
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 1) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED)
                openAlbum();
            else Toast.makeText(MainActivity.this, "Permission denied", Toast.LENGTH_SHORT).show();
        }
    }

    private void openAlbum() {
        Intent intent = new Intent("android.intent.action.GET_CONTENT");
        intent.setType("image/*");
        startActivityForResult(intent, 2);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        // album opened
        if (requestCode == 2) {
            // if return a image
            if (resultCode == RESULT_OK && data != null) {
                // save path of original image
                originPath = getActualPath(data);
                // display image
                displayOriginalImage(originPath);
                // clear background
                binding.imageView.setBackground(null);
                // enable buttons
                binding.btnColorize.setEnabled(true);
                binding.btnUpscale.setEnabled(true);
                binding.btnSave.setEnabled(false);
                binding.btnSave.hide();
            }
        }
    }

    // > Android 4.4
    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    private String getActualPath(Intent data) {
        String path = null;
        Uri uri = data.getData();
        if (DocumentsContract.isDocumentUri(this, uri)) {
            String docId = DocumentsContract.getDocumentId(uri);
            if ("com.android.providers.media.documents".equals(uri.getAuthority())) {
                String id = docId.split(":")[1];
                String selection = MediaStore.Images.Media._ID + "=" + id;
                path = getImagePath(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, selection);
            } else if ("com.android.providers.downloads.documents".equals(uri.getAuthority())) {
                Uri contentUri = ContentUris.withAppendedId(Uri.parse("content://downloads/public_downloads"), Long.valueOf(docId));
                path = getImagePath(contentUri, null);
            }
        } else if ("content".equalsIgnoreCase(uri.getScheme())) {
            path = getImagePath(uri, null);
        } else if ("file".equalsIgnoreCase(uri.getScheme())) {
            path = uri.getPath();
        }
        return path;
    }

    // core function
    private void boom(Bitmap bmp, int mode) {
        final ImageItem item = new ImageItem();
        RequestParams params = new RequestParams();
        File uploadImage = bmpToFile(bmp);
        try {
            params.put("image", uploadImage);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        AsyncHttpClient client = new AsyncHttpClient();
        client.addHeader("api-key", key);
        client.setConnectTimeout(5000);
        client.post(url[mode], params, new JsonHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                try {
                    // extract image from response packet
                    parseItem(item, response);
                    // completely success!
                } catch (IOException | JSONException ignored) {
                    Toast.makeText(MainActivity.this, "Bad luck!", Toast.LENGTH_SHORT).show();
                }
                enableButtons(true);
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, Throwable throwable, JSONObject response) {
                // authentication problem
                if (statusCode == 401) {
                    Toast.makeText(MainActivity.this, "Invalid api-key!\nPlease contact us on GitHub.", Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(MainActivity.this, "Connection failure!(" + statusCode + ")", Toast.LENGTH_SHORT).show();
                }
                enableButtons(false);
            }
        });
    }

    public Bitmap imageViewToBmp(ImageView imageView) {
        // handling LayDrawable
        if (imageView.getDrawable() instanceof LayerDrawable) {
            LayerDrawable ld = (LayerDrawable) imageView.getDrawable();
            int width = ld.getIntrinsicWidth();
            int height = ld.getIntrinsicHeight();
            Bitmap bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            ld.setBounds(0, 0, width, height);
            ld.draw(new Canvas(bmp));
            return bmp;
        }
        // handling BitmapDrawable
        else {
            BitmapDrawable drawable = (BitmapDrawable) imageView.getDrawable();
            return drawable.getBitmap();
        }
    }

    public File bmpToFile(Bitmap bmp) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bmp.compress(Bitmap.CompressFormat.JPEG, 50, baos);
        File file = new File(getApplicationContext().getFilesDir().getAbsolutePath() + "/temp.jpg");
        try {
            file.createNewFile();
            FileOutputStream fos = new FileOutputStream(file);
            InputStream is = new ByteArrayInputStream(baos.toByteArray());
            int x = 0;
            byte[] b = new byte[1024 * 100];
            while ((x = is.read(b)) != -1) {
                fos.write(b, 0, x);
            }
            fos.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return file;
    }

    private void disableButtons() {
        binding.tvHint.setText(R.string.hint_upload);
        AlphaAnimation alp = new AlphaAnimation(0.0f, 1.0f);
        alp.setDuration(500);
        binding.tvHint.setAnimation(alp);
        alp.setAnimationListener(new Animation.AnimationListener() {
            public void onAnimationStart(Animation animation) {
                binding.tvHint.setVisibility(View.INVISIBLE);
            }

            public void onAnimationRepeat(Animation animation) {
            }

            public void onAnimationEnd(Animation animation) {
                binding.tvHint.setVisibility(View.VISIBLE);
            }
        });
        binding.btnUpscale.setEnabled(false);
        binding.btnColorize.setEnabled(false);
        binding.btnSave.setEnabled(false);
        binding.btnSave.hide();
        binding.btnAlbum.setEnabled(false);
        binding.btnAlbum.hide();
    }

    private void enableButtons(boolean success) {
        if (success) {
            binding.tvHint.setText(R.string.hint_download);
            AlphaAnimation alp = new AlphaAnimation(1.0f, 0.0f);
            alp.setStartOffset(2000);
            alp.setDuration(500);
            binding.tvHint.setAnimation(alp);
            alp.setAnimationListener(new Animation.AnimationListener() {
                public void onAnimationStart(Animation animation) {
                }

                public void onAnimationRepeat(Animation animation) {
                }

                public void onAnimationEnd(Animation animation) {
                    binding.tvHint.setVisibility(View.INVISIBLE);
                }
            });
        }
        else {
            binding.tvHint.setVisibility(View.INVISIBLE);
        }
        binding.btnUpscale.setEnabled(true);
        binding.btnColorize.setEnabled(true);
        binding.btnSave.setEnabled(true);
        binding.btnSave.show();
        binding.btnAlbum.setEnabled(true);
        binding.btnAlbum.show();
    }

    private void saveToGallery(Bitmap bmp) {
        int index = originPath.lastIndexOf('.');
        String savePath = originPath.substring(0, index) + "_boom_" + System.currentTimeMillis() + ".jpg";
        FileOutputStream outputStream = null;
        File output = new File(savePath);
        try {
            outputStream = new FileOutputStream(output);
            bmp.compress(Bitmap.CompressFormat.JPEG, 75, outputStream);
            outputStream.flush();
            outputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        // gallery update request
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ContentValues values = new ContentValues();
            values.put(MediaStore.MediaColumns.DISPLAY_NAME, output.getName());
            values.put(MediaStore.MediaColumns.MIME_TYPE, getMimeType(output));
            values.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DCIM);
            ContentResolver contentResolver = getContentResolver();
            Uri uri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
            if (uri == null) {
                return;
            }
            try {
                outputStream = (FileOutputStream) contentResolver.openOutputStream(uri);
                FileInputStream fileInputStream = new FileInputStream(output);
                FileUtils.copy(fileInputStream, outputStream);
                fileInputStream.close();
                outputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            MediaScannerConnection.scanFile(
                    getApplicationContext(),
                    new String[]{output.getAbsolutePath()},
                    new String[]{"image/jpeg"},
                    (path, uri) -> {
                        // Scan Completed
                    });
        }
    }

    public String getMimeType(File file) {
        String suffix = getSuffix(file);
        if (suffix == null) {
            return "file/*";
        }
        String type = MimeTypeMap.getSingleton().getMimeTypeFromExtension(suffix);
        if (type != null || !type.isEmpty()) {
            return type;
        }
        return "file/*";
    }

    private static String getSuffix(File file) {
        if (file == null || !file.exists() || file.isDirectory()) {
            return null;
        }
        String fileName = file.getName();
        if (fileName.equals("") || fileName.endsWith(".")) {
            return null;
        }
        int index = fileName.lastIndexOf(".");
        if (index != -1) {
            return fileName.substring(index + 1).toLowerCase(Locale.US);
        } else {
            return null;
        }
    }

    private void parseItem(ImageItem item, JSONObject jsonBody) throws IOException, JSONException {
        try {
            item.setUrl(jsonBody.getString("output_url"));
        } catch (JSONException ignored) {
        }
        Picasso.get().load(item.getUrl()).placeholder(R.drawable.progress_animation).into(binding.imageView);
    }

    @SuppressLint("Range")
    private String getImagePath(Uri uri, String selection) {
        String path = null;
        Cursor cursor = getContentResolver().query(uri, null, selection, null, null);
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                path = cursor.getString(cursor.getColumnIndex(MediaStore.Images.Media.DATA));
            }
            cursor.close();
        }
        return path;
    }

    // display original image
    private void displayOriginalImage(String imagePath) {
        if (imagePath != null) {
            originBmp = currentBmp = BitmapFactory.decodeFile(imagePath);
            binding.imageView.setEnabled(true);
            binding.imageView.setImageBitmap(originBmp);
        } else {
            Toast.makeText(this, "Fail to access", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        //noinspection SimplifiableIfStatement
        String about = "Pixel Boom is designed for image superresolution " +
                "and colorization.\n\n" +
                "Development Team:\n" +
                "  @JerryZhangZZY\n" +
                "  @ZaitianWang\n" +
                "GitHub Repo: pixelboom\n\n"+
                "Powered by DeepAI API.";
        if (id == R.id.action_about) {
            AlertDialog alertDialog1 = new AlertDialog.Builder(this)
                    .setTitle("Pixel Boom")
                    .setMessage(about)
                    .setIcon(R.mipmap.ic_launcher)
                    .setPositiveButton("Go to GitHub", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            Uri uri = Uri.parse("https://github.com/ZaitianWang/pixelboom");
                            Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                            startActivity(intent);
                        }
                    })
                    .setNeutralButton("Go to DeepAI", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            Uri uri = Uri.parse("https://deepai.org/apis");
                            Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                            startActivity(intent);
                        }
                    })
                    .create();
            alertDialog1.show();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}