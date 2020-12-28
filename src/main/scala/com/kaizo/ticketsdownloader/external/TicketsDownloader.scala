package com.kaizo.ticketsdownloader.external


import java.time.Instant
import org.http4s.{Headers, MediaType, Method, Request, Uri}
import org.http4s.client.Client
import org.http4s.headers.`Content-Type`
import zio.{Task, ZManaged}
import zio.interop.catz._
import org.http4s.circe.CirceEntityCodec._

trait TicketsDownloader[T <: Ticket] {
  def download(startFrom: Instant): Task[List[T]]
}

object TicketsDownloader {
  sealed abstract class  DownloadError(message: String) extends Exception(message)
  def zenDesk(client: ZManaged[Any, Throwable, Client[Task]]): TicketsDownloader[ZenDeskTicket] = new ZenDeskTicketDownloader(client)
}

class ZenDeskTicketDownloader(client: ZManaged[Any, Throwable, Client[Task]]) extends TicketsDownloader[ZenDeskTicket] with ZenDeskTicketJson {
  val baseUri: Uri = Uri(path = "https://developer.zendesk.com/rest_api/docs/support/incremental_export")
  /*override def download(startFrom: Instant): Task[List[ZenDeskTicket]] = {
    client.use(client => {
    val requestBody: ZenDeskTicket = ???
    val authTokenUri: Uri = baseUri.addPath(s"/api/v2/incremental/tickets.json?start_time=$startFrom")
    val request = Request[Task](
      method = Method.POST,
      uri = authTokenUri,
      headers = Headers.of(`Content-Type`(MediaType.application.json))
    ).withEntity(requestBody)

    client.run(request).use{ resp =>
      if (resp.status.isSuccess) resp.as[List[ZenDeskTicket]]
      else
        resp
          .as[List[ZenDeskTicket]]
    }
    })
  }*/

  override def download(startFrom: Instant): Task[List[ZenDeskTicket]] = {
   Task.succeed(List(ZenDeskTicket(
     organizations = Organizations(OrganisationFields("","", ""), tags = Tags("simley", "teapot")),
     count = 10,
     endOfStream = true,
     endTime = Instant.now,
     nextPage = ""
   )))
  }
}

// auth failure
// when to poll database for updated credentials.
// types of ticketing system to consider
