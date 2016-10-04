package com.ccri.app

import com.vividsolutions.jts.geom.Coordinate
import org.apache.commons.math3.util.FastMath
import org.geotools.coverage.grid.GridCoverage2D
import org.geotools.geometry.DirectPosition2D
import org.locationtech.geomesa.utils.geohash.VincentyModel
import org.locationtech.geomesa.utils.geotools.GridSnap

import scala.math._

object GeoUtils {
  def calcDeltaAzimuth(atLocation: DirectPosition2D, towerLocation: DirectPosition2D, towerAzimuth: Double) = {
    val relativeToOriginCompass = new DirectPosition2D(atLocation.x - towerLocation.x, atLocation.y - towerLocation.y)
    val relativeToOriginMath = compass2mathQuadrants(relativeToOriginCompass)
    val mobileAzimuth = cleanAngle(FastMath.atan2(relativeToOriginMath.y, relativeToOriginMath.x) * 180.0 / FastMath.PI)
    angleDifference(mobileAzimuth, towerAzimuth)
  }

  def calcDeltaTilt(distanceFromTower: Double, tilt: Double, towerAltitude: Double, powerAltitude: Double): Double = {
    val t = FastMath.abs(FastMath.atan2(towerAltitude - powerAltitude, distanceFromTower) * 180.0 / FastMath.PI - tilt)
    if(t < 0 ) t+360 else t
  }

  def angleDifference(a: Double, b: Double) = {
    val out = FastMath.abs(cleanAngle(a) - cleanAngle(b))
    if (out > 180) 360.0 - out
    else out
  }

  /**
    * Converts bearing to azimuth: 0 to 359.999
    * @param a
    * @return
    */
  def cleanAngle(a: Double) = {
    if (a < 0) a + 360.0
    else if (a >= 360) a - 360.0
    else a
  }

  /**
    * Converts azimuth to bearing: 0 to -179.999 / 180
    * @param a
    * @return
    */
  def dirtyAngle(a: Double) = {
    if (a <= -180) a + 360.0
    else if (a > 180) a - 360.0
    else a
  }

  // Quadrants
  // 4 | 1          2 | 1
  // -----    to    -----
  // 3 | 2          3 | 4
  def compass2mathQuadrants(input: DirectPosition2D) = new DirectPosition2D(input.y, input.x)

  /**
    * Handle type of GridCoverage2D.evaluate
    * @param input
    * @return a double representation of raster value.
    */
  // TODO this method can be changed to support rasters with multiple values at each point
  private def extract(input: AnyRef): Option[Double] = input match {
    case arr: Array[Int] => arr.headOption.map(_.toDouble)
    case arr: Array[Double] => arr.headOption
    case _ => None
  }

  def queryRaster(raster: GridCoverage2D, lon: Double, lat: Double): Option[Double] = {
    extract(raster.evaluate(new DirectPosition2D(lon, lat)))
  }

  /**
    * Euclidean distance between two points.
    * @param start
    * @param end
    * @return
    */
  def euclideanDistance(start: DirectPosition2D, end: DirectPosition2D): Double = start.distance(end)

  /**
    * Cleans up angles when computing arc.
    * @param azimuth
    * @param offsetDeg
    * @return
    */
  def computeVertex(azimuth: Double, offsetDeg: Double) = {
    cleanAngle(azimuth - offsetDeg/2.0)
  }

  def arcCoords(startCoord: (Double, Double), azimuth: Double, beamwidth: Double, numPoints: Int = 50, distance: Double = 300.0) = {
    for { i <- 0 until numPoints } yield {
      // long = x, lat = y
      moveWithBearingAndDistance(startCoord._1, startCoord._2, computeVertex(azimuth+(i.toDouble/numPoints)*beamwidth, beamwidth), distance)
    }
  }

  def moveWithBearingAndDistance(startX: Double, startY: Double, bearing: Double, distance: Double) = {
    val p = VincentyModel.moveWithBearingAndDistance(startX, startY, dirtyAngle(bearing), distance)
    (p.getX, p.getY)
  }

  /**
    * Calculate distance between two points on earth.
    *
    * @param lon1 - longitude of first point
    * @param lat1 - latitude of first point
    * @param lon2 - longitude of second point
    * @param lat2 - latitude of second point
    * @return distance in meters
    */
  def distanceBetween(lon1: Double, lat1: Double, lon2: Double, lat2: Double): Double =
    VincentyModel.getDistanceBetweenTwoPoints(lon1, lat1, lon2, lat2).getDistanceInMeters


  // TODO: move this function to org.locationtech.geomesa.utils.geotools.GridSnap (see GB-399 in Jira)
  def genBresenhamCoordList(x0: Int, y0: Int, x1: Int, y1: Int, gs: GridSnap): List[Coordinate] = {
    val ( deltaX, deltaY ) = (abs(x1 - x0), abs(y1 - y0))
    if ((deltaX == 0) && (deltaY == 0)) List[Coordinate](new Coordinate(gs.x(x0), gs.y(y0)))
    else {
      val ( stepX, stepY ) = (if (x0 < x1) 1 else -1, if (y0 < y1) 1 else -1)
      val (fX, fY) =  ( stepX * x1, stepY * y1 )
      def iter = new Iterator[Coordinate] {
        var (xT, yT) = (x0, y0)
        var error = (if (deltaX > deltaY) deltaX else -deltaY) / 2
        def next() = {
          val errorT = error
          if(errorT > -deltaX){ error -= deltaY; xT += stepX }
          if(errorT < deltaY){ error += deltaX; yT += stepY }
          new Coordinate(gs.x(xT), gs.y(yT))
        }
        def hasNext = stepX * xT <= fX && stepY * yT <= fY
      }
      iter.toList.dropRight(1)
    }
  }

  /**
    * Generate a map on the grid created from given point. It has the coordinate offsets to central point as keys
    * and distances as values.
    *
    * @param ctrLon
    * @param ctrLat
    * @param gridSpec
    * @return Distance map
    */
  def generateDistanceMap(ctrLon: Double, ctrLat: Double, gridSpec: GridSpec): Map[(Double, Double), Double] = {
    gridSpec.generateSnappedGrid(ctrLon, ctrLat) { (gcLon, gcLat) =>
      val (x, y) = gridSpec.snapToGrid(gcLon - ctrLon, gcLat - ctrLat)
      val dist = GeoUtils.distanceBetween(ctrLon, ctrLat, gcLon, gcLat)
      ((x, y), dist)
    }.toMap
  }

  /**
    * Generate a map on the grid created from given point. It has the coordinate offsets to central point as keys
    * and slopes as values.
    *
    * @param tLon
    * @param tLat
    * @param tElev
    * @param gridSpec
    * @param elevGrid
    * @param distMap
    * @param xMin
    * @param yMin
    * @param xMax
    * @param yMax
    * @return Slope map
    */
  def generateSlopeMap(tLon: Double,
                       tLat: Double,
                       tElev: Double,
                       gridSpec: GridSpec,
                       elevGrid: GridCoverage2D,
                       distMap: Map[(Double, Double), Double],
                       xMin: Double,
                       yMin: Double,
                       xMax: Double,
                       yMax: Double): Map[(Double, Double), Double] = {
    println(s"xMin=$xMin, yMin=$yMin, xMax=$xMax, yMax=$yMax")
    gridSpec.generateSnappedGrid(tLon, tLat) { (gcLon, gcLat) =>
      val (x, y) = gridSpec.snapToGrid(gcLon - tLon, gcLat - tLat)
      println(s"tLon=$tLon, tLat=$tLat, gcLon=$gcLon, gcLat=$gcLat, x=$x, y=$y, distMap((x, y)=" + distMap((x, y)))
      val slope = if (gcLon >= xMin && gcLon <= xMax && gcLat >= yMin && gcLat <= yMax && distMap((x, y)) != 0.0) {
        // assume person holds cell phone about 1.5 m above the ground
        val gcElev = GeoUtils.queryRaster(elevGrid, gcLon, gcLat).getOrElse(scala.Double.NaN) + 1.5
        println(s"gcElev=$gcElev, tElev=$tElev")
        (gcElev - tElev) / distMap(x, y)
      } else {
        // cell is outside DEM boundaries, so set its value to NaN
        scala.Double.NaN
      }
      println(s"($x, $y) Slope: $slope")
      ((x, y), slope)
    }.toMap
  }

  /**
    * Calculate LOS for all perimeter cells using the given slope map.
    * 1 means has LOS, -1 means no LOS, and 0 means cell wasn't evaluated
    *
    * @param losPathMap
    * @param slopeMap
    * @param gridSpec
    * @return LOS array
    */
  def calculateTerrainBlocking(losPathMap: scala.collection.mutable.HashMap[Tuple2[Double, Double], List[Tuple2[Double, Double]]],
                               slopeMap: Map[(Double, Double), Double],
                               gridSpec: GridSpec): Array[Byte] = {
    val losArray: Array[Byte] = Array.fill[Byte](math.pow(gridSpec.numberOfGridPoints.toDouble, 2.0).toInt) {0}

    // set LOS for center cell to be one
    losArray(gridSpec.coordToArrayIdx(0.0, 0.0)) = 1.toByte

    for (key <- losPathMap.keys) {
      println(s"key: (${key._1}, ${key._2}")

      var maxSlope = scala.Double.NegativeInfinity
      val cellList = losPathMap(key)
      for (cell <- cellList) {
        val arrayIdx = gridSpec.coordToArrayIdx(cell._1, cell._2)
        println(s"(${cell._1}, ${cell._2} convert to index $arrayIdx")
        val slope = slopeMap(cell._1, cell._2)
        println(s"current maxSlope: $maxSlope, slope: $slope")
        if (!slope.isNaN) {
          if (slope >= maxSlope) {
            maxSlope = slope
            losArray(arrayIdx) = 1.toByte
          } else {
            // if we already marked this cell as visible, don't change it
            if (losArray(arrayIdx) != 1.toByte) {
              // if not already marked visible, mark as NOT visible
              losArray(arrayIdx) = (-1).toByte
            }
          }
        }
      }
    }

    losArray
  }

  /**
    * Calculate LOS paths from given central point to the perimeter cells of the grid.
    *
    * @param ctrXIndex - x index of central point
    * @param ctrYIndex - y index of central point
    * @param nXCells - cell number in x dimension
    * @param nYCells - cell number in y dimension
    * @param gridSpec - grid dpecification
    * @param snapGrid - snap grid for Bresenham calculalation
    * @return LOS path map
    */
  def calLosPathMap(ctrXIndex: Int,
                    ctrYIndex: Int,
                    nXCells: Int,
                    nYCells: Int,
                    gridSpec: GridSpec,
                    snapGrid: GridSnap):
  scala.collection.mutable.HashMap[Tuple2[Double, Double], List[Tuple2[Double, Double]]] = {
    val losPathMap = scala.collection.mutable.HashMap.empty[Tuple2[Double, Double], List[Tuple2[Double, Double]]]


    // perimeter cells on bottom and top
    for (x <- 0 until nXCells) {
      val bottomList = GeoUtils.genBresenhamCoordList(ctrXIndex, ctrYIndex, x, 0, snapGrid).map(c => Tuple2(snapGrid.i(c.x), snapGrid.j(c.y)))
      losPathMap += (gridSpec.gridIdxToCoord(x, 0) ->
        bottomList.map( { case (xIdx, yIdx) => gridSpec.gridIdxToCoord(xIdx, yIdx) } ))
      val topList = GeoUtils.genBresenhamCoordList(ctrXIndex, ctrYIndex, x, nYCells - 1, snapGrid).map(c => Tuple2(snapGrid.i(c.x), snapGrid.j(c.y)))
      losPathMap += (gridSpec.gridIdxToCoord(x, nYCells - 1) ->
        topList.map( { case (xIdx, yIdx) => gridSpec.gridIdxToCoord(xIdx, yIdx) } ))
    }
    // perimeter cells on left and right
    for (y <- 1 until nYCells - 1) {
      val leftList = GeoUtils.genBresenhamCoordList(ctrXIndex, ctrYIndex, 0, y, snapGrid).map(c => Tuple2(snapGrid.i(c.x), snapGrid.j(c.y)))
      losPathMap += (gridSpec.gridIdxToCoord(0, y) ->
        leftList.map( { case (xIdx, yIdx) => gridSpec.gridIdxToCoord(xIdx, yIdx) } ))
      val rightList = GeoUtils.genBresenhamCoordList(ctrXIndex, ctrYIndex, nXCells - 1, y, snapGrid).map(c => Tuple2(snapGrid.i(c.x), snapGrid.j(c.y)))
      losPathMap += (gridSpec.gridIdxToCoord(nXCells - 1, y) ->
        rightList.map( { case (xIdx, yIdx) => gridSpec.gridIdxToCoord(xIdx, yIdx) } ))
    }

    losPathMap
  }
}
