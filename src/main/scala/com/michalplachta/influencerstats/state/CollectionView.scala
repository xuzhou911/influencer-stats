package com.michalplachta.influencerstats.state

import com.michalplachta.influencerstats.core.model.Collection
import simulacrum.typeclass

@typeclass
trait CollectionView[F[_]] {
  def fetchCollection(id: String): F[Option[Collection]]
}
