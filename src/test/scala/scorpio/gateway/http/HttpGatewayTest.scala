package scorpio.gateway.http

import java.util.concurrent.TimeUnit

import scorpio.common.UnitSpec

import scala.concurrent.Await
import scala.concurrent.duration.Duration

/**
  * Created by HONGBIN on 2016/11/3.
  */

class HttpGatewayTest extends UnitSpec {

  val baseUrl = "http://192.168.0.2/~hbzhang/cgi-bin/scorpio-httpgateway-test.cgi"

  "HttpGateway" can "download content using GET method" in {
    val result = HttpGateway.get(baseUrl)
    assert(result == "empty")
  }

  it can "download content using GET method with parameter map" in {
    val result = HttpGateway.get(baseUrl, Map("ABC" -> "123", "DEF" -> "456" ))
    assert(result == "ABC = 123")
  }

  it can "send post request with parameter map" in {
    val result = HttpGateway.post(baseUrl, Map("ABC" -> "123", "DEF" -> "456" ))
    assert(result == "ABC = 123")
  }

  it can "download content using GET method asynchronously" in {
    val result = Await.result(HttpGateway.getAsync(baseUrl), Duration(5, TimeUnit.SECONDS))
    assert(result == "empty")
  }

  it can "download content using GET method with parameter map asynchronously" in {
    val result = Await.result(HttpGateway.getAsync(baseUrl, Map("ABC" -> "123", "DEF" -> "456" )), Duration(5, TimeUnit.SECONDS))
    assert(result == "ABC = 123")
  }

  it can "send post request with parameter map asynchronously" in {
    val result = Await.result(HttpGateway.postAsync(baseUrl, Map("ABC" -> "123", "DEF" -> "456" )), Duration(5, TimeUnit.SECONDS))
    assert(result == "ABC = 123")
  }

  it can "download data from http://hq.sinajs.cn/" in {
    val result = HttpGateway.get("http://hq.sinajs.cn/list=sh601857")
    assert(result.length > 0)
  }

}
