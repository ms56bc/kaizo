package com.kaizo.ticketsdownloader.services


import java.time.{Duration, Instant}
import java.util.UUID

import com.kaizo.ticketsdownloader.external.{Ticket, TicketsDownloader}
import com.kaizo.ticketsdownloader.repository.ClientInfoRepository
import com.kaizo.ticketsdownloader.services.DownloadManager.DownloadManagerError
import com.kaizo.ticketsdownloader.services.TicketsHandler.ProcessingStatus
import zio.clock.Clock
import zio.{Has, RIO, Schedule, Task, UIO, ZIO, ZLayer}
import zio.stream.ZStream

import scala.concurrent.duration._

trait TicketStreamProcessor[T] {
  def startDownloading(streamId: UUID): RIO[Clock, Unit]
  protected def getLastEvent(streamId: UUID, uuid: Option[UUID]): Task[Option[Instant]]
  protected def updateProcessedUntil(streamId: UUID, processedUntil: Instant): Task[Unit]
  protected def handle(tickets: List[T]): UIO[ProcessingStatus]
}

object TicketStreamProcessor {



  def inMemory[T<: Ticket](
                           clientInfoRepository: ClientInfoRepository.Service,
                           ticketsDownloader: TicketsDownloader[T],
                           ticketsHandler: TicketsHandler[T]) =
  new InMemoryTicketStream(clientInfoRepository, ticketsDownloader, ticketsHandler)

}

abstract class InMemoryTicketStream[T<: Ticket](clientInfoRepository: ClientInfoRepository.Service,
                                                   ticketsDownloader: TicketsDownloader[T],
                                                   ticketsHandler: TicketsHandler[T]) extends TicketStreamProcessor {

  private def repeatUntil(streamId: UUID): UIO[Boolean] =
    clientInfoRepository.getInfo(streamId)
      .map(_.exists(_.continueProcessing))
      .fold(
        _ => true,
        continue => !continue
      )

  override def startDownloading(streamId: UUID): ZIO[Clock, DownloadManagerError, Unit] = {
   ZStream
      .fromEffect(getLastEvent(streamId, None))
      .mapM{
        case Some(startFrom) => ticketsDownloader.download(startFrom)
        case None => ZIO.fail(DownloadManager.ClientStreamMissing(streamId))
      }
      .mapM(tickets => ticketsHandler.handle(tickets))
      .mapM(status => updateProcessedUntil(streamId, status.processedUntil))
      .runDrain
      .delay(Duration.ofSeconds(10))
      .repeatUntilM(_ => repeatUntil(streamId))
      .unit
      .mapError(error => DownloadManager.GenericError(error.getMessage))
  }

  override protected def getLastEvent(streamId: UUID, uuid: Option[UUID]): Task[Option[Instant]] = {
    clientInfoRepository.getInfo(streamId).map(_.flatMap(_.startFrom))
  }

  override protected def updateProcessedUntil(streamId: UUID, processedUntil: Instant): UIO[Unit] = {
    clientInfoRepository.saveOffset(streamId, processedUntil)
  }
}
// offset fetch and save (time or cursor based)
// process event (has different tickets types and processing need)
// download event(different api to download from)