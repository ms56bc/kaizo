package com.kaizo.ticketsdownloader


import java.time.Instant
import java.util.UUID

import com.kaizo.ticketsdownloader.api.{TicketingSystem, TicketsStreamRouteApp}
import com.kaizo.ticketsdownloader.external.{TicketsDownloader, ZenDeskTicket, ZenDeskTicketDownloader}
import com.kaizo.ticketsdownloader.repository.ClientInfoRepository
import com.kaizo.ticketsdownloader.repository.ClientInfoRepository.ClientInfoRow
import com.kaizo.ticketsdownloader.services.{DownloadManager, TicketStreamProcessor, TicketsHandler}
import org.http4s.client.Client
import org.http4s.client.blaze.BlazeClientBuilder
import org.http4s.server.blaze.BlazeServerBuilder
import zio._
import zio.clock.Clock
import zio.console._
import zio.interop.catz._
import zio.interop.catz.implicits._


object Main extends zio.App {
  val streamId = UUID.randomUUID()
  val clientName = "testclient"
  val authInfo = ""
  val domain = ""
  val startRrom = Instant.now
  val clientX = ClientInfoRow(streamId, clientName, authInfo, domain, Some(startRrom), TicketingSystem.ZenDesk, false, false)

  override def run(args: List[String]): URIO[zio.ZEnv, ExitCode] = {
   ZIO.runtime[zio.ZEnv].flatMap { implicit runtime =>
      for {
        state <- RefM.make(Map.empty[UUID, ClientInfoRow])
        repo = ClientInfoRepository.inMemory(state)
        client: ZManaged[Any, Throwable, Client[Task]] = BlazeClientBuilder[Task](runtime.platform.executor.asEC).resource.toManagedZIO
        ticketHandler = TicketsHandler.zenDeskConsole
        zenDeskDownloader = TicketsDownloader.zenDesk(client)
        zenDeskProcessor: TicketStreamProcessor = TicketStreamProcessor.inMemory[ZenDeskTicket](repo, zenDeskDownloader, ticketHandler)
        streamProcessors: Map[TicketingSystem, TicketStreamProcessor] = Map(TicketingSystem.ZenDesk -> zenDeskProcessor)
        downLoadManager = DownloadManager.live(repo, streamProcessors )
        _ <- BlazeServerBuilder[RIO[Clock, *]](runtime.platform.executor.asEC)
          .bindHttp(8080, "0.0.0.0")
          .withHttpApp(TicketsStreamRouteApp(downLoadManager).routes)
          .resource
          .toManagedZIO
          .useForever
      } yield ()
    }.foldCauseM(
      err => putStrErr(err.prettyPrint).exitCode,
      _ => ZIO.succeed(ExitCode.success)
    )
  }

  //def stream(ec: ExecutionContext): Stream[F, ExitCode] =

}
