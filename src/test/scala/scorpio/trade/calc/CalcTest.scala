package scorpio.trade.calc

import scorpio.common.UnitSpec

/**
  * Created by HONGBIN on 2017/1/27.
  */
class CalcTest extends UnitSpec {

  "Calc.roundDouble" can "round double to nearest value with two digits after decimal point" in {
    assert(Calc.round(123.4567) == 123.46)
    assert(Calc.round(123.4512) == 123.45)
    assert(Calc.round(0.1) == 0.1)
    assert(Calc.round(-123.4567) == -123.46)
    assert(Calc.round(-123.4512) == -123.45)
    assert(Calc.round(-0.1) == -0.1)
    assert(Calc.round(1000) == 1000)
    assert(Calc.round(-1000) == -1000)
    assert(Calc.round(1.001) == 1.00)
    assert(Calc.round(1.009) == 1.01)
    assert(Calc.round(-1.001) == -1.00)
    assert(Calc.round(-1.009) == -1.01)
    assert(Calc.round(1.235) == 1.24)
    assert(Calc.round(1.234) == 1.23)
    assert(Calc.round(-1.235) == -1.24)
    assert(Calc.round(-1.234) == -1.23)
  }

}
