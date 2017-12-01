package com.itzyf.incrementdemo;

import android.content.Intent;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.lang.ref.WeakReference;

/**
 * @author 依风听雨
 */
public class MainActivity extends AppCompatActivity implements DownloadUtil.OnDownloadListener {
    private AlertDialog mProgressDialog;

    private TextView mTvProgress;
    private ProgressBar mProgressBar;

    public static final String PATCH_URL = "http://itzyf.qiniudn.com/diff.patch";
    public static final int DOWNLOAD_SUCCESS = 1;
    public static final int DOWNLOAD_PROGRESS = 2;
    public static final int DOWNLOAD_FAILED = 3;
    protected Handler mHandler = new ActivityHandler(this);

    public void onUpdate(View view) {
        if (ApkExtract.getAppVersionCode(this) < 2) {
            showProgressDialog();
            DownloadUtil.get().download(PATCH_URL, "itzyf", this);
        } else {
            Toast.makeText(this, "已是新版本", Toast.LENGTH_SHORT).show();
        }
    }


    static class ActivityHandler extends Handler {
        WeakReference<MainActivity> mActivityReference;

        ActivityHandler(MainActivity activity) {
            mActivityReference = new WeakReference<>(activity);
        }

        @Override
        public void handleMessage(Message msg) {
            final MainActivity activity = mActivityReference.get();
            if (activity != null) {
                activity.handleMessage(msg);
            }
        }
    }

    public void handleMessage(Message msg) {
        switch (msg.what) {
            case DOWNLOAD_SUCCESS:
                mProgressDialog.dismiss();
                Toast.makeText(MainActivity.this, "下载成功", Toast.LENGTH_SHORT).show();
                String path = (String) msg.obj;
                String newApk = Environment.getExternalStorageDirectory() + File.separator + "itzyf" + File.separator + "new.apk";
                Log.d("MainActivity", "开始合成");
                BsPatch.bspatch(ApkExtract.extract(this), newApk, path);
                Log.d("MainActivity", "合成完成,开始安装");
                install(newApk);
                break;
            case DOWNLOAD_PROGRESS:
                int progress = (int) msg.obj;
                mProgressBar.setProgress(progress);
                mTvProgress.setText(progress + "%");
                break;
            case DOWNLOAD_FAILED:
                mProgressDialog.dismiss();
                Toast.makeText(MainActivity.this, "下载失败", Toast.LENGTH_SHORT).show();
                break;
            default:
                break;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }


    /**
     * 下载进度对话框
     */
    private void showProgressDialog() {
        View view = getLayoutInflater().inflate(R.layout.progress_download, null);
        mTvProgress = view.findViewById(R.id.tv_progress);
        mProgressBar = view.findViewById(R.id.loader);
        mProgressDialog = new AlertDialog.Builder(this)
                .setCancelable(false)
                .setView(view)
                .show();
    }

    /**
     * 安装apk
     *
     * @param filePath
     * @return
     */
    private boolean install(String filePath) {
        Intent i = new Intent(Intent.ACTION_VIEW);
        File file = new File(filePath);
        if (!file.exists() || !file.isFile() || file.length() <= 0) {
            return false;
        }

        i.setDataAndType(Uri.parse("file://" + filePath), "application/vnd.android.package-archive");
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(i);
        return true;
    }


    @Override
    public void onDownloadSuccess(String filePath) {
        Message message = Message.obtain();
        message.what = DOWNLOAD_SUCCESS;
        message.obj = filePath;
        mHandler.sendMessage(message);
    }

    @Override
    public void onDownloading(int progress) {
        Message message = Message.obtain();
        message.what = DOWNLOAD_PROGRESS;
        message.obj = progress;
        mHandler.sendMessage(message);
    }

    @Override
    public void onDownloadFailed() {
        mHandler.sendEmptyMessage(DOWNLOAD_FAILED);
    }
}
