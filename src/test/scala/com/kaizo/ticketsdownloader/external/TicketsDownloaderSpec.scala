package com.kaizo.ticketsdownloader.external

import zio.test.{RunnableSpec, TestAspect, TestRunner, ZSpec}

object TicketsDownloaderSpec extends RunnableSpec {
  override def aspects: List[TestAspect[Nothing, Nothing, Nothing, Any]] = ???

  override def runner: TestRunner[Nothing, Nothing] = ???

  override def spec: ZSpec[Nothing, Nothing] = ???
}
