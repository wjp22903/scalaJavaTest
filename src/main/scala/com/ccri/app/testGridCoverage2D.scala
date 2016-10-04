package com.ccri.app

import java.awt.image.DataBuffer
import java.io.File

import com.vividsolutions.jts.geom.Envelope
import geotrellis.raster.ArrayTile
import geotrellis.vector.Extent
import org.geotools.factory.Hints
import org.geotools.gce.geotiff.GeoTiffReader
import org.geotools.geometry.DirectPosition2D
import org.locationtech.geomesa.utils.geotools.GridSnap
import org.opengis.referencing.crs.CoordinateReferenceSystem
import org.geotools.referencing.CRS

import scala.collection.immutable.List
import scala.collection.immutable.Range.Double

class testGridCoverage2D {

}

object testGridCoverage2D extends scala.App {

  val smallGridTile = {
//Actual grid is like below
//    val arr = Array(100.0, 110.0, 105.0,
//                    110.0, 110.0, 105.0,
//                    105.0, 105.0, 140.0)

//Use transposed grid to construct it
    val arr = Array(140.0, 105.0, 105.0,
                    105.0, 110.0, 110.0,
                    105.0, 110.0, 100.0)
//For debug, not used in actual processing
//    val arr = Array(100.0, 110.0, 120.0,
//                    130.0, 140.0, 150.0,
//                    160.0, 170.0, 180.0)
    ArrayTile(arr, 3, 3)
  }

  val tg = new testGeotrellis

  val (xMin, yMin, xMax, yMax) = (38.075433, -78.492613, 38.078433, -78.489613) //x: Latitude, y: Longitude
  val extent: Extent = Extent(xMin, yMin, xMax, yMax)

  val rasterName = "cvillSmallGrid"

  val crs: CoordinateReferenceSystem = CRS.decode("EPSG:4326")
  val crsWkt = crs.toWKT
  println("CRS: " + crsWkt)

  //Convert a Tile instance to GridCoverage2D instance.
  val gc_fromTile = tg.tileToGridCoverage2D(smallGridTile, rasterName, extent, DataBuffer.TYPE_DOUBLE, crs)

  //Save GridCOverageD instance into grotill file
  val tifFile_converted = "/tmp/cvillSmallGrid.tif"
  tg.saveGridCoverage2DToGeotiffFile(gc_fromTile, tifFile_converted)

  //Extra test
  val tile = tg.gridCoverage2DToTile(gc_fromTile)
  val tileArray = tile.toArrayDouble()
  tileArray

  val gridFile = new File(tifFile_converted)
  val geotiffReader = new GeoTiffReader(gridFile, new Hints(Hints.FORCE_LONGITUDE_FIRST_AXIS_ORDER, true))
  val grid = geotiffReader.read(null)


  val gridSpec = DisplayGridSpec(3, 0.002)
  println(s"gridSpec resolution: ${gridSpec.resolution}")
  val cellSize = gridSpec.resolution

  val coord1 = new DirectPosition2D(yMin+cellSize/2, xMin+cellSize/2)
  val h1 = grid.evaluate(coord1).asInstanceOf[Array[Double]].head
  println(s"($xMin, $yMin) has h1 = $h1")

  val coord2 = new DirectPosition2D(yMax-cellSize/2, xMax-cellSize/2)
  val h2 = grid.evaluate(coord2).asInstanceOf[Array[Double]].head
  println(s"($xMax, $yMax) has h2 = $h2")

  val Array(xMin1, yMin1) = grid.getEnvelope2D.getLowerCorner.getCoordinate
  val Array(xMax1, yMax1) = grid.getEnvelope2D.getUpperCorner.getCoordinate
  println(s"xMin1=$xMin1, yMin1=$yMin1, xMax1=$xMax1, yMax1=$yMax1")

  val (startingPointX, startingPointY) = gridSpec.snapToGrid(xMin1 + cellSize / 2.0, yMin1 + cellSize / 2.0)
  val (endingPointX, endingPointY) = gridSpec.snapToGrid(xMax1 - cellSize / 2.0, yMax1 - cellSize / 2.0)
  println(s"startingPointX=$startingPointX, startingPointY=$startingPointY, endingPointX=$endingPointX, endingPointY=$endingPointY")


  val coordList = {
    val lonList = Double.inclusive(startingPointX, endingPointX, cellSize).toList
    val latList = Double.inclusive(startingPointY, endingPointY, cellSize).toList
    println(s"lonList: ${lonList.toString}")
    println(s"latList: ${latList.toString}")
    for {lon <- lonList; lat <- latList} yield {
      val p = gridSpec.snapToGrid(lon, lat)
      println(s"Generate grid point (${p._1}, ${p._2}) from ($lon, $lat)")
      p
    }
  }

  val searchRad = gridSpec.gridWidthInDegrees / 2.0
  val nXCells = gridSpec.numberOfGridPoints
  val nYCells = gridSpec.numberOfGridPoints
  val envelope = new Envelope(0.0, 2 * searchRad, 0.0, 2 * searchRad)
  val snapGrid = new GridSnap(envelope, nXCells, nYCells)
  val ctrXIndex = 0
  val ctrYIndex = 0
  val (ctrCoordX, ctrCoordY) = gridSpec.gridIdxToCoord(ctrXIndex, ctrYIndex)
  println(s"ctrCoordX=$ctrCoordX, ctrCoordY=$ctrCoordY")

  val losPathMap = GeoUtils.calLosPathMap(ctrXIndex, ctrYIndex, nXCells, nYCells, gridSpec, snapGrid)

  val distMap = GeoUtils.generateDistanceMap(startingPointX, endingPointY, gridSpec)

  val tHeight = 11.0
  val towerElev = GeoUtils.queryRaster(grid, startingPointX, endingPointY).getOrElse(scala.Double.NaN) + tHeight
  println(s"Tower height at ($startingPointX, $endingPointY) is $towerElev")
  val slopeMap = GeoUtils.generateSlopeMap(startingPointX, endingPointY, towerElev, gridSpec, grid, distMap, xMin1, yMin1, xMax1, yMax1)
  val losArray = GeoUtils.calculateTerrainBlocking(losPathMap, slopeMap, gridSpec)

  val ctrXIndex2 = (nXCells - 1) / 2
  val ctrYIndex2 = (nYCells - 1) / 2
  val (ctrCoordX2, ctrCoordY2) = gridSpec.gridIdxToCoord(ctrXIndex2, ctrYIndex2)
  println(s"ctrCoordX2=$ctrCoordX2, ctrCoordY2=$ctrCoordY2")

  val losPathMap2 = GeoUtils.calLosPathMap(ctrXIndex2, ctrYIndex2, nXCells, nYCells, gridSpec, snapGrid)

  val distMap2 = GeoUtils.generateDistanceMap(startingPointX + cellSize, startingPointY + cellSize, gridSpec)

  val tHeight2 = 11.0
  val towerElev2 = GeoUtils.queryRaster(grid, startingPointX + cellSize, startingPointY + cellSize).getOrElse(scala.Double.NaN) + tHeight2
  println(s"Tower height at (${startingPointX + cellSize}, ${startingPointY + cellSize}) is $towerElev2")
  val slopeMap2 = GeoUtils.generateSlopeMap(startingPointX + cellSize, startingPointY + cellSize, towerElev2, gridSpec, grid, distMap2, xMin1, yMin1, xMax1, yMax1)
  val losArray2 = GeoUtils.calculateTerrainBlocking(losPathMap2, slopeMap2, gridSpec)

  val t = 1
  val x = 1
}