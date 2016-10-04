package com.ccri.app

import scala.collection.immutable.IndexedSeq

trait GridSpec {
  val numberOfGridPoints: Int
  val gridWidthInDegrees: Double
  val resolution = ScalaUtils.roundPlaces(gridWidthInDegrees / (numberOfGridPoints - 1.0), 10)

  /**
    * Converts grid coordinates, where (0.0, 0.0) is center cell of grid, to an array index.
    *
    * @param xCoord x-coordinate on grid.
    * @param yCoord y-coordinate on grid.
    * @return Array index.
    */
  def coordToArrayIdx(xCoord: Double, yCoord: Double): (Int) = {
    val (row, col) = coordToRowCol(xCoord, yCoord)
    col.round.toInt + (row * numberOfGridPoints).round.toInt
  }

  /**
    * Converts grid coordinates, where (0.0, 0.0) is center cell of grid, to row-column indices.
    *
    * @param xCoord x-coordinate on grid.
    * @param yCoord y-coordinate on grid.
    * @return Row and column indices.
    */
  def coordToGridIdx(xCoord: Double, yCoord: Double): (Int, Int) = {
    val (row, col) = coordToRowCol(xCoord, yCoord)
    (row.round.toInt, col.round.toInt)
  }

  private def coordToRowCol(xCoord: Double, yCoord: Double): (Double, Double) = {
    val searchRad = gridWidthInDegrees / 2.0
    val row = (yCoord + searchRad) * ((numberOfGridPoints - 1) / (2.0 * searchRad))
    val col = (xCoord + searchRad) * ((numberOfGridPoints - 1) / (2.0 * searchRad))
    (row, col)
  }

  /**
    * Convert grid index to grid coordinates where (0.0, 0.0) is center cell of grid.
    *
    * @param xIdx row index
    * @param yIdx column index
    * @return Coordinates
    */
  def gridIdxToCoord(xIdx: Int, yIdx: Int): (Double, Double) = {
    val searchRad = gridWidthInDegrees / 2.0
    val xCoord = 2.0 * searchRad * (xIdx.toDouble / (numberOfGridPoints.toDouble - 1.0) ) - searchRad
    val yCoord = 2.0 * searchRad * (yIdx.toDouble / (numberOfGridPoints.toDouble - 1.0) ) - searchRad
    snapToGrid(xCoord, yCoord)
  }

  /** Snap to Grid
    * Rounds the lon lat to the nearest grid location.
 *
    * @param lon Longitude.
    * @param lat Latitude.
    * @return Snapped longitude and latitude.
    */
  def snapToGrid(lon: Double, lat: Double): (Double, Double) = {
    import ScalaUtils.roundToNearest
    (roundToNearest(lon, resolution), roundToNearest(lat, resolution))
  }

  def generateSnappedGrid[T](lon: Double, lat: Double)(f: (Double, Double) => T): IndexedSeq[T] = {
    import ScalaUtils.roundToNearest
    val diagonal = generateDiagonal(roundToNearest(lon, resolution), roundToNearest(lat, resolution))
    for {(lon, _) <- diagonal; (_, lat) <- diagonal} yield f(lon, lat)
  }

  // starts at lower left and goes diagonally up to the right
  // assumes a square grid, outputs a diagonal
  private def generateDiagonal(lon: Double, lat: Double): IndexedSeq[(Double, Double)] = {
    // todo what if grid points circle back around globe
    // todo squarematrix class if that's better
    import ScalaUtils.{roundPlaces, roundToNearest}
    val stepSize = roundPlaces(gridWidthInDegrees / (numberOfGridPoints - 1).toDouble, 10)
    val lonStart = lon - gridWidthInDegrees / 2.0
    val latStart = lat - gridWidthInDegrees / 2.0
    for {i <- 0 until numberOfGridPoints} yield {
      // unfortunately we need to round because of precision errors
      (roundToNearest(lonStart + i * stepSize, stepSize), roundToNearest(latStart + i * stepSize, stepSize))
    }
  }
}

case class DisplayGridSpec(numberOfGridPoints: Int, gridWidthInDegrees: Double) extends GridSpec {
  require(resolution % 0.001 == 0, s"Invalid resolution: $resolution")
}

object DisplayGridSpec {
  def apply(size: String) = {
    size match {
      // TODO determine brute force setting
      case "small-low" => new DisplayGridSpec(21, 0.1)
      case "small" => new DisplayGridSpec(101, 0.1)
      case "medium" => new DisplayGridSpec(301, 0.3)
      case "large" => new DisplayGridSpec(501, 0.5)
      case _ => sys.error(s"Invalid grid size: $size")
    }
  }
}

case class BfGridSpec(numberOfGridPoints: Int, gridWidthInDegrees: Double) extends GridSpec
