package com.kaizo.ticketsdownloader.api


import java.time.Instant
import cats.effect._
import com.kaizo.ticketsdownloader.services.DownloadManager
import org.http4s.circe.CirceEntityCodec._
import org.http4s._
import org.http4s.dsl.Http4sDsl
import org.http4s.implicits._
import zio.{RIO, Task, UIO, URIO, ZIO}
import zio.interop.catz._
import org.http4s.circe.CirceEntityCodec._
import zio.clock.Clock
import scala.collection.mutable


class TicketsStreamRouteApp(downloadManager: DownloadManager)
  extends Http4sDsl[RIO[Clock, *]] with HttpApiJson {
  var ids = mutable.ListBuffer.empty[String]
  val routes = HttpRoutes.of[RIO[Clock, *]] {
    case GET -> Root / "status" => Ok("Hello world.")

    case req @ POST -> Root / "streams" =>
      for {
        registrationRequest <- req.as[StreamRegistration]
       resp <- downloadManager.registerStream(registrationRequest).foldM(
         _ =>  InternalServerError(),
         value => Ok(value))
      } yield resp


    case req @ PUT -> Root / "streams" / UUIDVar(streamID) / "start" =>
      for {
        startRequest <- req.as[StreamStartReq]
        resp <-  downloadManager.startStream(streamID, Some(startRequest.startFrom)).foldM(
          _ =>  InternalServerError(),
          value => Ok(value))
      } yield resp

    case PUT -> Root / "streams" / UUIDVar(streamID)/ "stop" => downloadManager.stopStream(streamID).foldM(
      _ =>  InternalServerError(),
      value => Ok(value)
    )
    case GET -> Root / "streams" => downloadManager.getAllStreams.foldM(
      _ =>  InternalServerError(),
      value => Ok(value)
    )
    }.orNotFound
}

object TicketsStreamRouteApp {
  def apply(downloadManager: DownloadManager): TicketsStreamRouteApp = new TicketsStreamRouteApp(downloadManager)
}