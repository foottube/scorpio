package scorpio.gateway.mail

import java.time.LocalTime

import scorpio.common.UnitSpec


/**
  * Created by HONGBIN on 2016/11/1.
  */
class MailGatewayTest extends UnitSpec {

  val subject = "TEST MESSAGE"
  val content = LocalTime.now().toString

  "A MailGateway" can "send message to specified receiver" in {
    MailGateway.sendMessage("foottube@126.com", "foottube@126.com", subject, content)
    Thread.sleep(10000) // Sleep for 10 seconds
  }

  it can "retrieve the message just sent " in {
    val mail = MailGateway.pollMessage("TEST")
    assert(mail.from == "foottube@126.com")
    assert(mail.subject == subject)
    // Currently can't parse content due to problem in handling multipart MIME type
    // TODO: re-enable this test after multipart problem resolved
    //assert(mail.content == content)
  }

  it can "save message to Mongo and retrieve it" in {
    val mail = MailGateway.pollMessage("TEST")
    MailMessage.upsert(mail)
    val retrieved = MailMessage.findAll
    assert(retrieved.size == 1)
    assert(retrieved.head == mail)
  }

}
