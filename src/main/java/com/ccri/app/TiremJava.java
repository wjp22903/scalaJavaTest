package com.ccri.app;

import com.sun.jna.Library;
//import com.sun.jna.Native;
import com.sun.jna.ptr.*;

public interface TiremJava extends Library
{
//    TiremJava INSTANCE = (TiremJava) Native.loadLibrary("tirem", TiremJava.class);

    void calctiremloss_(FloatByReference TANTHT,
                        FloatByReference RANTHT,
                        FloatByReference PROPFQ,
                        IntByReference NPRFL,
                        float[] HPRFL,
                        float[] XPRFL,
                        IntByReference EXTNSN,
                        FloatByReference REFRAC,
                        FloatByReference CONDUC,
                        FloatByReference PERMIT,
                        FloatByReference HUMID,
                        char[] POLARZ,
                        char[] VRSION,
                        char[] MODE,
                        FloatByReference LOSS,
                        FloatByReference FSPLSS);
}
