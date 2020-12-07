package scorpio.marketdata.model

import java.time.{LocalDate, LocalTime, ZoneId, ZonedDateTime}

import org.mongodb.scala.Completed
import org.mongodb.scala.model.Filters._

import scala.concurrent.ExecutionContext.Implicits.global
import scorpio.common.UnitSpec


/**
  * Created by HONGBIN on 2016/12/12.
  */
class SelectedStockTest extends UnitSpec {

  val stock1 = SelectedStock(code = "sh600485", name = "信威集团", selected = true,
    whenSelected = ZonedDateTime.of(LocalDate.parse("2016-12-10"), LocalTime.parse("10:00:00"), ZoneId.of("Asia/Shanghai")))
  val stock2 = SelectedStock(code = "sz002601", name= "佰利联", selected = true,
    whenSelected = ZonedDateTime.of(LocalDate.parse("2016-10-10"), LocalTime.parse("13:00:00"), ZoneId.of("Asia/Shanghai")))
  val stock3 = SelectedStock(code = "sz300068", name = "南都电源", selected = false,
    whenSelected = ZonedDateTime.of(LocalDate.parse("2016-10-10"), LocalTime.parse("13:00:00"), ZoneId.of("Asia/Shanghai")),
    whenInvalidated = Some(ZonedDateTime.of(LocalDate.parse("2016-10-17"), LocalTime.parse("14:00:00"), ZoneId.of("Asia/Shanghai"))))

  "SelectedStock" can "be inserted into Mongo DB" in {
    SelectedStock.insertMany(List(stock1, stock2, stock3))
  }

  it can "be searched by selected" in {
    val selected = SelectedStock.findSelected
    assert(selected.contains(stock1) && selected.contains(stock2))
    assert(!selected.contains(stock3))
  }

  it can "be dropped" in {
    SelectedStock.drop
  }

}
