package com.ccri.app;

import com.sun.jna.Library;
import com.sun.jna.Native;

public interface Add extends Library
{
    Add INSTANCE = (Add) Native.loadLibrary("add", Add.class);
    int add(int x, int y);
}
