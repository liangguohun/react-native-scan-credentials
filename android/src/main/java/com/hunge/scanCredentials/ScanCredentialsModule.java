
package com.hunge.scanCredentials;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Environment;
import android.util.Log;

import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.Callback;
import com.googlecode.tesseract.android.TessBaseAPI;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.Arrays;
import java.util.List;


/**
 * 2、getFilesDir().getAbsolutePath() = /data/user/0/packname/files
 这个方法是获取某个应用在内部存储中的files路径
 3、getCacheDir().getAbsolutePath() = /data/user/0/packname/cache
 这个方法是获取某个应用在内部存储中的cache路径
 4、getDir(“myFile”, MODE_PRIVATE).getAbsolutePath() = /data/user/0/packname/app_myFile
 */
public class ScanCredentialsModule extends ReactContextBaseJavaModule {

  private final ReactApplicationContext reactContext;
  private static final String TAG = "error";
  private static final String lang = "eng";
  private static String DATA_PATH = "";
  private static final String TESSDATA = "tessdata";

  public ScanCredentialsModule(ReactApplicationContext reactContext) {
    super(reactContext);
    this.reactContext = reactContext;
      DATA_PATH = Environment.getExternalStorageDirectory() + File.separator + reactContext.getString(R.string.app_name);
      copyfile(DATA_PATH, "tessdata/"+lang+".traineddata");
  }

  @Override
  public String getName() {
    return "RNScanCredentials";
  }

    private static final List<String> LOCAL_URI_PREFIXES = Arrays.asList("file://", "content://");
    private static boolean isLocalUri(String uri) {
        for (String localPrefix : LOCAL_URI_PREFIXES) {
            if (uri.startsWith(localPrefix)) {
                return true;
            }
        }
        return false;
    }
  @ReactMethod
  public void startOCR(String mUri, Callback callback) {

      try {
          Bitmap bitmap = openBitmapInputStream(mUri);
          if(bitmap!=null){
              String result = extractText(bitmap);
              callback.invoke(result);
          }
      } catch (Exception e) {
          callback.invoke(e.getMessage());
      }
  }

    private Bitmap openBitmapInputStream(String mUri) throws IOException {

        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inSampleSize = 4; // 1 - means max size. 4 - means maxsize/4 size. Don't use value <4, because you need more memory in the heap to store your data.
        Bitmap bitmap = null;
        InputStream stream = null;
        if( isLocalUri(mUri) ){ //red from local file
            Uri uri = Uri.parse(mUri);
            String path = uri.getPath();
            File file = new File(path);
            if(file.exists()){
                stream = new FileInputStream(file);//你自己之前的获取方法
                //stream =  getReactApplicationContext().getContentResolver().openInputStream(uri);
                //Data path does not exist!  FileInputStream 网络或缓存安全，没法读取。。。。 最后排查是后面 解析识别库出来的
                // bitmap = BitmapFactory.decodeFile(uri.getPath(), options);
            } else {
                throw new IOException("Cannot inputstream file: " + mUri);
            }
        } else { //red from net
            URLConnection connection = new URL(mUri).openConnection();
            stream = connection.getInputStream();
        }
        if (stream == null) {
            throw new IOException("Cannot open bitmap: " + mUri);
        }
        bitmap = BitmapFactory.decodeStream(stream, null, options);
        return bitmap;
    }



  private String extractText(Bitmap bitmap) {
      TessBaseAPI tessBaseApi = null;
      try {
          tessBaseApi = new TessBaseAPI();
      } catch (Exception e) {
          Log.e(TAG, e.getMessage());
          if (tessBaseApi == null) {
              Log.e(TAG, "TessBaseAPI is null. TessFactory not returning tess object.");
          }
      }
      copyfile(DATA_PATH, "tessdata/"+lang+".traineddata");
      tessBaseApi.init(DATA_PATH, lang);
      tessBaseApi.setImage(bitmap);
      String extractedText = "empty result";
      try {
          extractedText = tessBaseApi.getUTF8Text();
      } catch (Exception e) {
          Log.e(TAG, "Error in recognizing text.");
      }
      tessBaseApi.end();
      return extractedText;
  }

    /**
     * open can't begin with file separator
     * @param filepath
     * @param assetsName
     */
    public void copyfile(String filepath, String assetsName) {
    try {
        File file = new File(filepath + File.separator+ assetsName);
        if (!file.exists()) {
            File dir = new File(DATA_PATH + File.separator + "tessdata");
            if (!dir.exists()){
                dir.mkdirs();
            }
            InputStream is = reactContext.getResources().getAssets().open(assetsName);
            OutputStream fos = new FileOutputStream(file);
            byte[] buffer = new byte[1024];
            int count;
            while ((count = is.read(buffer)) !=-1) {
                fos.write(buffer, 0, count);
            }
            is.close();
            fos.flush();
            fos.close();
        }
    } catch (IOException e){
        e.printStackTrace();
    }
}


}
