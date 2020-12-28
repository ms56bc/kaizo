package com.kaizo.ticketsdownloader.services

import java.time.Instant

import com.kaizo.ticketsdownloader.external.{Ticket, ZenDeskResponse, ZenDeskTicket}
import com.kaizo.ticketsdownloader.services.TicketsHandler.ProcessingStatus
import zio.{UIO, ZIO}

trait TicketsHandler[T <: Ticket] {
  def handle(ticket: T): UIO[ProcessingStatus]
}

object TicketsHandler {
  case class ProcessingStatus(processedUntil: Instant)

  val zenDeskConsole = new ZenDeskConsoleTicketProcessor

  class ZenDeskConsoleTicketProcessor extends TicketsHandler[ZenDeskResponse] {
    override def handle(ticket: ZenDeskResponse): UIO[ProcessingStatus] =
      for {
      _<- ZIO.foreach_(ticket.tickets)(ticket => ZIO.succeed(println(ticket)))
    } yield ProcessingStatus(Instant.ofEpochSecond(ticket.endTime))
  }
}
