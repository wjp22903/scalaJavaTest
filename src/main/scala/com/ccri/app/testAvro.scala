package com.ccri.app

import java.io.File

import org.apache.avro.file.{DataFileReader, DataFileWriter}
import org.apache.avro.io.{DatumReader, DatumWriter}
import org.apache.avro.specific.{SpecificDatumReader, SpecificDatumWriter}
import example.avro._

class testAvro {

//  def serializationTest(): Unit = {
//    val user1 = new User()
//    user1.setName("Alyssa")
//    user1.setFavoriteNumber(256)
//    // Leave favorite color null
//
//    // Alternate constructor
//    val user2 = new User("Ben", 7, "red")
//
//    // Construct via builder
//    val user3 = User.newBuilder()
//                    .setName("Charlie")
//                    .setFavoriteColor("blue")
//                    .setFavoriteNumber(null)
//                    .build()
//
//    val userDatumWriter: DatumWriter[User] = new SpecificDatumWriter[User](User.class)
//    val dataFileWriter: DataFileWriter[User] = new DataFileWriter[User](userDatumWriter)
//    dataFileWriter.create(user1.getSchema(), new File("/tmp/users.avro"))
//    dataFileWriter.append(user1)
//    dataFileWriter.append(user2)
//    dataFileWriter.append(user3)
//    dataFileWriter.close()
//  }
//
//  def deserializationTest(): Unit = {
//    // Deserialize Users from disk
//    val file = new File("/tmp/users.avro")
//    val userDatumReader: DatumReader[User] = new SpecificDatumReader[User](User.class)
//    val dataFileReader: DataFileReader[User] = new DataFileReader[User](file, userDatumReader)
//    while (dataFileReader.hasNext()) {
//      // Reuse user object by passing it to next(). This saves us from
//      // allocating and garbage collecting many objects for files with
//      // many items.
//      val user = dataFileReader.next(user)
//      System.out.println(user)
//    }
//
//  }
}

//object testAvro extends scala.App {
//  val tester = new testAvro
//  tester.serializationTest
//  tester.deserializationTest
//}