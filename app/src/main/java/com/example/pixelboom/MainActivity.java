package com.example.pixelboom;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.ContentUris;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.LayerDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
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
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import cz.msebera.android.httpclient.Header;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;
    // mode0: upscale; mode1: colorization
    private final String[] url = {"https://api.deepai.org/api/torch-srgan",
                                  "https://api.deepai.org/api/colorizer"};
    private final String key = "b439aaca-965f-4372-b29d-4192684ee7eb";

    private Bitmap originBmp = null;
    private Bitmap currentBmp = null;

    @SuppressLint("ResourceAsColor")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.toolbar);

        binding.fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // request permission
                if ((ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission
                        .WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED)
                || (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission
                        .READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED)
                || (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission
                        .INTERNET) != PackageManager.PERMISSION_GRANTED))
                {
                    ActivityCompat.requestPermissions(MainActivity.this, new String[]
                            {Manifest.permission.WRITE_EXTERNAL_STORAGE,
                                    Manifest.permission.READ_EXTERNAL_STORAGE,
                                    Manifest.permission.INTERNET}
                            , 1);
                } else {
                    openAlbum();
                }
            }
        });

        binding.btnColorize.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (currentBmp == null)
                    Toast.makeText(MainActivity.this, "Please select a photo first", Toast.LENGTH_SHORT).show();
                else
                    boom(currentBmp, 1);
            }
        });

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
                // display image
                displayOriginalImage(getActualPath(data));
                // clear background
                binding.imageView.setBackground(null);
            }
        }
    }

    // > 4.4
    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    private String getActualPath(Intent data) {
        String path = null;
        Uri uri = data.getData();
        //根据不同的uri进行不同的解析
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
                    parseItem(item, response);
                    // set current bitmap;
                    currentBmp = imageViewToBmp(binding.imageView);
                    // complete success!
                } catch (IOException | JSONException ignored) {
                    Toast.makeText(MainActivity.this, "Bad luck!", Toast.LENGTH_SHORT).show();
                }
            }
            @Override
            public void onFailure(int statusCode, Header[] headers, Throwable throwable, JSONObject response) {
                if (statusCode == 401) {
                    Toast.makeText(MainActivity.this, "Invalid api-key!", Toast.LENGTH_SHORT).show();
                }
                else {
                    Toast.makeText(MainActivity.this, "Doom!", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    public Bitmap imageViewToBmp(ImageView imageView) {
        LayerDrawable ld = (LayerDrawable) imageView.getDrawable();
        int width = ld.getIntrinsicWidth();
        int height = ld.getIntrinsicHeight();
        Bitmap bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        ld.setBounds(0, 0, width, height);
        ld.draw(new Canvas(bmp));
        return bmp;
    }

    public File bmpToFile(Bitmap bmp) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bmp.compress(Bitmap.CompressFormat.JPEG, 50, baos);
        File file = new File(Environment.getExternalStorageDirectory() + "/temp.jpg");
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

    private void saveToGallery(ImageView imageView){
        BitmapDrawable bitmapDrawable = (BitmapDrawable) imageView.getDrawable();
        Bitmap bitmap = bitmapDrawable.getBitmap();

        FileOutputStream outputStream = null;
        File file = Environment.getExternalStorageDirectory();
        File dir = new File(file.getAbsolutePath() + "/Boom");
        dir.mkdir();

        String filename = String.format("%d.png", System.currentTimeMillis());
        File outFile = new File(dir,filename);
        try {
            outputStream = new FileOutputStream(outFile);
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream);
            outputStream.flush();
            outputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void parseItem(ImageItem item, JSONObject jsonBody) throws IOException, JSONException {
        try {
            item.setUrl(jsonBody.getString("output_url"));
        } catch (JSONException ignored) {}
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
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}