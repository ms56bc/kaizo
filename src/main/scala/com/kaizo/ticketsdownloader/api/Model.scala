package com.kaizo.ticketsdownloader.api


import java.time.Instant
import java.util.UUID
import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}



case class StreamRegistration(
                               clientName: String,
                               authInfo: String,
                               domain: String,
                               system: TicketingSystem
                             )

case class StreamInfo(
                       streamId: UUID,
                       clientName: String,
                       domain: String,
                       processedUntil: Option[Instant],
                       system: TicketingSystem,
                       isRunning: Boolean
                     )

sealed trait TicketingSystem extends Product with Serializable
case object TicketingSystem {
  case object ZenDesk extends TicketingSystem
}

sealed trait StreamStatus extends Product with Serializable
object StreamStatus {
  case object StreamAlreadyRunning extends StreamStatus
  case object StreamNotFound extends StreamStatus
  case object StreamStarted extends StreamStatus
  case object StreamTypeNotFound extends StreamStatus
}



trait HttpApiJson {
  implicit val xxxD: Decoder[TicketingSystem] = deriveDecoder
  implicit val xxxE: Encoder[TicketingSystem] = deriveEncoder
  implicit val StreamRegistrationDecoder: Decoder[StreamRegistration] = deriveDecoder
  implicit val StreamRegistrationEncoder: Encoder[StreamRegistration] = deriveEncoder
  implicit val StreamInfoDecoder: Decoder[StreamInfo] = deriveDecoder
  implicit val StreamInfoEncoder: Encoder[StreamInfo] = deriveEncoder
  implicit val StreamStatusDecoder: Decoder[StreamStatus] = deriveDecoder
  implicit val StreamStatusDEncoder: Encoder[StreamStatus] = deriveEncoder
}
