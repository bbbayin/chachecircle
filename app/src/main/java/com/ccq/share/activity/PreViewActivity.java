package com.ccq.share.activity;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.ccq.share.Constants;
import com.chacq.share.R;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

/**************************************************
 *
 * 作者：巴银
 * 时间：2018/11/6 23:10
 * 描述：
 * 版本：
 *
 **************************************************/

public class PreViewActivity extends Activity {

    private static String TAG = "PreViewActivity";

    private LinearLayout llImageContainer;
    private TextView tvDesc;
    private int screenWidth;

    public static String EXTRA_IMAGE_LIST = "image_list";
    public static String EXTRA_HEAD_IMAGE = "head_image";
    public static String EXTRA_Water_Image = "qr_code";
    public static String EXTRA_NICKNAME = "nickname";
    public static String EXTRA_CAR_INFO = "car_info";
    private TextView tvNickName;
    private ImageView qrCode;
    private ImageView ivUserHeader;

    private int count = 0;
    private View rootView;

    public static void launch(Context context, String nickname, String carName, String waterImage, String headImage, ArrayList<Uri> imageList) {
        Intent intent = new Intent();
        intent.putExtra(EXTRA_NICKNAME, nickname);
        intent.putExtra(EXTRA_CAR_INFO, carName);
        intent.putExtra(EXTRA_Water_Image, waterImage);
        intent.putExtra(EXTRA_HEAD_IMAGE, headImage);
        intent.putExtra(EXTRA_IMAGE_LIST, imageList);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.setClass(context, PreViewActivity.class);
        context.startActivity(intent);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_preview);
        count = 0;
        rootView = findViewById(R.id.rootView);
        ivUserHeader = (ImageView) findViewById(R.id.user_header);
        tvDesc = (TextView) findViewById(R.id.tv_desc);
        tvNickName = (TextView) findViewById(R.id.user_name);
        qrCode = (ImageView) findViewById(R.id.image_qrCode);
        llImageContainer = (LinearLayout) findViewById(R.id.ll_image_container);
        Display defaultDisplay = getWindowManager().getDefaultDisplay();
        screenWidth = defaultDisplay.getWidth();

        // init view
        Intent intent = getIntent();
        String desc = intent.getStringExtra(EXTRA_CAR_INFO);
        tvDesc.setText(desc);
        LayoutInflater inflater = LayoutInflater.from(this);
        try {
            List<Uri> imageList = (List<Uri>) intent.getSerializableExtra(EXTRA_IMAGE_LIST);
            for (int i = 0; i < imageList.size(); i++) {
                Uri uri = imageList.get(i);
                int[] bitmapSize = getBitmapSize(uri);
                if (bitmapSize[0] != screenWidth) {
                    bitmapSize[1] = screenWidth * bitmapSize[1] / bitmapSize[0];
                    bitmapSize[0] = screenWidth;
                }
                ImageView imageView = (ImageView) inflater.inflate(R.layout.item_image, llImageContainer, false);
                ViewGroup.LayoutParams layoutParams = imageView.getLayoutParams();
                layoutParams.width = bitmapSize[0];
                layoutParams.height = bitmapSize[1];
                imageView.setLayoutParams(layoutParams);
                imageView.setImageURI(uri);
                llImageContainer.addView(imageView);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        //昵称
        tvNickName.setText(getIntent().getStringExtra(EXTRA_NICKNAME));
        //头像
        Glide.with(this).load(getIntent().getStringExtra(EXTRA_HEAD_IMAGE)).asBitmap().listener(new RequestListener<String, Bitmap>() {
            @Override
            public boolean onException(Exception e, String model, Target<Bitmap> target, boolean isFirstResource) {
                ivUserHeader.setImageResource(R.mipmap.icon_default_header);
                count++;
                launchWechat();
                return true;
            }

            @Override
            public boolean onResourceReady(Bitmap resource, String model, Target<Bitmap> target, boolean isFromMemoryCache, boolean isFirstResource) {
                ivUserHeader.setImageBitmap(resource);
                count++;
                launchWechat();
                return true;
            }
        }).into(ivUserHeader);
        //二维码
        Glide.with(this).load(String.format("%s!300auto", getIntent().getStringExtra(EXTRA_Water_Image))).asBitmap().listener(new RequestListener<String, Bitmap>() {
            @Override
            public boolean onException(Exception e, String model, Target<Bitmap> target, boolean isFirstResource) {
                count++;
                qrCode.setImageResource(R.mipmap.ccq_qrcode);
                launchWechat();
                return true;
            }

            @Override
            public boolean onResourceReady(Bitmap resource, String model, Target<Bitmap> target, boolean isFromMemoryCache, boolean isFirstResource) {
                count++;
                qrCode.setImageBitmap(resource);
                launchWechat();
                return true;
            }
        }).into(qrCode);
    }

    private void launchWechat() {
        if (count == 2) {
            //图片已经加载完毕
            Intent intent = new Intent();
            ComponentName comp = new ComponentName("com.tencent.mm",
                    Constants.WECHAT_SHAREUI_NAME);
            intent.setComponent(comp);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.setAction(Intent.ACTION_SEND_MULTIPLE);
            intent.setType("image/*");
            ArrayList<Uri> uris = new ArrayList<>();
            uris.add(Uri.fromFile(viewSaveToImage(rootView)));
            intent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris);
            startActivity(intent);
            finish();
        }
    }
//
//    public Bitmap convertViewToBitmap(View view) {
//        view.measure(0,0);
//        Bitmap shareBitmap = Bitmap.createBitmap(view.getMeasuredWidth(),
//                view.getMeasuredHeight(),
//                Bitmap.Config.RGB_565);
//        Canvas c = new Canvas(shareBitmap);
//        view.setDrawingCacheEnabled(true);
//        c.drawColor(Color.WHITE);
//        view.draw(c);
//        Log.i(TAG,"bitmap原始大小：width = "+shareBitmap.getWidth()+" height = "+shareBitmap.getHeight());
//        return shareBitmap;
//    }

    private File viewSaveToImage(View view) {
        view.setDrawingCacheEnabled(true);
        view.setDrawingCacheQuality(View.DRAWING_CACHE_QUALITY_HIGH);
        view.setDrawingCacheBackgroundColor(Color.WHITE);
        File result = null;
        // 把一个View转换成图片
        Bitmap cachebmp = loadBitmapFromView(view);

        FileOutputStream fos;
        String imagePath = "";
        try {
            // 判断手机设备是否有SD卡
            boolean isHasSDCard = Environment.getExternalStorageState().equals(
                    android.os.Environment.MEDIA_MOUNTED);
            if (isHasSDCard) {
                // SD卡根目录
                File sdRoot = Environment.getExternalStorageDirectory();
                result = new File(sdRoot, Calendar.getInstance().getTimeInMillis() + ".png");
                fos = new FileOutputStream(result);
                imagePath = result.getAbsolutePath();
            } else
                throw new Exception("创建文件失败!");

            cachebmp.compress(Bitmap.CompressFormat.PNG, 100, fos);

            fos.flush();
            fos.close();

        } catch (Exception e) {
            e.printStackTrace();
        }

        view.destroyDrawingCache();

        return result;
    }

    private Bitmap loadBitmapFromView(View v) {
        v.measure(0, 0);
        int w = v.getMeasuredWidth();
        int h = v.getMeasuredHeight();

        Bitmap bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(bmp);

        c.drawColor(Color.WHITE);
        /** 如果不设置canvas画布为白色，则生成透明 */

        v.layout(0, 0, w, h);
        v.draw(c);

        return bmp;
    }

    public File saveBitmap(Context context, Bitmap mBitmap) {
        String savePath;
        File filePic;
        if (Environment.getExternalStorageState().equals(
                Environment.MEDIA_MOUNTED)) {
            savePath = Environment.getExternalStorageDirectory().getPath() + "/ccq";
        } else {
            savePath = context.getApplicationContext().getFilesDir()
                    .getAbsolutePath()
                    + "/ccq";
        }
        try {
            filePic = new File(savePath + String.valueOf(System.currentTimeMillis()) + ".jpg");
            if (!filePic.exists()) {
                filePic.getParentFile().mkdirs();
                filePic.createNewFile();
            }
            FileOutputStream fos = new FileOutputStream(filePic);
            mBitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos);
            fos.flush();
            fos.close();
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
        Log.i(TAG, "图片存储路径：" + filePic.getAbsolutePath());
        return filePic;
    }


    private int[] getBitmapSize(Uri uri) {
        int[] size = new int[2];
        try {
            InputStream inputStream = getContentResolver().openInputStream(uri);
            BitmapFactory.Options onlyBoundsOptions = new BitmapFactory.Options();
            onlyBoundsOptions.inJustDecodeBounds = true;
            onlyBoundsOptions.inDither = true;//optional
            onlyBoundsOptions.inPreferredConfig = Bitmap.Config.ARGB_8888;//optional
            BitmapFactory.decodeStream(inputStream, null, onlyBoundsOptions);
            inputStream.close();
            size[0] = onlyBoundsOptions.outWidth;
            size[1] = onlyBoundsOptions.outHeight;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return size;
    }

    public Bitmap getBitmapFormUri(Activity ac, Uri uri) throws FileNotFoundException, IOException {
        InputStream input = ac.getContentResolver().openInputStream(uri);
        BitmapFactory.Options onlyBoundsOptions = new BitmapFactory.Options();
        onlyBoundsOptions.inJustDecodeBounds = true;
        onlyBoundsOptions.inDither = true;//optional
        onlyBoundsOptions.inPreferredConfig = Bitmap.Config.ARGB_8888;//optional
        BitmapFactory.decodeStream(input, null, onlyBoundsOptions);
        input.close();
        int originalWidth = onlyBoundsOptions.outWidth;
        int originalHeight = onlyBoundsOptions.outHeight;
        if ((originalWidth == -1) || (originalHeight == -1))
            return null;
        //图片分辨率以480x800为标准
        float hh = 800f;//这里设置高度为800f
        float ww = 480f;//这里设置宽度为480f
        //缩放比。由于是固定比例缩放，只用高或者宽其中一个数据进行计算即可
        int be = 1;//be=1表示不缩放
        if (originalWidth > originalHeight && originalWidth > ww) {//如果宽度大的话根据宽度固定大小缩放
            be = (int) (originalWidth / ww);
        } else if (originalWidth < originalHeight && originalHeight > hh) {//如果高度高的话根据宽度固定大小缩放
            be = (int) (originalHeight / hh);
        }
        if (be <= 0)
            be = 1;
        //比例压缩
        BitmapFactory.Options bitmapOptions = new BitmapFactory.Options();
        bitmapOptions.inSampleSize = be;//设置缩放比例
        bitmapOptions.inDither = true;//optional
        bitmapOptions.inPreferredConfig = Bitmap.Config.ARGB_8888;//optional
        input = ac.getContentResolver().openInputStream(uri);
        Bitmap bitmap = BitmapFactory.decodeStream(input, null, bitmapOptions);
        input.close();

        return compressImage(bitmap);//再进行质量压缩
    }

    public Bitmap compressImage(Bitmap image) {

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        image.compress(Bitmap.CompressFormat.JPEG, 100, baos);//质量压缩方法，这里100表示不压缩，把压缩后的数据存放到baos中
        int options = 100;
        while (baos.toByteArray().length / 1024 > 100) {  //循环判断如果压缩后图片是否大于100kb,大于继续压缩
            baos.reset();//重置baos即清空baos
            //第一个参数 ：图片格式 ，第二个参数： 图片质量，100为最高，0为最差  ，第三个参数：保存压缩后的数据的流
            image.compress(Bitmap.CompressFormat.JPEG, options, baos);//这里压缩options%，把压缩后的数据存放到baos中
            options -= 10;//每次都减少10
        }
        ByteArrayInputStream isBm = new ByteArrayInputStream(baos.toByteArray());//把压缩后的数据baos存放到ByteArrayInputStream中
        Bitmap bitmap = BitmapFactory.decodeStream(isBm, null, null);//把ByteArrayInputStream数据生成图片
        return bitmap;
    }
}
