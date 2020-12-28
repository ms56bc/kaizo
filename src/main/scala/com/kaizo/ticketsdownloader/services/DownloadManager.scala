package com.kaizo.ticketsdownloader.services

import java.time.Instant
import java.util.UUID
import com.kaizo.ticketsdownloader.api.{StreamInfo, StreamRegistration, StreamStatus, TicketingSystem}
import com.kaizo.ticketsdownloader.repository.ClientInfoRepository
import com.kaizo.ticketsdownloader.repository.ClientInfoRepository.ClientInfoRow
import com.kaizo.ticketsdownloader.services.DownloadManager.DownloadManagerError
import zio.clock.Clock
import zio.{IO, ZIO}


trait DownloadManager {
  def registerStream(streamRegistration: StreamRegistration): IO[DownloadManagerError, StreamInfo]
  def startStream(streamId: UUID, startTime: Option[Instant]): ZIO[Clock, DownloadManagerError, StreamStatus]
  def stopStream(streamId: UUID): IO[DownloadManagerError, Unit]
  def getAllStreams: IO[DownloadManagerError, List[StreamInfo]]
}

object DownloadManager {
  abstract class DownloadManagerError(msg: String) extends Exception(msg)
  case class ClientStreamMissing(streamId: UUID) extends DownloadManagerError(s"Stream Missing. $streamId")
  case class GenericError(msg: String) extends DownloadManagerError(msg)

  def live(clientInfoRepository: ClientInfoRepository.Service,
           streamProcessors: Map[TicketingSystem, TicketStreamProcessor]) = new DownloadManagerImpl(clientInfoRepository, streamProcessors)

  class DownloadManagerImpl(clientInfoRepository: ClientInfoRepository.Service,
                            streamProcessors: Map[TicketingSystem, TicketStreamProcessor]
                           ) extends DownloadManager {

    private def _startStream(streamId: UUID,
                             clientStreamInfo: ClientInfoRow, startTime: Option[Instant]): ZIO[Clock, DownloadManagerError, StreamStatus] = {
      val res = streamProcessors.get(clientStreamInfo.system) match {
        case Some(streamProcessor) => for {
          _ <- clientInfoRepository.setStatus(streamId, continueProcessing = true, isRunning = true)
          _ <- clientInfoRepository.setStartFrom(streamId, startTime)
          _ <- streamProcessor.startDownloading(streamId).fork
        } yield StreamStatus.StreamStarted
        case None => ZIO.succeed(StreamStatus.StreamTypeNotFound)
      }
      res.mapError(error => GenericError(error.getMessage))
    }

    override def startStream(streamId: UUID, startTime: Option[Instant]): ZIO[Clock, DownloadManagerError, StreamStatus] = {
     for {
        clientStreamInfo <- clientInfoRepository.getInfo(streamId).mapError(error => GenericError(error.getMessage))
        res <- clientStreamInfo match {
          case Some(clientStreamInfo) if clientStreamInfo.isRunning => ZIO.succeed(StreamStatus.StreamAlreadyRunning)
          case Some(clientStreamInfo)  => _startStream(streamId, clientStreamInfo, startTime)
          case None => ZIO.succeed(StreamStatus.StreamNotFound)
        }
      } yield res
    }

    override def stopStream(streamId: UUID): IO[DownloadManagerError, Unit] =
     clientInfoRepository
       .setStatus(streamId, continueProcessing = false, isRunning = false)

    override def registerStream(streamRegistration: StreamRegistration): IO[DownloadManagerError, StreamInfo] = {
      clientInfoRepository.registerClient(
        streamId = UUID.randomUUID(),
        clientName = streamRegistration.clientName,
        domain = streamRegistration.domain,
        authInfo = streamRegistration.authInfo,
        startFrom = None,
        system = streamRegistration.system,
        continueProcessing = false,
        isRunning = false
      ).bimap(
          error => DownloadManager.GenericError(error.getMessage),
          toStreamInfo)
    }

    override def getAllStreams: IO[DownloadManagerError, List[StreamInfo]] = {
      clientInfoRepository
        .getAllStreams
        .bimap(error => DownloadManager.GenericError(error.getMessage),
          _.map(toStreamInfo))
    }

    private def toStreamInfo(stream: ClientInfoRow): StreamInfo =
      StreamInfo(stream.streamId, stream.clientName, stream.domain, stream.startFrom, stream.system, stream.isRunning)

  }
}

