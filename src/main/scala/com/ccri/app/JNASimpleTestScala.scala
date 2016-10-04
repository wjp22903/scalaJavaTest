import com.ccri.app.Add
import com.sun.jna.Native

object JNASimpleTestScala extends scala.App {
  System.setProperty ("jna.library.path", "/tmp")
  //  System.setProperty ("java.library.path", "/home/jw9bn/SPINOZA/Geomesa/grizzlybear/pnt/tirem")

  val lib = Native.loadLibrary("add", classOf[Add]).asInstanceOf[Add]
  //  val lib = Native.loadLibrary("tirem", classOf[tirem]).asInstanceOf[tirem]
  //  val lib = System.load("/home/jw9bn/SPINOZA/Geomesa/grizzlybear/pnt/tirem/libtirem.a").asInstanceOf[tirem]

  System.out.println(lib.add(10, 20))
  println("Test is done!")

}