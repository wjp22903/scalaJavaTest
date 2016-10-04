package com.ccri.app

import scala.io.Source
import java.io._

object gpsSimulator {
  def main(args: Array[String]) = {
    val jz = JamZone(-78.460598, 38.023186, 3000.0)
//    val inputFile = "/opt/devel/src/grizzly-bear/Data/powers/all-powers.csv"
    val inputFile = "/home/jw9bn/SPINOZA/Geomesa/grizzlybear/test_gps_jamzone/messages.sc1.streaming.csv"
    ///    val outputFile = "/home/jw9bn/SPINOZA/Geomesa/grizzlybear/test_gps_jamzone/all-powers-jamzone.csv"
    //Use one cellId for all power measures
//    val outputFile = "/home/jw9bn/SPINOZA/Geomesa/grizzlybear/test_gps_jamzone/all-powers-jamzone-onecellId.csv"
    val outputFile = "/home/jw9bn/SPINOZA/Geomesa/grizzlybear/test_gps_jamzone//messages.sc1.streaming.jamzone.only.csv"
    var fields = Array[String]()

    val writer = new PrintWriter((new File(outputFile)))
    for(line <- Source.fromFile(inputFile).getLines()) {
      fields = line.split(",")
      val (lon, lat) = (fields(3).toDouble, fields(2).toDouble)

      val str= {
        if (distFrom(lat, lon, jz.lat, jz.lon) < jz.radius) {
          fields(13) = "-6000.0"
          fields(9) = "42"
          fields.mkString(",")
        }
        else ""
//        fields(9) = "42"
//        fields.mkString(",")
      }

      //writer.write(str + "\n")
      if (str.length > 0) writer.write(str + "\n")
    }

    writer.close()
  }

  def generateJourney(jamZone: JamZone, journeyConfig: JourneyCofnig): List[PowerReading] = {
    null
  }

  def distFrom(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Double = {
    val earthRadius = 6371000; //meters
    val dLat = math.toRadians(lat2-lat1)
    val dLng = math.toRadians(lng2-lng1)
    val a = math.sin(dLat/2) * math.sin(dLat/2) +
      math.cos(math.toRadians(lat1)) * math.cos(math.toRadians(lat2)) * math.sin(dLng/2) * math.sin(dLng/2)
    val c = 2 * math.atan2(math.sqrt(a), math.sqrt(1-a))
    val dist = (earthRadius * c)

    return dist
  }
}

case class JamZone(lon: Double, lat: Double, radius: Double)

case class JourneyCofnig(sLon: Double, sLat: Double, direction: Double, startTime: Long, speed: Double, duration: Long)

case class PowerReading(batchId: String,
                        cellId: Int,
                        datetime: Long,
                        altitude: Double,
                        accuracy: Double,
                        mcc: String,
                        mnc: Int,
                        lac: Int,
                        powerDbm: Double,
                        lon: Double,
                        lat: Double) {
  val uniqueCellId = s"$mcc-$mnc-$lac-$cellId"
}
