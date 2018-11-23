package com.michalplachta.influencerstats.core
import cats.Monad
import cats.implicits._
import com.michalplachta.influencerstats.client.VideoClient
import com.michalplachta.influencerstats.core.model._
import com.michalplachta.influencerstats.logging.Logging
import com.michalplachta.influencerstats.state.CollectionsState

object Statistics {
  def calculate(items: List[InfluencerItem]): InfluencerResults = {
    items.foldLeft(InfluencerResults(0, 0, 0)) { (results, item) =>
      InfluencerResults(
        impressions = results.impressions + item.views,
        engagements = results.engagements + item.comments + item.likes + item.dislikes,
        score = results.engagements + item.likes
      )
    }
  }

  def getInfluencerResults[F[_]: Monad: VideoClient: CollectionsState: Logging](id: String): F[InfluencerResults] = {
    for {
      _          <- Logging[F].info(s"trying to fetch collection with id $id")
      collection <- CollectionsState[F].fetchCollection(id)
      _          <- Logging[F].info(s"fetched collection: $collection")
      videoIds   = collection.map(_.videos).getOrElse(List.empty)
      _          <- Logging[F].info(s"going to make ${videoIds.size} fetches")
      responses  <- videoIds.map(VideoClient[F].fetchVideoListResponse).sequence
      _          <- Logging[F].info(s"got responses: $responses")
      items = responses.flatMap(
        _.items.map(_.statistics).map(video => InfluencerItem(video.viewCount, video.likeCount, 0, 0))
      )
      _ <- Logging[F].info(s"got list of influencer items: $items")
    } yield calculate(items)
  }
}
