package com.michalplachta.influencerstats.state

import cats.Monad
import cats.effect.Sync
import cats.implicits._
import cats.mtl.FunctorTell
import com.michalplachta.influencerstats.core.model.Collection
import monix.execution.atomic.Atomic

class InMemListState[F[_]: Monad: Sync](implicit F: FunctorTell[F, String]) extends CollectionsState[F] {
  private val state = Atomic(List.empty[(String, Collection)])

  def fetchCollection(id: String): F[Option[Collection]] = {
    def find(collections: List[(String, Collection)]): F[Option[Collection]] = {
      collections match {
        case x :: xs =>
          for {
            _          <- F.tell(s"checking if $x is the collection we are looking for")
            collection <- if (x._1 == id) Sync[F].pure(Some(x._2)) else find(xs)
          } yield collection
        case Nil =>
          Sync[F].pure(None)
      }
    }

    for {
      _      <- F.tell(s"looking for collection with id $id")
      result <- find(state.get)
    } yield result
  }

  def saveCollection(id: String, collection: Collection): F[Unit] = {
    for {
      _ <- F.tell(s"saving collection $collection under id $id")
      _ <- Sync[F].delay(state.transform(_ :+ ((id, collection))))
      _ <- F.tell(s"state now contains ${state.get.size} collections")
    } yield ()
  }

  def fetchAllCollectionIds: F[List[String]] = {
    Sync[F].delay {
      state.get.map(_._1)
    }
  }
}
