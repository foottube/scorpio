package scorpio.trade.model

import java.time.ZonedDateTime

import scorpio.common.UnitSpec

/**
  * Created by HONGBIN on 2017/1/22.
  */
class EodMilestoneTest extends UnitSpec {

  val now = ZonedDateTime.now

  val eod = now.minusMinutes(1)

  val eodMilestone = EodMilestone(eod)

  "EodMilestone" can "be inserted into Mongo and retrieved as latestBefore" in {
    EodMilestone.insert(eodMilestone)
    val saved = EodMilestone.findLatestBefore(now, 1)
    assert(saved.size == 1)
    assert(saved.head == eodMilestone)
    val yesterday = EodMilestone.findLatestBefore(now.minusDays(1), 1)
    assert(yesterday.isEmpty)
    EodMilestone.drop()
  }

}
