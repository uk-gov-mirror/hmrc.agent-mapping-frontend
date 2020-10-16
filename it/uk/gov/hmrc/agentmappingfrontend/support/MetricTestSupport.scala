package uk.gov.hmrc.agentmappingfrontend.support

import com.codahale.metrics.MetricRegistry
import com.kenshoo.play.metrics.Metrics
import org.scalatest.{Assertion, Matchers}
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import scala.collection.JavaConverters._

trait MetricTestSupport {
  self: GuiceOneAppPerSuite with Matchers =>

  def app: Application

  private var metricsRegistry: MetricRegistry = _

  def givenCleanMetricRegistry(): Unit = {
    val registry = app.injector.instanceOf[Metrics].defaultRegistry
    for (metric <- registry.getMetrics.keySet().iterator().asScala) {
      registry.remove(metric)
    }
    metricsRegistry = registry
  }

  def timerShouldExistsAndBeenUpdated(metric: String): Assertion = {
    val timers = metricsRegistry.getTimers
    val metrics = timers.get(s"Timer-$metric")
    if (metrics == null) throw new Exception(s"Metric [$metric] not found, try one of ${timers.keySet()}")
    metrics.getCount should be >= 1L
  }
}
