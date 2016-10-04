package com.ccri.app

import java.awt.{Point, Transparency, RenderingHints}
import java.awt.color.ColorSpace
import java.awt.image._
import java.io.File
import javax.media.jai.{JAI, ImageLayout}

import geotrellis.raster._
import geotrellis.raster.io.geotiff.reader.GeoTiff
import geotrellis.vector.Extent
import org.geotools.geometry.{Envelope2D, GeneralEnvelope}
import org.opengis.geometry.Envelope
import org.opengis.referencing.crs.CoordinateReferenceSystem

//import geotrellis.raster.io.geotiff.reader.GeoTiffReader
import org.geotools.coverage.grid.{GridEnvelope2D, GridCoverageFactory, GridCoverage2D}
import org.geotools.coverage.grid.io.AbstractGridCoverage2DReader
import org.geotools.coverageio.gdal.dted.DTEDReader
import org.geotools.factory.Hints
import org.geotools.gce.geotiff.{GeoTiffWriter, GeoTiffReader}
import org.geotools.resources.coverage.CoverageUtilities
import spire.syntax.cfor._

class testGeotrellis {
  val argPath = "/tmp/"
  val filePathToTestData = "/opt/devel/git/geotrellis/raster-test/data/"

  def testGeotiffReader(path: String): Unit = {

  }

  private def read(fileName: String): GeoTiff = {
    val filePath = filePathToTestData + fileName
    geotrellis.raster.io.geotiff.reader.GeoTiffReader(filePath).read
  }

  private def readAndSave(fileName: String) {
    val geoTiff = read(fileName)

    geoTiff.imageDirectories.foreach(ifd => {
      val currentFileName = math.abs(ifd.hashCode) + "-" + fileName.substring(0,
        fileName.length - 4)

      val corePath = argPath + currentFileName
      val pathArg = corePath + ".arg"
      val pathJson = corePath + ".json"
      ifd.writeRasterToArg(corePath, currentFileName)
    })
  }


  def getGridCoverage2D(fileName: String): GridCoverage2D = {
    val reader = getReader(new File(fileName), getFileExtension(fileName))
    reader.read(null)
  }

  def getTile(fileName: String): Tile = {
    val reader = getReader(new File(fileName), getFileExtension(fileName))
    val gc = reader.read(null)
    gridCoverage2DToTile(gc)
  }

  /**
   * Convert GridCoverage2D instance to Tile instance.
   * Note: the result is one of BytesArrayTile, IntArrayTile, DoubleArrayTile, etc depending on data type.
   *
   * @param gc
   * @return a Tile instance
   */
  def gridCoverage2DToTile(gc: GridCoverage2D): Tile = {
    val gtData = gc.getRenderedImage.getData

    //Nothing is Geotiff specific in loadRasterExtent
    val rasterExtent = geotrellis.geotools.GeoTiffReader.loadRasterExtent(gc)
    val cellType = geotrellis.geotools.GeoTiffReader.getDataType(gc)

    val cols = rasterExtent.cols
    val rows = rasterExtent.rows

    //Use best guess method here. In tests no general way found to get no-data-value from GridCoverage2D
    val noDataValueArray = CoverageUtilities.getBackgroundValues(gc)
    val noData = d2i(noDataValueArray(0))

    //Copy some codes from GeoTiffReader in geotrellis. Original codes are embedded in geotrellis.geotools.GeoTiffReader.
    //It's not geotiff specific and can be applied to GridCoverage2D
    val tile =
      cellType match {
        case TypeBit | TypeByte | TypeShort =>
          val data = Array.ofDim[Int](cols * rows).fill(NODATA)
          gtData.getPixels(0, 0, cols, rows, data)
          val tile = ArrayTile(data.map(_.toShort), cols, rows)
          val nd = s2i(noData.toShort)
          if(isData(nd) && isData(noData)) {
            var conflicts = 0
            cfor(0)(_ < rows, _ + 1) { row =>
              cfor(0)(_ < cols, _ + 1) { col =>
                val z = tile.get(col, row)
                if(isNoData(z)) conflicts += 1
                if (z == nd) { tile.set(col, row, NODATA) }
              }
            }

            if(conflicts > 0) {
              println(s"[WARNING]  GeoTiff contained values of $shortNODATA, which are considered to be NO DATA values in ARG format. There are $conflicts raster cells that are now considered NO DATA values in the converted format.")

            }
          }

          tile
        case TypeInt =>
          val data = Array.ofDim[Int](cols * rows).fill(NODATA)
          gtData.getPixels(0, 0, cols, rows, data)
          val tile = ArrayTile(data, cols, rows)
          val nd = noData.toInt
          if(isData(nd) && isData(noData)) {
            var conflicts = 0
            cfor(0)(_ < rows, _ + 1) { row =>
              cfor(0)(_ < cols, _ + 1) { col =>
                val z = tile.get(col, row)
                if(isNoData(z)) conflicts += 1
                if (z == nd) { tile.set(col, row, NODATA) }
              }
            }

            if(conflicts > 0) {
              println(s"[WARNING]  GeoTiff contained values of $NODATA, which are considered to be NO DATA values in ARG format. There are $conflicts raster cells that are now considered NO DATA values in the converted format.")

            }
          }

          tile
        case TypeFloat =>
          val data = Array.ofDim[Float](cols * rows).fill(Float.NaN)
          gtData.getPixels(0, 0, cols, rows, data)
          val tile = ArrayTile(data, cols, rows)
          val nd = noData.toFloat
          if(isData(nd)) {
            var conflicts = 0
            cfor(0)(_ < rows, _ + 1) { row =>
              cfor(0)(_ < cols, _ + 1) { col =>
                val z = tile.getDouble(col, row)
                if(isNoData(z)) conflicts += 1
                if (z == nd) { tile.setDouble(col, row, Double.NaN) }
              }
            }

            if(conflicts > 0) {
              println(s"[WARNING]  GeoTiff contained values of ${Float.NaN}, which are considered to be NO DATA values in ARG format. There are $conflicts raster cells that are now considered NO DATA values in the converted format.")

            }
          }

          tile
        case TypeDouble =>
          val data = Array.ofDim[Double](cols * rows).fill(Double.NaN)
          gtData.getPixels(0, 0, cols, rows, data)
          val tile = ArrayTile(data, cols, rows)
          if(isData(noData)) {
            var conflicts = 0
            cfor(0)(_ < rows, _ + 1) { row =>
              cfor(0)(_ < cols, _ + 1) { col =>
                val z = tile.getDouble(col, row)
                if(isNoData(z)) conflicts += 1
                if (z == noData) { tile.setDouble(col, row, Double.NaN) }
              }
            }

            if(conflicts > 0) {
              println(s"[WARNING]  GeoTiff contained values of ${Double.NaN}, which are considered to be NO DATA values in ARG format. There are $conflicts raster cells that are now considered NO DATA values in the converted format.")

            }
          }

          tile

      }

      tile
  }

  /**
   * Generate a GridCoverage2D instance from a Tile instance.
   * Note: a Tile doesn't keep name and actual coordinate values.
   *
   * @param tile
   * @param rasterName
   * @param extent
   * @return GridCoverage2D instance
   */
  def tileToGridCoverage2D(tile: Tile, rasterName: String, extent: Extent, dataType: Int, crs: CoordinateReferenceSystem):
  GridCoverage2D = {
    val envelope = new GeneralEnvelope(Array(extent.xmin, extent.ymin), Array(extent.xmax, extent.ymax))
    envelope.setCoordinateReferenceSystem(crs)
    val image = drawImage(tile, tile.rows, tile.cols, dataType)
    defaultGridCoverageFactory.create(rasterName, image, envelope)
  }

  def getReader(imageFile: File, imageType: String): AbstractGridCoverage2DReader = {
    imageType match {
      case "TIFF" => getTiffReader(imageFile)
      case "DTED" => getDtedReader(imageFile)
      case _ => throw new Exception("Image type is not supported.")
    }
  }

  def getTiffReader(imageFile: File): AbstractGridCoverage2DReader = {
    new GeoTiffReader(imageFile, new Hints(Hints.FORCE_LONGITUDE_FIRST_AXIS_ORDER, true))
  }

  def getDtedReader(imageFile: File): AbstractGridCoverage2DReader = {
    val l = new ImageLayout()
    l.setTileGridXOffset(0).setTileGridYOffset(0).setTileHeight(512).setTileWidth(512)
    val hints = new Hints
    hints.add(new RenderingHints(JAI.KEY_IMAGE_LAYOUT, l))
    new DTEDReader(imageFile, hints)
  }

  def getFileExtension(file: String) = file.toLowerCase match {
    case geotiff if (file.endsWith("tif") || file.endsWith("tiff"))                     => "TIFF"
    case dted if (file.endsWith("dt0") || file.endsWith("dt1") || file.endsWith("dt2")) => "DTED"
    case _                                                                              => "NOTSUPPORTED"
  }

  val defaultGridCoverageFactory = new GridCoverageFactory

  def renderedImageToGridCoverage2d(name: String, image: RenderedImage, env: Envelope) =
    defaultGridCoverageFactory.create(name, image, env)

//  def transposeMatrix[T](ma: Array[T], cols: Int, rows: Int): List[T] =
//    ma.toList.grouped(cols).toList.flatMap(_.zipWithIndex).sortBy(-_._2).grouped(rows).toList.flatMap{_.unzip._1}

  //It turns out that Tile stores raster (2-D array) in different order as the raster in BufferedImage.
  //Following conversion is to restore the raster in a Tile to the format used in BufferedImage.
  def convertData[T: Manifest](ma: Array[T], cols: Int, rows: Int): Array[T] = {
    val newma: Array[T] = Array.ofDim[T](cols * rows)
    for {
      i <- 0 to (cols - 1)
      j <- 0 to (rows - 1)
    } {
        newma(i * rows + j) = ma((rows - 1 - j) * cols + (cols - 1 - i))
    }
    newma
  }

  def drawImage(tile: Tile, xdim: Int, ydim: Int, dataType: Int): BufferedImage = {
    val dbuffer = tile.cellType match {
      case TypeByte =>
        new DataBufferByte(convertData(tile.asInstanceOf[ByteArrayTile].array, xdim, ydim), xdim * ydim)
      case TypeInt =>
        new DataBufferInt(convertData(tile.asInstanceOf[IntArrayTile].array, xdim, ydim), xdim * ydim)
      case TypeShort =>
        new DataBufferShort(convertData(tile.asInstanceOf[ShortArrayTile].array, xdim, ydim), xdim * ydim)
      case TypeFloat =>
        new DataBufferFloat(convertData(tile.asInstanceOf[FloatArrayTile].array, xdim, ydim), xdim * ydim)
      case TypeDouble =>
        new DataBufferDouble(convertData(tile.asInstanceOf[DoubleArrayTile].array, xdim, ydim), xdim * ydim)
      case _ => throw new Exception("Wrong data type.")
    }

    val sampleModel = new BandedSampleModel(dataType,
                                            xdim,
                                            ydim,
                                            1)
    val colorModel = new ComponentColorModel(ColorSpace.getInstance(ColorSpace.CS_GRAY),
                                             null,
                                             false,
                                             false,
                                             Transparency.OPAQUE,
                                             dataType)
    val raster = Raster.createWritableRaster(sampleModel,
                                             dbuffer,
                                             new Point(0, 0))
    new BufferedImage(colorModel, raster, false, null)
  }

  def saveGridCoverage2DToGeotiffFile(gc: GridCoverage2D, fileName: String): Unit = {
    val writer = new GeoTiffWriter(new File(fileName))
    try {
      writer.write(gc, null)
      writer.dispose
    }
    catch {
      case e: Exception => println("Failed in writing GridCoverage2D instance into Geotiff file.")
    }
  }
}

object testGeotrellis extends scala.App {
  println("Test geotrellis!")
  val tg = new testGeotrellis
  println("Test: Read and write grotiff using Geotrellis!")
  tg.readAndSave("aspect.tif")

  println("Test: Conversion between GridCoverage2D and Tile")
  conversion_GridCoverage2D_Tile_Test

  def conversion_GridCoverage2D_Tile_Test(): Unit = {
    println("Read dted file into GridCoverage object and save it into a geotiff file.")
    val dtedFile = "/home/jw9bn/SPINOZA/Geomesa/wcs/DTED/rasexp_dted_1414070615_36939/dted/w123/n37.dt0"
    val gc_orig = tg.getGridCoverage2D(dtedFile)
    val tifFile_orig = "/tmp/n37_orig.tif"
    tg.saveGridCoverage2DToGeotiffFile(gc_orig, tifFile_orig)

    println("Convert GridCoverage object into Tile.")
    val dataType_orig = gc_orig.getSampleDimension(0).getSampleDimensionType
    val crs_orig = gc_orig.getCoordinateReferenceSystem2D
    //  val rasterName = gc_orig.getName.toString
    val rasterName = "n37"
    val extent = geotrellis.geotools.GeoTiffReader.loadRasterExtent(gc_orig).extent
    val tile = tg.gridCoverage2DToTile(gc_orig)

    println("Convert Tile into GridCoverage object and save it into a new geotiff file.")
    val dataType = DataBuffer.TYPE_SHORT
    val gc_fromTile = tg.tileToGridCoverage2D(tile, rasterName, extent, dataType, crs_orig)
    val tifFile_converted = "/tmp/n37_converted.tif"
    tg.saveGridCoverage2DToGeotiffFile(gc_fromTile, tifFile_converted)
  }
}