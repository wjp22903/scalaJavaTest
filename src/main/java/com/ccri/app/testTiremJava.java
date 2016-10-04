package com.ccri.app;

import com.sun.jna.Native;
import com.sun.jna.ptr.FloatByReference;
import com.sun.jna.ptr.IntByReference;

import java.util.Arrays;

public class testTiremJava {
    public static void main(String[] args) {
        System.setProperty ("jna.library.path", "/home/jw9bn/SPINOZA/Geomesa/grizzlybear/pnt/tirem/test1");

//        TiremJava tirem = TiremJava.INSTANCE;
        TiremJava tirem = (TiremJava) Native.loadLibrary("tirem", TiremJava.class);

        float hPrfl[] = {0.0f,   0.0f,   0.0f,   0.0f,   0.0f,   0.0f,   0.0f,   0.0f,   0.0f,   0.0f,
                2.0f,   3.0f,   5.0f,   5.0f,   3.0f,   3.0f,   2.0f,   1.0f,   5.0f,   6.0f,
                6.0f,  11.0f,  26.0f,  38.0f,  52.0f,  61.0f,  63.0f,  74.0f,  91.0f, 103.0f,
                117.0f, 126.0f, 139.0f, 160.0f, 177.0f, 185.0f, 190.0f, 198.0f, 210.0f, 223.0f,
                235.0f, 244.0f, 245.0f, 249.0f, 253.0f, 260.0f, 268.0f, 275.0f, 283.0f, 285.0f,
                297.0f, 301.0f, 304.0f, 307.0f, 309.0f, 311.0f, 312.0f, 311.0f, 310.0f, 308.0f,
                307.0f, 305.0f, 305.0f, 305.0f, 305.0f, 308.0f, 313.0f, 325.0f, 338.0f, 350.0f,
                351.0f, 345.0f, 340.0f, 325.0f, 318.0f, 326.0f, 347.0f, 362.0f, 363.0f, 346.0f,
                348.0f, 364.0f, 369.0f, 382.0f, 382.0f, 370.0f, 375.0f, 390.0f, 382.0f, 347.0f,
                323.0f, 338.0f, 376.0f, 413.0f, 409.0f, 384.0f, 393.0f, 442.0f, 469.0f, 460.0f,
                450.0f, 444.0f, 447.0f, 461.0f, 476.0f, 455.0f, 434.0f, 431.0f, 417.0f, 432.0f,
                452.0f, 457.0f, 463.0f, 464.0f, 445.0f, 416.0f, 411.0f, 437.0f, 475.0f, 497.0f,
                497.0f, 508.0f, 552.0f, 593.0f, 605.0f, 615.0f, 635.0f, 651.0f, 656.0f, 671.0f,
                717.0f, 761.0f, 633.0f, 481.0f, 356.0f, 233.0f, 171.0f, 131.0f, 105.0f, 118.0f,
                183.0f, 260.0f, 229.0f, 162.0f, 104.0f,  61.0f,  71.0f, 103.0f,  95.0f,  61.0f,
                63.0f,  62.0f,  79.0f, 107.0f, 104.0f,  87.0f,  70.0f,  59.0f,  52.0f,  49.0f,
                50.0f,  40.0f,  35.0f,  36.0f,  28.0f,  27.0f,  27.0f,  18.0f,  10.0f,   5.0f,
                5.0f,   3.0f,   3.0f,   2.0f,   3.0f,   2.0f,   1.0f,   1.0f,   0.0f,   0.0f,
                0.0f,   0.0f,   0.0f,   0.0f,   0.0f};

        System.out.println("hPrfl[] length = " + hPrfl.length);

        int numPts = 185;
        float[] xPrfl = new float[numPts];
        for (int i = 0; i < 185; i++) {
            xPrfl[i] = i * 500.0f;
        }

        FloatByReference txHt = new FloatByReference(10.0f);
        FloatByReference rxHt = new FloatByReference(10.0f);
        FloatByReference freq = new FloatByReference(100.0f);
        IntByReference extenFlag = new IntByReference(0);
        IntByReference numPtsRef = new IntByReference(numPts);
        FloatByReference refrac = new FloatByReference(301.0f);
        FloatByReference conduc = new FloatByReference(0.028f);
        FloatByReference permit = new FloatByReference(15.0f);
        FloatByReference humid = new FloatByReference(10.0f);
        char[] polarization = {'V', ' ', ' ', ' '};
        char[] version = {' ', ' ', ' ', ' ', ' ', ' ', ' ', ' '};
        char[] mode = {' ', ' ', ' ', ' '};
        FloatByReference lossr = new FloatByReference(0.0f);
        FloatByReference fsplss = new FloatByReference(0.0f);

        tirem.calctiremloss_(txHt, rxHt, freq, numPtsRef, hPrfl, xPrfl, extenFlag, refrac, conduc, permit,
                humid, polarization, version, mode, lossr, fsplss);

        //Got weird mode and version displayed. Tried version.toString and new String(version), problem isn't fixed.
        //It doesn't matter that much for now.
        System.out.println("loss = " + lossr.getValue() + ",fsplss = " + fsplss.getValue() + ",mode = " +
                Arrays.toString(mode) + ",version = " + Arrays.toString(version));

        System.out.println("Test is done!");

    }
}
