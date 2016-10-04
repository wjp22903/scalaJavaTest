package com.ccri.app

import com.sun.jna.Library
import com.sun.jna.Native
import com.sun.jna.Platform
import com.sun.jna.ptr._

trait tirem extends Library {
  def calctiremloss_(TANTHT: Float,
                    RANTHT: Float,
                    PROPFQ: Float,
                    NPRFL: Int,
                    HPRFL: Float,
                    XPRFL: Float,
                    EXTNSN: Boolean,
                    REFRAC: Float,
                    CONDUC: Float,
                    PERMIT: Float,
                    HUMID: Float,
                    POLARZ: String,
                    VRSION: String,
                    MODE: String,
                    LOSS: Float,
                    FSPLSS: Float): Unit
}

class testTirem {

}

object testTirem extends scala.App {
  System.setProperty ("jna.library.path", "/home/jw9bn/SPINOZA/Geomesa/grizzlybear/pnt/tirem/test1")
//  System.setProperty ("java.library.path", "/home/jw9bn/SPINOZA/Geomesa/grizzlybear/pnt/tirem")

//  val lib = Native.loadLibrary("tirem", classOf[tirem])
  val lib = Native.loadLibrary("tirem", classOf[tirem]).asInstanceOf[tirem]
//  val lib = System.load("/home/jw9bn/SPINOZA/Geomesa/grizzlybear/pnt/tirem/libtirem.a").asInstanceOf[tirem]

  println("Test is done!")

}