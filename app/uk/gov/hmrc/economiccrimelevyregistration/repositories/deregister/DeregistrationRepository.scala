/*
 * Copyright 2023 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uk.gov.hmrc.economiccrimelevyregistration.repositories.deregister

import org.mongodb.scala.bson.conversions.Bson
import org.mongodb.scala.model.Indexes.ascending
import org.mongodb.scala.model._
import play.api.libs.json.{Format, Json}
import uk.gov.hmrc.economiccrimelevyregistration.config.AppConfig
import uk.gov.hmrc.economiccrimelevyregistration.models.deregister.Deregistration
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.mongo.play.json.PlayMongoRepository
import uk.gov.hmrc.mongo.play.json.formats.MongoJavatimeFormats

import java.time.{Clock, Instant}
import java.util.concurrent.TimeUnit
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class DeregistrationRepository @Inject() (
  mongoComponent: MongoComponent,
  appConfig: AppConfig,
  clock: Clock
)(implicit ec: ExecutionContext)
    extends PlayMongoRepository[Deregistration](
      collectionName = "deregistration",
      mongoComponent = mongoComponent,
      domainFormat = DeregistrationRepository.format,
      indexes = Seq(
        IndexModel(
          Indexes.ascending("lastUpdated"),
          IndexOptions()
            .name("lastUpdatedIdx")
            .expireAfter(appConfig.mongoTtl, TimeUnit.SECONDS)
        ),
        IndexModel(ascending("internalId"), IndexOptions().name("internalIdIdx").unique(true))
      )
    ) {

  private def byId(id: String): Bson = Filters.equal("internalId", id)

  def keepAlive(id: String): Future[Boolean] =
    collection
      .updateOne(
        filter = byId(id),
        update = Updates.set("lastUpdated", Instant.now(clock))
      )
      .toFuture()
      .map(_ => true)

  def get(id: String): Future[Option[Deregistration]] =
    keepAlive(id).flatMap { _ =>
      collection
        .find(byId(id))
        .headOption()
    }

  def upsert(deregistration: Deregistration): Future[Unit] = {
    val updatedRegistration = deregistration.copy(lastUpdated = Some(Instant.now(clock)))

    collection
      .replaceOne(
        filter = byId(updatedRegistration.internalId),
        replacement = updatedRegistration,
        options = ReplaceOptions().upsert(true)
      )
      .toFuture()
      .map(_ => ())
  }

  def deleteRecord(id: String): Future[Unit] =
    collection
      .deleteOne(byId(id))
      .toFuture()
      .map(_ => ())
}

object DeregistrationRepository {
  implicit val instantFormat: Format[Instant] = MongoJavatimeFormats.instantFormat
  val format: Format[Deregistration]          = Json.format[Deregistration]
}
