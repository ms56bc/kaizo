package com.kaizo.ticketsdownloader.external

import java.time.Instant

import com.kaizo.ticketsdownloader.api.TicketingSystem
import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}


case class Organizations(organizations: OrganisationFields, tags: Tags)

case class OrganisationFields(datapudding: String, orgField1: String, orgField2: String)

case class Tags(smiley: String, teapotKettle: String)

sealed trait Ticket extends Product with Serializable

case class ZenDeskTicket(
                          organizations: Organizations,
                          count: Long,
                          endOfStream: Boolean,
                          endTime: Instant,
                          nextPage: String) extends Ticket


trait ZenDeskTicketJson {
  implicit val tagsDecoder: Decoder[Tags] = deriveDecoder
  implicit val organisationFieldsDecoder: Decoder[OrganisationFields] = deriveDecoder
  implicit val organizationsDecoder: Decoder[Organizations] = deriveDecoder
  implicit val zenDeskTicketDecoder: Decoder[ZenDeskTicket] = deriveDecoder
  implicit val ticketDecoder: Decoder[Ticket] = deriveDecoder

  implicit val tagsEncoder: Encoder[Tags] = deriveEncoder
  implicit val organisationFieldsEncoder: Encoder[OrganisationFields] = deriveEncoder
  implicit val organizationsEncoder: Encoder[Organizations] = deriveEncoder
  implicit val zenDeskTicketEncoder: Encoder[ZenDeskTicket] = deriveEncoder
  implicit val ticketEncoder: Encoder[Ticket] = deriveEncoder
}
