package com.gerritforge.analytics.engine.events

import com.typesafe.scalalogging.LazyLogging

// for documentation about this object model see https://gerrit-review.googlesource.com/Documentation/cmd-stream-events.html

trait GerritJsonEventParser extends Serializable {
  def fromJson(json: String): Option[GerritJsonEvent]
}


object EventParser extends GerritJsonEventParser with LazyLogging {
  val ChangeMergedEventType = "change-merged"
  val RefUpdatedEventType = "ref-updated"


  override def fromJson(json: String): Option[GerritJsonEvent] = {
    import org.json4s._
    import org.json4s.native.JsonMethods._

    implicit val formats = DefaultFormats

    def tryParse[T](jvalue: JValue)(implicit formats: Formats, mf: scala.reflect.Manifest[T]): Option[T] = {
      try {
        Some(jvalue.extract[T])
      } catch {
        case e: MappingException =>
          logger.warn(s"Invalid event of type `${mf.runtimeClass.getName}`", e)
          None
      }
    }

    parseOpt(json).flatMap { parsedJson =>
      parsedJson \ "type" match {
        case JString(ChangeMergedEventType) =>
          tryParse[ChangeMergedEvent](parsedJson)

        case JString(RefUpdatedEventType) =>
          tryParse[RefUpdatedEvent](parsedJson)

        case JNothing =>
          logger.warn("Invalid JSON object received, missing 'type' field")
          None

        case unexpectedJsonType =>
          logger.warn(s"Invalid JSON format for field 'type' `${unexpectedJsonType.getClass.getName}`")
          None

      }
    }
  }
}

case class GerritAccount(name: String, email: String, username: String)

case class GerritComment(reviewer: GerritAccount, message: String)

case class GerritChange(project: String,
                        branch: String,
                        topic: String,
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
                           draft: Boolean = false,
                           kind: String,
                           uploader: GerritAccount,
                           author: GerritAccount,
                           approvals: List[GerritApproval],
                           parents: List[String],
                           createdOn: Long,
                           sizeInsertions: Int,
                           sizeDeletions: Int
                         )


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
                            submitter: GerritAccount,
                            override val eventCreatedOn: Long
                          ) extends GerritRefHasNewRevisionEvent {
  override val `type`: String = "ref-updated"

  override def account: GerritAccount = submitter

  def modifiedProject: String = refUpdate.project

  def modifiedRef: String = refUpdate.refName

  def newRev: String = refUpdate.newRev
}