package ch.epfl.bluebrain.nexus.perf.config

import AppConfig._
import akka.http.scaladsl.model.Uri

import scala.concurrent.duration.FiniteDuration

final case class AppConfig(http: HttpConfig,
                           kg: KgConfig,
                           createConfig: CreateConfig,
                           fetchConfig: FetchConfig,
                           updateConfig: UpdateConfig,
                           tagConfig: TagConfig,
                           attachmentsConfig: AttachmentsConfig)

object AppConfig {

  final case class HttpConfig(token: String, retries: Int)

  final case class KgConfig(base: Uri)

  final case class CreateConfig(project: Int, size: Int, parallelUsers: Int)

  final case class FetchConfig(project: Int, duration: FiniteDuration, reads: Int, writes: Int, users: Int)

  final case class UpdateConfig(project: Int, revisions: Int, revisionsStep: Int)

  final case class TagConfig(tags: Int)

  final case class AttachmentsConfig(project: Int,
                                     instances: Int,
                                     attachmentSize: Int,
                                     attachmentsPerInstance: Int,
                                     parallelUsers: Int,
                                     duration: FiniteDuration)

}
