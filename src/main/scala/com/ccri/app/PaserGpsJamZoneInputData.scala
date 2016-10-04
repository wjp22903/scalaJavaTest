package com.ccri.app

import scala.io.Source
import java.io._

object PaserGpsJamZoneInputData {
  def main(args: Array[String]) {
    println("Filtering file")

    val inputFile = "/home/jw9bn/SPINOZA/Geomesa/grizzlybear/test_gps_jamzone/status-msg-with_NaNs.csv"
    val outputFile = "/home/jw9bn/SPINOZA/Geomesa/grizzlybear/test_gps_jamzone/status-msg-with_NaNs-10sensors.csv"
    val sensorIdsToInclude = Array("1001", "1901", "1501", "201", "701", "901", "1801", "1601", "801", "101")
    var fields = Array[String]()

    val writer = new PrintWriter((new File(outputFile)))
    for (line <- Source.fromFile(inputFile).getLines()) {
      fields = line.split(',')
      val sensorId = fields(1).split('.')(0)
      if (sensorIdsToInclude.indexOf(sensorId) >= 0) {
        writer.write(line + "\n")
      }
    }

    writer.close()
  }
}