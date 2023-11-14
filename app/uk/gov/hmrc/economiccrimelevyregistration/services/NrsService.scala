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

package uk.gov.hmrc.economiccrimelevyregistration.services

import cats.data.EitherT
import play.api.Logging
import play.api.http.{HeaderNames, MimeTypes}
import play.api.libs.json.{JsObject, JsString}
import play.api.mvc.Headers
import uk.gov.hmrc.economiccrimelevyregistration.connectors.NrsConnector
import uk.gov.hmrc.economiccrimelevyregistration.controllers.ErrorHandler
import uk.gov.hmrc.economiccrimelevyregistration.models.errors.NrsSubmissionError
import uk.gov.hmrc.economiccrimelevyregistration.models.nrs._
import uk.gov.hmrc.economiccrimelevyregistration.models.requests.AuthorisedRequest
import uk.gov.hmrc.http.HeaderCarrier

import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.time.{Clock, Instant}
import java.util.Base64
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class NrsService @Inject() (nrsConnector: NrsConnector, clock: Clock)(implicit
  ec: ExecutionContext
) extends Logging
    with ErrorHandler {

  def submitToNrs(
    optBase64EncodedNrsSubmissionHtml: Option[String],
    eclRegistrationReference: String,
    eventName: String
  )(implicit
    hc: HeaderCarrier,
    request: AuthorisedRequest[_]
  ): EitherT[Future, NrsSubmissionError, NrsSubmissionResponse] = {

    for {
      authToken                      <- getUserAuthToken(request.headers, HeaderNames.AUTHORIZATION)
      headerData                      = new JsObject(request.headers.toMap.map(x => x._1 -> JsString(x._2 mkString ",")))
      base64EncodedNrsSubmissionHtml <- getBase64EncodedNrsSubmissionHtml(optBase64EncodedNrsSubmissionHtml)
      nrsSearchKeys                   = NrsSearchKeys(eclRegistrationReference = eclRegistrationReference)
      nrsMetadata                     = assembleNrsMetadata(
                                          base64EncodedNrsSubmissionHtml,
                                          request.nrsIdentityData,
                                          authToken,
                                          headerData,
                                          nrsSearchKeys,
                                          eventName
                                        )
    nrsSubmission = NrsSubmission(base64EncodedNrsSubmissionHtml,  nrsMetadata)
    } yield NrsSubmissionResponse("")

    // val userAuthToken: String                  = request.headers.get(HeaderNames.AUTHORIZATION).get
    //val headerData: JsObject                   = new JsObject(request.headers.toMap.map(x => x._1 -> JsString(x._2 mkString ",")))
//    val base64EncodedNrsSubmissionHtml: String = optBase64EncodedNrsSubmissionHtml.getOrElse(
//      throw new IllegalStateException("Base64 encoded NRS submission HTML not found in registration data")
//    )

    //val nrsSearchKeys: NrsSearchKeys = NrsSearchKeys(eclRegistrationReference = eclRegistrationReference)

//    val nrsMetadata = NrsMetadata(
//      businessId = "ecl",
//      notableEvent = eventName,
//      payloadContentType = MimeTypes.HTML,
//      payloadSha256Checksum = payloadSha256Checksum(base64EncodedNrsSubmissionHtml),
//      userSubmissionTimestamp = Instant.now(clock),
//      identityData = request.nrsIdentityData,
//      userAuthToken = userAuthToken,
//      headerData = headerData,
//      searchKeys = nrsSearchKeys
//    )

//    val nrsSubmission = NrsSubmission(
//      payload = base64EncodedNrsSubmissionHtml,
//      metadata = nrsMetadata
//    )

    nrsConnector
      .submitToNrs(nrsSubmission)
      .map { nrsSubmissionResponse =>
        logger.info(s"Success response received from NRS with submission ID: ${nrsSubmissionResponse.nrSubmissionId}")
        nrsSubmissionResponse
      }
      .recover { case e: Throwable =>
        logger.error(
          s"Failed to send NRS submission for ECL reference $eclRegistrationReference with notable event $eventName: ${e.getMessage}"
        )
        throw e
      }
  }

  private def payloadSha256Checksum(base64EncodedNrsSubmissionHtml: String): String = {
    val decodedHtml: String = new String(Base64.getDecoder.decode(base64EncodedNrsSubmissionHtml))

    MessageDigest
      .getInstance("SHA-256")
      .digest(decodedHtml.getBytes(StandardCharsets.UTF_8))
      .map("%02x".format(_))
      .mkString
  }

  def getUserAuthToken(headers: Headers, key: String): EitherT[Future, NrsSubmissionError, String] =
    EitherT {
      Future.successful(
        headers.get(key) match {
          case Some(value) => Right(value)
          case None        => Left(NrsSubmissionError.InternalUnexpectedError("User auth token not present in header", None))
        }
      )
    }

  def getBase64EncodedNrsSubmissionHtml(html: Option[String]): EitherT[Future, NrsSubmissionError, String] =
    EitherT {
      Future.successful(
        html match {
          case Some(value) => Right(value)
          case None        =>
            Left(
              NrsSubmissionError
                .InternalUnexpectedError("Base64 encoded NRS submission HTML not found in registration data", None)
            )
        }
      )
    }

  def assembleNrsMetadata(
    base64EncodedNrsSubmissionHtml: String,
    nrsIdentityData: NrsIdentityData,
    userAuthToken: String,
    headerData: JsObject,
    nrsSearchKeys: NrsSearchKeys,
    eventName: String
  ): NrsMetadata =
    NrsMetadata(
      businessId = "ecl",
      notableEvent = eventName,
      payloadContentType = MimeTypes.HTML,
      payloadSha256Checksum = payloadSha256Checksum(base64EncodedNrsSubmissionHtml),
      userSubmissionTimestamp = Instant.now(clock),
      identityData = nrsIdentityData,
      userAuthToken = userAuthToken,
      headerData = headerData,
      searchKeys = nrsSearchKeys
    )
}
