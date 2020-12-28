package com.kaizo.ticketsdownloader.services


import java.time.{Duration, Instant}
import java.util.UUID

import com.kaizo.ticketsdownloader.external.{Ticket, TicketsDownloader}
import com.kaizo.ticketsdownloader.repository.ClientInfoRepository
import com.kaizo.ticketsdownloader.repository.ClientInfoRepository.ClientInfoRow
import zio.clock.Clock
import zio.{Task, UIO, URIO, ZIO}
import zio.stream.ZStream


trait TicketStreamProcessor {
  def startDownloading(streamId: UUID): URIO[Clock, Unit]
  protected def updateProcessedUntil(streamId: UUID, processedUntil: Instant): Task[Unit]
}

object TicketStreamProcessor {



  def inMemory[T<: Ticket](
                           clientInfoRepository: ClientInfoRepository.Service,
                           ticketsDownloader: TicketsDownloader[T],
                           ticketsHandler: TicketsHandler[T]) =
  new InMemoryTicketStream(clientInfoRepository, ticketsDownloader, ticketsHandler)

}

class InMemoryTicketStream[T<: Ticket](clientInfoRepository: ClientInfoRepository.Service,
                                                   ticketsDownloader: TicketsDownloader[T],
                                                   ticketsHandler: TicketsHandler[T]) extends TicketStreamProcessor {

  private def repeatUntil(streamId: UUID): UIO[Boolean] =
    clientInfoRepository.getInfo(streamId)
      .map(_.exists(_.continueProcessing))
      .fold(
        _ => true,
        continue => !continue
      )

  override def startDownloading(streamId: UUID): URIO[Clock, Unit] =
    ZStream
     .fromEffect(getClientInfo(streamId))
     .mapM(info => ticketsDownloader.download(info._1, info._2, info._3))
     .mapM(ticketsHandler.handle)
     .mapM(status => updateProcessedUntil(streamId, status.processedUntil))
     .onError(
       _ => finalizeStream(streamId)
     )
     .runDrain
     .delay(Duration.ofSeconds(10))
     .repeatUntilM(_ => repeatUntil(streamId))
      .fold(
        error => println(error),
        _ => ()
      )

  private def getClientInfo(streamId: UUID): Task[(Instant, String, String)] = {
    clientInfoRepository
      .getInfo(streamId)
      .flatMap{
        case Some(ClientInfoRow(_, _, authInfo, domain, Some(startFrom), _, _, _)) =>
          ZIO.succeed((startFrom, domain, authInfo))
        case _ => ZIO.fail(DownloadManager.ClientStreamMissing(streamId))
      }
  }

  override protected def updateProcessedUntil(streamId: UUID, processedUntil: Instant): UIO[Unit] = {
    clientInfoRepository.saveOffset(streamId, processedUntil)
  }

  private def finalizeStream(streamId: UUID) = {
      clientInfoRepository
        .setStatus(streamId, true, false)
  }
}
// offset fetch and save (time or cursor based)
// process event (has different tickets types and processing need)
// download event(different api to download from)