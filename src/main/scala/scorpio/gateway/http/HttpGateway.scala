package scorpio.gateway.http

import org.apache.http.client.entity.UrlEncodedFormEntity
import org.apache.http.client.fluent.Request
import org.apache.http.client.methods.{HttpGet, HttpPost, HttpUriRequest}
import org.apache.http.impl.nio.client.HttpAsyncClients
import org.apache.http.message.BasicNameValuePair

import scala.collection.JavaConverters._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.io.Source

/**
  * Created by HONGBIN on 2016/11/3.
  */
object HttpGateway {

  lazy val asyncClient = {
    val client = HttpAsyncClients.createDefault()
    client.start()
    client
  }

  def get(url: String): String = {
    Request.Get(url).execute().returnContent().asString().trim
  }

  def getAsync(url: String): Future[String] = {
    val request = new HttpGet(url)
    Future {
      asyncRequest(request)
    }
  }

  def get(base: String, params: Map[String, String]): String = {
    val url = base + "?" + params.map((item: (String, String)) => s"${item._1}=${item._2}").mkString("&")
    Request.Get(url).execute().returnContent().asString().trim
  }

  def getAsync(base: String, params: Map[String, String]): Future[Any] = {
    val url = base + "?" + params.map((item: (String, String)) => s"${item._1}=${item._2}").mkString("&")
    val request = new HttpGet(url)
    Future {
      asyncRequest(request)
    }
  }

  def post(url: String, params: Map[String, String]): String = {
    val data = params.map((item: (String, String)) => { new BasicNameValuePair(item._1, item._2)})
    Request.Post(url).bodyForm(data.toSeq:_*).execute().returnContent().asString().trim
  }

  def postAsync(url: String, params: Map[String, String]): Future[String] = {
    val data = params.map((item: (String, String)) => { new BasicNameValuePair(item._1, item._2)}).asJava
    val request = new HttpPost(url)
    request.setEntity(new UrlEncodedFormEntity(data))
    Future {
      asyncRequest(request)
    }
  }

  private def asyncRequest(request: HttpUriRequest): String = {
    val entity = asyncClient.execute(request, null).get.getEntity
    Source.fromInputStream(entity.getContent).getLines().mkString("\n")
  }

}