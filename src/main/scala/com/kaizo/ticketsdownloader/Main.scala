package com.kaizo.ticketsdownloader


import java.util.UUID
import com.kaizo.ticketsdownloader.api.{TicketingSystem, TicketsStreamRouteApp}
import com.kaizo.ticketsdownloader.external.{TicketsDownloader, ZenDeskResponse}
import com.kaizo.ticketsdownloader.repository.ClientInfoRepository
import com.kaizo.ticketsdownloader.repository.ClientInfoRepository.ClientInfoRow
import com.kaizo.ticketsdownloader.services.{DownloadManager, TicketStreamProcessor, TicketsHandler}
import org.http4s.client.blaze.BlazeClientBuilder
import org.http4s.server.blaze.BlazeServerBuilder
import zio._
import zio.clock.Clock
import zio.console._
import zio.interop.catz._


object Main extends zio.App {

  override def run(args: List[String]): URIO[zio.ZEnv, ExitCode] = {
   ZIO.runtime[zio.ZEnv].flatMap { implicit runtime =>
     BlazeClientBuilder[Task](runtime.platform.executor.asEC).resource.toManagedZIO.use { client =>
       for {
         state <- RefM.make(Map.empty[UUID, ClientInfoRow])
         streamsState <- RefM.make(Map.empty[UUID, Fiber.Runtime[Throwable, Unit]])
         repo = ClientInfoRepository.inMemory(state)
         ticketHandler = TicketsHandler.zenDeskConsole
         zenDeskDownloader = TicketsDownloader.zenDesk(client)
         zenDeskProcessor: TicketStreamProcessor = TicketStreamProcessor.inMemory[ZenDeskResponse](repo, zenDeskDownloader, ticketHandler)
         streamProcessors: Map[TicketingSystem, TicketStreamProcessor] = Map(TicketingSystem.ZenDesk -> zenDeskProcessor)
         downLoadManager = DownloadManager.live(repo, streamProcessors, streamsState)
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
  }
}
