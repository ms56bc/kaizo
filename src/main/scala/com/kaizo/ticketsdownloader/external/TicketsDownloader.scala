package com.kaizo.ticketsdownloader.external


import java.time.Instant
import cats.MonadError
import org.http4s.Uri.{Authority, RegName, Scheme}
import org.http4s._
import org.http4s.client.Client
import org.http4s.headers.{Authorization, `Content-Type`}
import zio.Task
import zio.interop.catz._
import org.http4s.circe.CirceEntityCodec._
import cats.syntax.option._
import scala.collection.immutable.Seq


trait TicketsDownloader[T <: Ticket] {
  def download(startFrom: Instant, domain: String, accessToken: String): Task[T]
}

object TicketsDownloader {
  sealed abstract class  DownloadError(message: String) extends Exception(message)

  def zenDesk(client: Client[Task]): TicketsDownloader[ZenDeskResponse] =
    new ZenDeskTicketDownloader(client)
}

class ZenDeskTicketDownloader(client: Client[Task]) extends TicketsDownloader[ZenDeskResponse] with ZenDeskTicketJson {

  private def baseUri(domain: String): Uri = Uri(
    scheme = Scheme.https.some,
    authority = Authority(host = RegName(s"$domain.zendesk.com")).some
  )

  private def parseErrorResponse(response: Response[Task]) =
    response
      .as[String](implicitly[MonadError[Task, Throwable]], EntityDecoder.text)
      .flatMap{ (e: String) =>
        Task.fail(new Exception(e))
      }


  override def download(startFrom: Instant, domain: String, accessToken: String): Task[ZenDeskResponse] = {
    val authTokenUri: Uri = baseUri(domain).addPath(s"/api/v2/incremental/tickets.json")
      .setQueryParams(Map("start_time" -> Seq(startFrom.getEpochSecond), "per_page" -> Seq(10L)))

    val request = Request[Task](
      method = Method.GET,
      uri = authTokenUri,
      headers = Headers.of(`Content-Type`(MediaType.application.json), Authorization(Credentials.Token(AuthScheme.Bearer, accessToken))),
    )
    client.run(request).use{ resp =>
      if (resp.status.isSuccess) resp.as[ZenDeskResponse]
      else parseErrorResponse(resp)
    }
  }
}

// auth failure
// when to poll database for updated credentials.
// types of ticketing system to consider
// not handling all errors properly
