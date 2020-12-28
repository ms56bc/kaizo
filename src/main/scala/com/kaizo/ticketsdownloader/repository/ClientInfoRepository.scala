package com.kaizo.ticketsdownloader.repository


import java.time.Instant
import java.util.UUID

import com.kaizo.ticketsdownloader.api.TicketingSystem
import zio.{Has, IO, RefM, UIO, ZLayer}

object ClientInfoRepository {

  case class ClientInfoRow(streamId: UUID,
                           clientName: String,
                           authInfo: String,
                           domain: String,
                           startFrom: Option[Instant],
                           system: TicketingSystem,
                           continueProcessing: Boolean,
                           isRunning: Boolean)

  sealed abstract class RepositoryError(msg: String) extends Exception(msg)
  case class AlreadyExists(clientName: String, streamID: UUID) extends RepositoryError(s"client already exists $clientName and $streamID")

  trait Service {
    def getInfo(streamId: UUID): IO[RepositoryError, Option[ClientInfoRow]]

    def setStatus(streamId: UUID, continueProcessing: Boolean, isRunning: Boolean): UIO[Unit]
    def setStartFrom(streamId: UUID, startFrom:Option[Instant]): IO[RepositoryError, Unit]

    def registerClient(streamId: UUID,
                       clientName: String,
                       authInfo: String,
                       domain: String,
                       startFrom: Option[Instant],
                       system: TicketingSystem,
                       continueProcessing: Boolean,
                       isRunning: Boolean): IO[RepositoryError, ClientInfoRow]

    def saveOffset(streamId: UUID, processedUntil: Instant): UIO[Unit]

    def getAllStreams: IO[RepositoryError, List[ClientInfoRow]]
  }

  def  inMemory(clientStreams: RefM[Map[UUID, ClientInfoRow]]): ClientInfoRepository.Service =  new InMemoryClientInfoRepository(clientStreams)

}