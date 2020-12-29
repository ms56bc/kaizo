package com.kaizo.ticketsdownloader.repository


import java.time.Instant
import java.util.UUID

import com.kaizo.ticketsdownloader.api.TicketingSystem
import com.kaizo.ticketsdownloader.repository.ClientInfoRepository.{AlreadyExists, ClientInfoRow, RepositoryError}
import zio.{IO, RefM, UIO, ZIO}


class InMemoryClientInfoRepository(state: RefM[Map[UUID, ClientInfoRow]]) extends ClientInfoRepository.Service {


  override def getInfo(streamId: UUID): IO[RepositoryError, Option[ClientInfoRow]] =
    for {
      clientsInfo <- state.get
    } yield clientsInfo.get(streamId)

  override def setStatus(streamId: UUID, isRunning: Boolean): UIO[Unit] =
    for {
      res <- state.update(state => state.get(streamId) match {
        case Some(stream) => ZIO.succeed(state + (streamId -> stream.copy(isRunning = isRunning)))
        case None => ZIO.succeed(state)
      })
    } yield res


  override def setStartFrom(streamId: UUID, startFrom: Option[Instant]): IO[RepositoryError, Unit] =  for {
    res <- state.update(state => state.get(streamId) match {
      case Some(stream) => ZIO.succeed(state + (streamId -> stream.copy(startFrom = startFrom)))
      case None => ZIO.succeed(state)
    })
  } yield res

  override def registerClient(streamId: UUID,
                              clientName: String,
                              authInfo: String,
                              domain: String,
                              startFrom: Option[Instant],
                              system: TicketingSystem,
                              isRunning: Boolean): IO[RepositoryError, ClientInfoRow] = {
    val row = ClientInfoRow(streamId, clientName, authInfo, domain, startFrom, system, isRunning)
    for {
      _ <- state.update(state => state.get(streamId) match {
        case Some(_) => ZIO.fail(AlreadyExists(clientName, streamId): RepositoryError)
        case None => ZIO.succeed(state + (streamId -> row))
      })
    } yield row
  }

  override def saveOffset(streamId: UUID, processedUntil: Instant): UIO[Unit] =
    for {
      res <- state.update(state => state.get(streamId) match {
        case Some(stream) => ZIO.succeed(state + (streamId -> stream.copy(startFrom = Some(processedUntil))))
        case None => ZIO.succeed(state)
      })
    } yield res

  override def getAllStreams: IO[RepositoryError, List[ClientInfoRow]] =
    for {
      res <- state.get
    } yield res.values.toList
}