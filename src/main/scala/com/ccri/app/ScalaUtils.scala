package com.ccri.app

import scala.util.{Failure, Success, Try}

object ScalaUtils {
  // Try doesn't have a "finally" (as opposed to try {} catch{} finally{}), so use this as workaround.
  def finalize[A](t: Try[A], `finally`: => Unit) = t match {
    case s@Success(_) => `finally`; s
    case f@Failure(_) => `finally`; f
  }

  def time[R](block: => R, label: String = ""): R = {
    val t0 = System.nanoTime()
    val result = block    // call-by-name
    val t1 = System.nanoTime()
    val elapsed = (t1 - t0) * 1E-9
    if (elapsed > 60) println("Elapsed time: " + elapsed / 60 + "m")
    else println("Elapsed time: " + elapsed + "s")
    result
  }

  def time(t0: Long, label: String) {
    val t1 = System.nanoTime()
    val elapsed = (t1 - t0) * 1E-9
    val s = if (elapsed > 60) "Elapsed time: " + elapsed / 60 + "m"
            else "Elapsed time: " + elapsed + "s"
    println(label + " - " + s)
  }

  def minutesToMillis(minutes: Double) = (minutes * 60000L).toLong

  def roundToNearest(num: Long, nearest: Long): Long = {
    roundToNearest(num.toDouble, nearest.toDouble).toLong
  }

  def roundToNearest(num: Double, nearest: Double): Double = {
    val numDecimalPlaces = nearest.toString.split('.')(1).length
    roundPlaces(Math.round(num / nearest) * nearest, numDecimalPlaces)
  }

  def roundPlaces(num: Double, numPlaces: Int) = {
    BigDecimal(num).setScale(numPlaces, BigDecimal.RoundingMode.HALF_UP).toDouble
  }

  def tryToInt(s: String, default: Int): Int = {
    Try(s.toInt).toOption match { case Some(x) => x ; case None => default }
  }

  def generateRandomList(maxNum: Int, numToGenerate: Int): List[Int] = {
    val r = scala.util.Random
    val randomSet = scala.collection.mutable.Set[Int]()
    val actualNumToGenerate = Math.min(numToGenerate, maxNum)
    while (randomSet.size < actualNumToGenerate) randomSet += (r.nextInt(maxNum))
    randomSet.toList
  }
}
