// Copyright (C) 2018 GerritForge Ltd
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.gerritforge.analytics.gitcommits.engine.events

import com.typesafe.scalalogging.LazyLogging
import scala.util.{Failure, Success, Try}

// for documentation about this object model see https://gerrit-review.googlesource.com/Documentation/cmd-stream-events.html

trait GerritJsonEventParser extends Serializable {
  def fromJson(json: String): Try[GerritJsonEvent]
}

object EventParser extends GerritJsonEventParser with LazyLogging {
  val ChangeMergedEventType = "change-merged"
  val RefUpdatedEventType   = "ref-updated"

  override def fromJson(json: String): Try[GerritJsonEvent] = {
    import org.json4s._
    import org.json4s.native.JsonMethods._

    implicit val formats: DefaultFormats.type = DefaultFormats

    def tryParse[T](jvalue: JValue)(implicit formats: Formats,
                                    mf: scala.reflect.Manifest[T]): Try[T] = {
      try {
        Success(jvalue.extract[T])
      } catch {
        case e: MappingException =>
          Failure(
            MappingException(s"Invalid event of type `${mf.runtimeClass.getName}`: ${e.getMessage}",
                             e))
      }
    }

    Try(parse(json)).flatMap { parsedJson =>
      parsedJson \ "type" match {
        case JString(ChangeMergedEventType) =>
          tryParse[ChangeMergedEvent](parsedJson)

        case JString(RefUpdatedEventType) =>
          tryParse[RefUpdatedEvent](parsedJson)

        case JNothing =>
          Failure(
            new IllegalArgumentException("Invalid JSON object received, missing 'type' field"))

        case JString(unsupportedType) =>
          Failure(new IllegalArgumentException(s"Unsupported event type '$unsupportedType'"))

        case unexpectedJsonType =>
          Failure(
            new IllegalArgumentException(
              s"Invalid JSON format for field 'type' `${unexpectedJsonType.getClass.getName}`"))
      }
    }
  }
}

case class GerritAccount(name: String, email: String, username: String)
object GerritAccount {
  val NoAccountInfo = GerritAccount("", "", "")
}

case class GerritComment(reviewer: GerritAccount, message: String)

case class GerritChange(project: String,
                        branch: String,
                        topic: Option[String],
                        id: String,
                        number: String,
                        subject: String,
                        commitMessage: String,
                        owner: GerritAccount,
                        url: String,
                        createdOn: Long,
                        lastUpdated: Option[Long],
                        comments: List[GerritComment])

case class GerritApproval(
    `type`: String,
    value: String,
    updated: Boolean = false,
    oldValue: String,
    by: GerritAccount
)

case class GerritPatchSet(
    number: String,
    revision: String,
    ref: String,
    draft: Option[Boolean],
    kind: String,
    uploader: GerritAccount,
    author: GerritAccount,
    approvals: List[GerritApproval],
    parents: List[String],
    createdOn: Long,
    sizeInsertions: Int,
    sizeDeletions: Int
) {
  def isDraft: Boolean = draft.getOrElse(false)
}

sealed trait GerritJsonEvent {
  def `type`: String

  def eventCreatedOn: Long

  def account: GerritAccount
}

sealed trait GerritRepositoryModifiedEvent extends GerritJsonEvent {
  def modifiedProject: String

  def modifiedRef: String
}

sealed trait GerritRefHasNewRevisionEvent extends GerritRepositoryModifiedEvent {
  def newRev: String
}

sealed trait GerritChangeBasedEvent extends GerritRefHasNewRevisionEvent {
  def change: GerritChange

  def patchSet: GerritPatchSet

  //def files: Set[String]
}

case class GerritChangeKey(id: String, `type`: Option[String], eventCreatedOn: Option[Long])

//https://gerrit-review.googlesource.com/Documentation/cmd-stream-events.html#_change_merged
case class ChangeMergedEvent(
    override val change: GerritChange,
    override val patchSet: GerritPatchSet,
    submitter: GerritAccount,
    override val newRev: String,
    override val eventCreatedOn: Long,
    changeKey: GerritChangeKey
) extends GerritChangeBasedEvent {
  override val `type`: String = "change-merged"

  override def account: GerritAccount = submitter

  def modifiedProject: String = change.project

  def modifiedRef: String = patchSet.ref
}

case class GerritRefUpdate(project: String, refName: String, oldRev: String, newRev: String)

case class RefUpdatedEvent(
    refUpdate: GerritRefUpdate,
    submitter: Option[GerritAccount],
    override val eventCreatedOn: Long
) extends GerritRefHasNewRevisionEvent {
  override val `type`: String = "ref-updated"

  override def account: GerritAccount = submitter.getOrElse(GerritAccount.NoAccountInfo)

  def modifiedProject: String = refUpdate.project

  def modifiedRef: String = refUpdate.refName

  def newRev: String = refUpdate.newRev
}
