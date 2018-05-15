package uk.gov.hmrc.agentmappingfrontend.support

import org.scalatest.enablers.KeyMapping
import org.scalatest.exceptions.TestFailedException
import org.scalatest.matchers.{MatchResult, Matcher}
import org.scalatest.{BeforeAndAfterEach, Matchers, Suite}
import uk.gov.hmrc.agentmappingfrontend.audit.AuditService
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.audit.model.DataEvent

import scala.concurrent.Future

trait AuditSupport extends BeforeAndAfterEach {
  me: Suite with Matchers =>

  private val expectedAuditSource = "agent-mapping-frontend"
  private var lastAuditEvent: Option[DataEvent] = None

  protected val testAuditService: AuditService = new AuditService {
    override def send(event: DataEvent)(implicit hc: HeaderCarrier): Future[Unit] = {
      lastAuditEvent = Some(event)
      Future.successful(())
    }
  }

  def auditEventShouldNotHaveBeenSent[A](eventType: String): Unit = lastAuditEvent match {
    case Some(event) =>
      if (event.auditType == eventType)
        throw new TestFailedException(s"Unexpected audit event type $eventType : $event", 0)
    case None => MatchResult.apply(matches = true, "", "")
  }

  def auditEventShouldHaveBeenSent[A](expectedAuditType: String)(matcher: Matcher[DataEvent]): Unit =
    lastAuditEvent match {
      case Some(event) if event.auditType == expectedAuditType =>
        val matcherResult = auditTagsNotEmpty("path", "X-Session-ID", "X-Request-ID", "clientIP", "clientPort")
          .and(auditSource(expectedAuditSource))
          .and(auditDetailsNotEmpty("Authorization"))
          .and(matcher)
          .apply(event)
        if (!matcherResult.matches) {
          throw new TestFailedException(matcherResult.failureMessage, 0)
        }

      case Some(event) =>
        throw new TestFailedException(
          s"Audit event ${event.auditType} has been sent but expected $expectedAuditType",
          0)
      case None => throw new TestFailedException("Audit event has not been sent although expected.", 0)
    }

  def auditSource(auditSource: String): Matcher[DataEvent] = new Matcher[DataEvent] {
    def apply(event: DataEvent): MatchResult = be(auditSource).apply(event.auditSource)
  }

  def auditType(auditType: String): Matcher[DataEvent] = new Matcher[DataEvent] {
    def apply(event: DataEvent): MatchResult = be(auditType).apply(event.auditType)
  }

  def auditDetailsNotEmpty(keys: String*): Matcher[DataEvent] =
    keys.map(k => auditDetail(k, not(be(empty)))).reduce(_ and _)

  def auditDetail(pair: (String, String)): Matcher[DataEvent] = auditDetail(pair._1, be(pair._2))

  def auditDetailKey(key: String)(implicit keyMapping: KeyMapping[Map[String, String]]): Matcher[DataEvent] =
    new Matcher[DataEvent] {
      def apply(event: DataEvent): MatchResult =
        contain.key(key).matcher(keyMapping).apply(event.detail)
    }

  def auditDetail(key: String, matcher: Matcher[String]): Matcher[DataEvent] = new Matcher[DataEvent] {
    def apply(event: DataEvent): MatchResult =
      if (!event.detail.contains(key))
        MatchResult(
          matches = false,
          s"did not contains event detail $key",
          "",
          Vector(event, key)
        )
      else matcher(event.detail(key))
  }

  def auditTagsNotEmpty(keys: String*): Matcher[DataEvent] = keys.map(k => auditTag(k, not(be(empty)))).reduce(_ and _)

  def auditTag(pair: (String, String)): Matcher[DataEvent] = auditTag(pair._1, be(pair._2))

  def auditTag(key: String, matcher: Matcher[String]): Matcher[DataEvent] = new Matcher[DataEvent] {
    def apply(event: DataEvent): MatchResult =
      if (!event.tags.contains(key))
        MatchResult(
          matches = false,
          s"did not contains event tag $key",
          "",
          Vector(event, key)
        )
      else {
        matcher(event.tags(key))
      }
  }

  override protected def beforeEach(): Unit = {
    super.beforeEach()
    lastAuditEvent = None
  }

}
