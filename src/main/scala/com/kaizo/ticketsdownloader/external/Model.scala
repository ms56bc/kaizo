package com.kaizo.ticketsdownloader.external

import java.time.Instant
import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}


sealed trait Ticket extends Product with Serializable

case class ZenDeskTicket(url: String,
                          id: Long)

case class ZenDeskResponse(
                          tickets: List[ZenDeskTicket],
                          endOfStream: Boolean,
                          endTime: Long) extends Ticket


trait ZenDeskTicketJson {

  implicit val zenDeskTicketDecoder: Decoder[ZenDeskTicket] = Decoder
    .forProduct2("url", "id")(
      (url, id) => ZenDeskTicket(url, id)
    )
  implicit val ticketDecoder: Decoder[Ticket] = deriveDecoder
  implicit val ZenDeskResponseDecoder: Decoder[ZenDeskResponse] =  Decoder
    .forProduct3("tickets", "end_of_stream", "end_time")(
      (tickets, endOfStream, endTime) => ZenDeskResponse(tickets, endOfStream, endTime)
    )


  implicit val zenDeskTicketEncoder: Encoder[ZenDeskTicket] = deriveEncoder
  implicit val ticketEncoder: Encoder[Ticket] = deriveEncoder
  implicit val ZenDeskResponseEncoder: Encoder[ZenDeskResponse] = deriveEncoder
}
