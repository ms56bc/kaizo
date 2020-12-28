package com.kaizo.ticketsdownloader.services

import java.time.Instant

import com.kaizo.ticketsdownloader.external.{Ticket, ZenDeskTicket}
import com.kaizo.ticketsdownloader.services.TicketsHandler.ProcessingStatus
import zio.{UIO, ZIO}

trait TicketsHandler[T <: Ticket] {
  def handle(tickets: List[T]): UIO[ProcessingStatus]
}

object TicketsHandler {
  case class ProcessingStatus(processedUntil: Instant)
  val zenDeskConsole = new ZenDeskConsoleTicketProcessor
  class ZenDeskConsoleTicketProcessor extends TicketsHandler[ZenDeskTicket] {
    override def handle(tickets: List[ZenDeskTicket]): UIO[ProcessingStatus] =
      for {
      _<- ZIO.foreach_(tickets)(ticket => ZIO.succeed(println(ticket)))
    } yield processesUntil(tickets)

   private[services] def processesUntil(tickets: List[ZenDeskTicket]): ProcessingStatus = {
      val endTime = tickets.maxBy(_.endTime).endTime
      ProcessingStatus(endTime)
    }
  }
}
