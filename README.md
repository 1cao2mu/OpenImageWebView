解决 react native 的 webview 组件不支持android客户端上传图片文件问题

1. 为什么Rn的android端webview不支持上传图片？

   android原生的webview，本身就需要配置一个方法来配合上传图片，RN封装的webView没有配置这个方法

   ```java
   // For Android 4.1
           public void openFileChooser(ValueCallback<Uri> uploadMsg, String acceptType, String capture) {
               if (mUploadMessage != null) {
                   mUploadMessage.onReceiveValue(null);
               }
               mUploadMessage = uploadMsg;
               showPopSelectPic();
           }

           public boolean onShowFileChooser(WebView webView, ValueCallback<Uri[]> filePathCallback, FileChooserParams fileChooserParams) {
               if (mUploadMessage != null) {
                   mUploadMessage.onReceiveValue(null);
               }
               mUploadMessage = filePathCallback;
               showPopSelectPic();
               return true;
           }
   ```

2. 如何给RN的webview配置上这个方法？

   在js代码中暂时没办法处理，那只好改原生的方法，原生的webview封装在react native 包里，没办法改，只好重新封装一个webview.

3. 怎么重新封装一个Rn组件？
   https://reactnative.cn/docs/0.45/native-component-android.html#content  

   首先把/node_modules/react-native/ReactAndroid/src/main/java/com/facebook/react/views/webview中的ReactWebViewManager的复制到自己的src的java包目录里

   然后根据自己的需求进行更改，比如现在的需求事添加选择图片的配置

   1.新建一个ActivityResultInterface 接口回调

   ```java
   interface ActivityResultInterface {
       void callback(int requestCode, int resultCode, Intent data);
   }
   ```

   2.PickerActivityEventListener onActivityResult回调和自定义回调链接

   ```java
   public class PickerActivityEventListener extends BaseActivityEventListener {

       private ActivityResultInterface mCallback;

       public PickerActivityEventListener(ReactApplicationContext reactContext, ActivityResultInterface callback) {
           reactContext.addActivityEventListener(this);
           mCallback = callback;
       }

       // < RN 0.33.0
       public void onActivityResult(int requestCode, int resultCode, Intent data) {
           mCallback.callback(requestCode, resultCode, data);
       }

       // >= RN 0.33.0
       public void onActivityResult(Activity activity, int requestCode, int resultCode, Intent data) {
           mCallback.callback(requestCode, resultCode, data);
       }
   }
   ```

   3.ReactWebViewManager 自定义添加配置

   ```java
   public class ReactWebViewManager extends SimpleViewManager<WebView> implements ActivityResultInterface {
       //一些初始化变量
       private ReactApplicationContext reactApplicationContext;
       private ValueCallback mUploadMessage;
       private Uri imageUri;
       private static final int TAKE_PHOTO = 10001;
       private static final int CHOOSE_PHOTO = 10002;
       protected static final String REACT_CLASS = "RCTWebView2";
       ......
       //修改构造方法
       public ReactWebViewManager(ReactApplicationContext reactApplicationContext) {
           this.reactApplicationContext = reactApplicationContext;
           new PickerActivityEventListener(reactApplicationContext, this);
           mWebViewConfig = new WebViewConfig() {
               public void configWebView(WebView webView) {
               }
           };
       }
       ......
        protected WebView createViewInstance(final ThemedReactContext reactContext) {
           ReactWebView webView = createReactWebViewInstance(reactContext);
           webView.setWebChromeClient(new WebChromeClient() {
             ......
               // For Android 4.1
               public void openFileChooser(ValueCallback<Uri> uploadMsg, String acceptType, String capture) {
                   if (mUploadMessage != null) {
                       mUploadMessage.onReceiveValue(null);
                   }
                   mUploadMessage = uploadMsg;
                   showPopSelectPic(reactContext);
               }

               public boolean onShowFileChooser(WebView webView, ValueCallback<Uri[]> filePathCallback, FileChooserParams fileChooserParams) {
                   if (mUploadMessage != null) {
                       mUploadMessage.onReceiveValue(null);
                   }
                   mUploadMessage = filePathCallback;
                   showPopSelectPic(reactContext);
                   return true;
               }

           });
           ......
       }

       private void showPopSelectPic(final ThemedReactContext Context) {
           String[] items = new String[]{"相机", "相册"};
           AlertDialog.Builder builder = new AlertDialog.Builder(Context);
           builder.setTitle("提示")
                   .setSingleChoiceItems(items, 0, new DialogInterface.OnClickListener() {
                       @Override
                       public void onClick(DialogInterface dialog, int which) {
                           dialog.dismiss();
                           if (which == 0) {
                               //openCamera
                               File outputImage = new File(Context.getExternalCacheDir(), "output_image.jpg");
                               try {
                                   if (outputImage.exists()) {
                                       outputImage.delete();
                                   }
                                   outputImage.createNewFile();
                               } catch (IOException e) {
                                   e.printStackTrace();
                               }
                               if (Build.VERSION.SDK_INT < 24) {
                                   imageUri = Uri.fromFile(outputImage);
                               } else {
                                   imageUri = FileProvider.getUriForFile(Context, "com.rnpro.fileprovider", outputImage);
                               }
                               // 启动相机程序
                               if (ContextCompat.checkSelfPermission(Context, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                                   //ActivityCompat.requestPermissions(Context, new String[]{Manifest.permission.CAMERA}, 2);
                               } else {
                                   openCamera();
                               }
                           } else if (which == 1) {
                               //openAlbum
                               if (ContextCompat.checkSelfPermission(Context, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                                   //ActivityCompat.requestPermissions(Context, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
                               } else {
                                   openAlbum();
                               }
                           }
                       }
                   })
                   .setOnCancelListener(new DialogInterface.OnCancelListener() {
                       @Override
                       public void onCancel(DialogInterface dialog) {
                           if (mUploadMessage != null) {
                               mUploadMessage.onReceiveValue(null);
                               mUploadMessage = null;
                           }
                       }
                   });
           builder.create().show();
       }

       void openCamera() {
           Intent intent = new Intent("android.media.action.IMAGE_CAPTURE");
           intent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);
           Activity currentActivity = reactApplicationContext.getCurrentActivity();
           currentActivity.startActivityForResult(intent, TAKE_PHOTO);
       }

       void openAlbum() {
           Intent intent = new Intent("android.intent.action.GET_CONTENT");
           intent.setType("image/*");
           Activity currentActivity = reactApplicationContext.getCurrentActivity();
           currentActivity.startActivityForResult(intent, CHOOSE_PHOTO); // 打开相册
       }

       @Override
       public void callback(int requestCode, int resultCode, Intent data) {
           switch (requestCode) {
               case TAKE_PHOTO:
                   if (resultCode == RESULT_OK) {
                       if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                           mUploadMessage.onReceiveValue(new Uri[]{imageUri});
                       } else {
                           mUploadMessage.onReceiveValue(imageUri);
                       }
                       mUploadMessage = null;
                   } else {
                       mUploadMessage.onReceiveValue(null);
                       mUploadMessage = null;
                       return;
                   }
                   break;
               case CHOOSE_PHOTO:
                   if (resultCode == RESULT_OK) {
                       if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                           mUploadMessage.onReceiveValue(new Uri[]{data.getData()});
                       } else {
                           mUploadMessage.onReceiveValue(data.getData());
                       }
                       mUploadMessage = null;
                   } else {
                       mUploadMessage.onReceiveValue(null);
                       mUploadMessage = null;
                       return;
                   }
                   break;
               default:
                   break;
           }
       }

   }

   ```

   4.注意 需要注册FileProvider和设置xml path

   ```xml
          //清单文件中注册
          <provider
               android:name="android.support.v4.content.FileProvider"
               android:authorities="com.rnpro.fileprovider"
               android:exported="false"
               android:grantUriPermissions="true">
               <meta-data
                   android:name="android.support.FILE_PROVIDER_PATHS"
                   android:resource="@xml/file_paths" />
           </provider>
         //res资源文件中新建xml文件夹新建文件file_paths.xml
         <?xml version="1.0" encoding="utf-8"?>
         <paths xmlns:android="http://schemas.android.com/apk/res/android">
          <external-path name="my_images" path="" />
          </paths> 
   ```

4. 将写好的ReactWebViewManager写入到ReactPackage中，将ReactPackage写入Application中

   ```java
   public class WebViewReactPackage implements ReactPackage {


       public List<Class<? extends JavaScriptModule>> createJSModules() {
           return Collections.emptyList();
       }

       @Override
       public List<ViewManager> createViewManagers(ReactApplicationContext reactApplicationContext) {
           return Arrays.<ViewManager>asList(
                   new ReactWebViewManager(reactApplicationContext)
           );
       }

       @Override
       public List<NativeModule> createNativeModules(
               ReactApplicationContext reactApplicationContext) {
           return Collections.emptyList();
       }
   }
   ```

   ```java
   public class MainApplication extends Application implements ReactApplication {
   ......
     private final ReactNativeHost mReactNativeHost = new ReactNativeHost(this) {
     ......
       @Override
       protected List<ReactPackage> getPackages() {
         return Arrays.<ReactPackage>asList(
             new MainReactPackage(),
               new WebViewReactPackage()
         );
       }
    ......
     };
   ......
   }
   ```

5. 在js文件中，把/node_modules/react-native/Libraries/Components/WebView中的webview.android.js,复制到自己的js文件夹中，做一定的修改

   webview.ios.js

   ```javascript
   import { WebView } from 'react-native';
   export default WebView;
   ```

   webview.android.js

   ```javascript
   var RCT_WEBVIEW_REF = 'webview2';
   ......
   class WebView2 extends React.Component {
       ......
       var RCTWebView = requireNativeComponent('RCTWebView2', WebView2, WebView2.extraNativeComponentConfig);//RCTWebView2与ReactWebViewManager的REACT_CLASS对应
   //WebView2与上面的WebView2对应
   module.exports = WebView2;
   ```

6. ​

   ​