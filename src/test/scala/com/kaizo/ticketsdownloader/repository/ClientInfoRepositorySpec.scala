/*
package com.kaizo.ticketsdownloader.repository


import java.time.Instant
import java.util.UUID

import com.kaizo.ticketsdownloader.api.TicketingSystem
import com.kaizo.ticketsdownloader.repository
import com.kaizo.ticketsdownloader.repository.ClientInfoRepository.ClientInfoRow
import ClientInfoRepository.ClientInfoRepository
import zio.internal.Platform
import zio.test.Assertion.equalTo
import zio.test.{RunnableSpec, TestAspect, TestResult, TestRunner, assert, assertM, suite, test, testM}
import zio.{Has, RefM, Runtime, ZIO, ZLayer}


object ClientInfoRepositorySpec extends RunnableSpec {


  def spec =
    suite("OpenAuth")(
      testM("creates cookie with the correct token") {
        val streamId = UUID.randomUUID()
        val clientName = ""
        val authInfo = ""
        val domain = ""
        val startRrom = Instant.now
        val clientX = ClientInfoRow(streamId, clientName, authInfo, domain, startRrom, TicketingSystem.ZenDesk, false, false)
          for {
            x <- streamsStorage
          client <- ClientInfoRepository.inMemory.registerClient(streamId, clientName, authInfo, domain, startRrom, TicketingSystem.ZenDesk, false, false)
              .provideLayer(streamsStorage)
        } yield assert(client)(equalTo(clientX))

      }
    )

  val streamsStorage: ZLayer[Any, Nothing, ClientInfoRepository] = RefM.make(Map.empty[UUID, ClientInfoRow]).toLayer >>> ClientInfoRepository.inMemory
  val testRuntime: Runtime.Managed[ClientInfoRepository] = Runtime.unsafeFromLayer(
    streamsStorage,
    Platform.default
  )
}*/
