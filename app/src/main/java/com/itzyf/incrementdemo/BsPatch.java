package com.itzyf.incrementdemo;

public class BsPatch {

    static {
        System.loadLibrary("native-lib");
    }

    public static native int bspatch(String oldApk, String newApk, String patch);

}