package scorpio.service

import scorpio.common.UnitSpec
import scorpio.marketdata.model.StockCodeName

/**
  * Created by HONGBIN on 2017/1/7.
  */
class StockNameQueryServiceTest extends UnitSpec {

  "StockNameQueryService" can "retrieve stock code/name mapping and populate local cache and MongoDB" in {
    StockCodeName.insert(StockCodeName("sh123456", "XXX", false))
    assert(StockNameQueryService.cache.size == 0)
    assert(StockCodeName.findAll.isEmpty)
    val codeName = StockNameQueryService.get("sh601398").get
    assert(codeName.name == "工商银行")
    assert(StockNameQueryService.cache.size == 1)
    assert(StockCodeName.findAll.size == 1)
    val codeName2 = StockNameQueryService.get("sh601398").get
    assert(codeName2.name == "工商银行")
    assert(StockNameQueryService.cache.size == 1)
    assert(StockCodeName.findAll.size == 1)
    assert(StockNameQueryService.get("sh999999").isEmpty)
    StockCodeName.drop()
  }

}
