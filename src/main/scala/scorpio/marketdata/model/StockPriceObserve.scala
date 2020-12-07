package scorpio.marketdata.model

import java.time._
import java.util.concurrent.TimeUnit

import com.typesafe.config.ConfigFactory
import org.mongodb.scala.bson.collection.immutable.Document
import org.mongodb.scala.model.Filters._
import scorpio.dao.mongo.{MongoEntity, MongoUtils, TimedMongoOperation}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}

/**
  * Created by HONGBIN on 2016/12/10.
  */
case class StockPriceObserve (
                               code: String,
                               name: String,
                               todayOpen: Double,
                               yesterdayClose: Double,
                               currentPrice: Double,
                               todayHigh: Double,
                               todayLow: Double,
                               offerPrice: Double,
                               bitPrice: Double,
                               volume: Long,
                               volumeValue: Double,
                               offerVolumes: Vector[Int],
                               offerPrices: Vector[Double],
                               bidVolumes: Vector[Int],
                               bidPrices: Vector[Double],
                               datetime: ZonedDateTime
                            ) extends MongoEntity[StockPriceObserve] {

  override def toDocument = Document(
    "code" -> code,
    "name" -> name,
    "todayOpen" -> todayOpen,
    "yesterdayClose" -> yesterdayClose,
    "currentPrice" -> currentPrice,
    "todayHigh" -> todayHigh,
    "todayLow" -> todayLow,
    "offerPrice" -> offerPrice,
    "bitPrice" -> bitPrice,
    "volume" -> volume,
    "volumeValue" -> volumeValue,
    "offerVolumes" -> offerVolumes,
    "offerPrices" -> offerPrices,
    "bidVolumes" -> bidVolumes,
    "bidPrices" -> bidPrices,
    "datetime" -> MongoUtils.toBsonDateTime(datetime)
  )
}

object StockPriceObserve extends TimedMongoOperation[StockPriceObserve] {

  val timeout = ConfigFactory.load().getInt("mongo.timeout")

  /**
    * Parse data in the format of
    * var hq_str_sh600485="信威集团,16.870,16.900,16.890,17.030,16.820,16.890,16.900,24563063,415484916.000,
    * 2900,16.890,32000,16.880,57500,16.870,61800,16.860,81100,16.850,66205,16.900,58200,16.910,53400,16.920,
    * 41700,16.930,49200,16.940,2016-12-09,15:00:00,00";
    * @param data
    */
  def apply(data: String): StockPriceObserve = {
    val pat = """var\s+hq_str_(\w+)="(.+)";""".r
    data match {
      case pat(code, others) =>
        val tokens = others.split(",")
        new StockPriceObserve(
          code = code,
          name = tokens(0),
          todayOpen = tokens(1).toDouble,
          yesterdayClose = tokens(2).toDouble,
          currentPrice = tokens(3).toDouble,
          todayHigh = tokens(4).toDouble,
          todayLow = tokens(5).toDouble,
          offerPrice = tokens(6).toDouble,
          bitPrice = tokens(7).toDouble,
          volume = tokens(8).toLong,
          volumeValue = tokens(9).toDouble,
          offerVolumes = Vector(tokens(10).toInt, tokens(12).toInt, tokens(14).toInt, tokens(16).toInt, tokens(18).toInt),
          offerPrices = Vector(tokens(11).toDouble, tokens(13).toDouble, tokens(15).toDouble, tokens(17).toDouble, tokens(19).toDouble),
          bidVolumes = Vector(tokens(20).toInt, tokens(22).toInt, tokens(24).toInt, tokens(26).toInt, tokens(28).toInt),
          bidPrices = Vector(tokens(21).toDouble, tokens(23).toDouble, tokens(25).toDouble, tokens(27).toDouble, tokens(29).toDouble),
          datetime = ZonedDateTime.of(LocalDate.parse(tokens(30)), LocalTime.parse(tokens(31)), ZoneId.of("Asia/Shanghai"))
        )
      case _ => throw new IllegalArgumentException(s"Cannot parse ${data}")
    }
  }

  override val dateTimeField = "datetime"

  override val collectionName = "StockPriceObserve"

  override def fromDocument(doc: Document) = {
    val epoch = MongoUtils.getEpochInMilliseconds(doc, "datetime")
    new StockPriceObserve(
      name = MongoUtils.getString(doc, "name"),
      code = MongoUtils.getString(doc, "code"),
      todayOpen = MongoUtils.getDouble(doc, "todayOpen"),
      yesterdayClose = MongoUtils.getDouble(doc, "yesterdayClose"),
      currentPrice = MongoUtils.getDouble(doc, "currentPrice"),
      todayHigh = MongoUtils.getDouble(doc, "todayHigh"),
      todayLow = MongoUtils.getDouble(doc, "todayLow"),
      offerPrice = MongoUtils.getDouble(doc, "offerPrice"),
      bitPrice = MongoUtils.getDouble(doc, "bitPrice"),
      volume = MongoUtils.getLong(doc, "volume"),
      volumeValue = MongoUtils.getDouble(doc, "volumeValue"),
      offerVolumes = MongoUtils.getVectorOfInt(doc, "offerVolumes"),
      offerPrices = MongoUtils.getVectorOfDouble(doc, "offerPrices"),
      bidVolumes = MongoUtils.getVectorOfInt(doc, "bidVolumes"),
      bidPrices = MongoUtils.getVectorOfDouble(doc, "bidPrices"),
      datetime = ZonedDateTime.ofInstant(Instant.ofEpochMilli(epoch), ZoneId.of("Asia/Shanghai"))
    )
  }

  def upsertMany(observes: Seq[StockPriceObserve]) = {
    val futures = observes map { observe =>
      StockPriceObserve.upsertAsync(and(equal("code", observe.code), equal("datetime", MongoUtils.toBsonDateTime(observe.datetime))), observe)
    }
    Await.result(Future.sequence(futures), Duration(timeout, TimeUnit.SECONDS))
  }
}
