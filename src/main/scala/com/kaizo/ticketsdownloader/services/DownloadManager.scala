package com.kaizo.ticketsdownloader.services


import java.time.Instant
import java.util.UUID
import com.kaizo.ticketsdownloader.api.{StreamInfo, StreamRegistration, StreamStatus, TicketingSystem}
import com.kaizo.ticketsdownloader.repository.ClientInfoRepository
import com.kaizo.ticketsdownloader.repository.ClientInfoRepository.{ClientInfoRow, RepositoryError}
import com.kaizo.ticketsdownloader.services.DownloadManager.DownloadManagerError
import zio.clock.Clock
import zio.{Fiber, IO, RefM, Task, ZIO}


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
           streamProcessors: Map[TicketingSystem, TicketStreamProcessor],
           streamsState: RefM[Map[UUID, Fiber.Runtime[Throwable, Unit]]]) =
    new DownloadManagerImpl(clientInfoRepository, streamProcessors, streamsState)

  class DownloadManagerImpl(clientInfoRepository: ClientInfoRepository.Service,
                            streamProcessors: Map[TicketingSystem, TicketStreamProcessor],
                            streamsState: RefM[Map[UUID, Fiber.Runtime[Throwable, Unit]]]
                           ) extends DownloadManager {

    private def addStream(streamId: UUID, fiber: Fiber.Runtime[Throwable, Unit]): IO[RepositoryError, Unit] =
      for {
      res <- streamsState.update(state => state.get(streamId) match {
        case Some(_) => ZIO.succeed(state)
        case None => ZIO.succeed(state + (streamId -> fiber))
      })
    } yield res

    private def removeStream(streamId: UUID): Task[Unit] =
      for {
        res <- streamsState.update(state => state.get(streamId) match {
          case Some(fiber) => fiber.interrupt *> ZIO.succeed(state - streamId)
          case None => ZIO.succeed(state)
        })
      } yield res

    private def _startStream(streamId: UUID,
                             clientStreamInfo: ClientInfoRow, startTime: Option[Instant]): ZIO[Clock, DownloadManagerError, StreamStatus] = {
      val res = streamProcessors.get(clientStreamInfo.system) match {
        case Some(streamProcessor) => for {
          _ <- clientInfoRepository.setStatus(streamId,  isRunning = true)
          _ <- clientInfoRepository.setStartFrom(streamId, startTime)
          fiber <- streamProcessor.startDownloading(streamId).fork
          _<- addStream(streamId, fiber)
          _ = streamsState
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
      for {
          _ <- removeStream(streamId).mapError(error => DownloadManager.GenericError(error.getMessage))
          _ <- clientInfoRepository.setStatus(streamId,  isRunning = false)
        } yield ()

    override def registerStream(streamRegistration: StreamRegistration): IO[DownloadManagerError, StreamInfo] = {
      clientInfoRepository.registerClient(
        streamId = UUID.fromString("b96e2b74-e45a-4519-b33b-4745665a1000"),
        clientName = streamRegistration.clientName,
        domain = streamRegistration.domain,
        authInfo = streamRegistration.authInfo,
        startFrom = None,
        system = streamRegistration.system,
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

