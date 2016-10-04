package com.ccri.app;

public class JNASimpleTest {
    public static void main(String[] args) {
        System.setProperty ("jna.library.path", "/tmp");

        Add lib = Add.INSTANCE;
        System.out.println(lib.add(10, 20));
    }
}
