package com.itzyf.incrementdemo;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.support.v7.app.AlertDialog;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * @author zouyuanfeng
 * @version 2016/4/21 11:53
 */
public class FileDownload extends AsyncTask<String, Long, String> {
    private AlertDialog mProgressDialog;

    private TextView mTvProgress;
    private ProgressBar mProgressBar;
    private Context context;

    private long fileLength = 0;

    /**
     * #{@link FileDownload#execute(Object[])} <br/>
     * 参数1：下载链接<br/>
     * 参数2：文件保存路径
     */
    public FileDownload(Context context) {
        this.context = context;
        View view = LayoutInflater.from(context).inflate(R.layout.progress_download, null);
        mTvProgress = view.findViewById(R.id.tv_progress);
        mProgressBar = view.findViewById(R.id.loader);
        mProgressDialog = new AlertDialog.Builder(context)
                .setCancelable(false)
                .setView(view).create();
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        if (isValidContext(context)) {
            mProgressDialog.show();
        }
    }


    @Override
    protected String doInBackground(String... urlAndPath) {
        try {

            if (urlAndPath.length < 2) {
                throw new IllegalArgumentException("参数异常,参数一：url链接，参数二：保存的文件路径");
            }

            //连接地址
            URL u = new URL(urlAndPath[0]);
            HttpURLConnection c = (HttpURLConnection) u.openConnection();
            c.setRequestMethod("GET");
            c.setConnectTimeout(5000);
            //计算文件长度
            fileLength = c.getContentLength();
            mProgressBar.setMax((int) (fileLength / 1024));

            String filename = urlAndPath[0].substring(urlAndPath[0].lastIndexOf("/") + 1);


            String path = urlAndPath[1];
            if (TextUtils.isEmpty(path)) {
                return null;
            }

            File file = new File(path, filename);
            if (!file.getParentFile().exists()) {
                file.getParentFile().mkdirs();
            }

            FileOutputStream f = new FileOutputStream(file);
            InputStream in = c.getInputStream();
            //下载的代码
            byte[] buffer = new byte[1024];
            int len1 = 0;
            long total = 0;
            while ((len1 = in.read(buffer)) > 0) {
                total += len1;
                publishProgress(total);
                f.write(buffer, 0, len1);
            }
            f.close();
            return file.getPath();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    protected void onProgressUpdate(Long... progress) {
        mProgressBar.setProgress((int) (progress[0] / 1024));
        mTvProgress.setText(context.getString(R.string.download_progress, progress[0] / 1024, fileLength / 1024));
    }

    @Override
    protected void onPostExecute(String s) {
        super.onPostExecute(s);
        if (isValidContext(context) && mProgressDialog.isShowing()) {
            mProgressDialog.dismiss();
        }
        if (s != null) {
            if (s.endsWith("patch")) {
                String extract = ApkExtract.extract(context);
                String newApk = getSdcardDir() + "/tydic/aa.apk";
                BsPatch.bspatch(extract, newApk, s);
                install(context, newApk);
            } else {
                install(context, s);
            }
        } else {
            Toast.makeText(context, "下载失败", Toast.LENGTH_SHORT).show();
        }
    }

    private boolean isValidContext(Context c) {
        Activity a = (Activity) c;

        if (a.isDestroyed() || a.isFinishing()) {
            Log.i("YXH", "Activity is invalid." + " isDestoryed-->" + a.isDestroyed() + " isFinishing-->" + a.isFinishing());
            return false;
        } else {
            return true;
        }
    }

    private boolean install(Context context, String filePath) {
        Intent i = new Intent(Intent.ACTION_VIEW);
        File file = new File(filePath);
        if (!file.exists() || !file.isFile() || file.length() <= 0) {
            return false;
        }

        i.setDataAndType(Uri.parse("file://" + filePath), "application/vnd.android.package-archive");
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(i);
        return true;
    }

    private String getSdcardDir() {
        if (Environment.getExternalStorageState().equalsIgnoreCase(Environment.MEDIA_MOUNTED)) {
            return Environment.getExternalStorageDirectory().toString();
        }
        return null;
    }

}
