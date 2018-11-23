package com.michalplachta.influencerstats

import cats.effect.internals.IOContextShift
import cats.effect.{ContextShift, IO}
import com.michalplachta.influencerstats.cache.StatisticsCaching
import com.michalplachta.influencerstats.client.{HammockVideoClient, VideoClient}
import com.michalplachta.influencerstats.core.Statistics
import com.michalplachta.influencerstats.logging.{IoLogger, Logging}
import com.michalplachta.influencerstats.server.Server
import com.michalplachta.influencerstats.server.http4s.Http4sServer
import com.michalplachta.influencerstats.state.{CollectionsState, InMemMapState}
import com.typesafe.config.ConfigFactory

object Main extends App {
  val config        = ConfigFactory.load()
  val host          = config.getString("app.host")
  val port          = config.getInt("app.port")
  val youtubeUri    = config.getString("apis.youtubeUri")
  val youtubeApiKey = config.getString("apis.youtubeApiKey")

  implicit val contextShift: ContextShift[IO] = IOContextShift.global
  implicit val logging: Logging[IO]           = new IoLogger

  implicit val client: VideoClient[IO]     = new HammockVideoClient(youtubeUri, youtubeApiKey)
  implicit val state: CollectionsState[IO] = new InMemMapState[IO]
  implicit val server: Server[IO]          = new Http4sServer

  val statisticsCaching = new StatisticsCaching(state.fetchAllCollectionIds, Statistics.getInfluencerResults[IO])

  Server[IO]
    .serve(host, port, statisticsCaching.getCachedInfluencerResults)
    .unsafeRunSync()
}
