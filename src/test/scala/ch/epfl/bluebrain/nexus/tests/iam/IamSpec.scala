package ch.epfl.bluebrain.nexus.tests.iam

import java.util.regex.Pattern.quote

import akka.http.scaladsl.model.HttpMethods._
import akka.http.scaladsl.model.{StatusCodes, HttpRequest => Req}
import ch.epfl.bluebrain.nexus.commons.http.JsonLdCirceSupport._
import ch.epfl.bluebrain.nexus.commons.test.Randomness
import ch.epfl.bluebrain.nexus.tests.BaseSpec
import ch.epfl.bluebrain.nexus.tests.iam.types._
import org.scalatest.concurrent.Eventually
import org.scalatest.{CancelAfterFailure, Inspectors}

class IamSpec extends BaseSpec with Inspectors with CancelAfterFailure with Eventually with Randomness {

  "manage realms" should {
    val realmLabel = genString(8)

    "create realm" in {
      val body =
        jsonContentOf("/iam/realms/create.json",
                      Map(
                        quote("{realm}") -> config.iam.testRealm
                      )).toEntity

      cl(Req(PUT, s"$iamBase/realms/$realmLabel", headersGroup, body))
        .mapJson { (json, result) =>
          result.status shouldEqual StatusCodes.Created
          json.removeFields("_createdAt", "_createdBy", "_updatedAt", "_updatedBy") shouldEqual jsonContentOf(
            "/iam/realms/ref-response.json",
            Map(
              quote("{realm}")      -> config.iam.testRealm,
              quote("{iamBase}")    -> config.iam.uri.toString,
              quote("{label}")      -> realmLabel,
              quote("{rev}")        -> "1",
              quote("{deprecated}") -> "false"
            )
          )
        }

    }

    "fetch realm" in {
      cl(Req(GET, s"$iamBase/realms/$realmLabel", headersGroup))
        .mapJson { (json, result) =>
          result.status shouldEqual StatusCodes.OK
          json.removeFields("_createdAt", "_createdBy", "_updatedAt", "_updatedBy") shouldEqual jsonContentOf(
            "/iam/realms/fetch-response.json",
            Map(
              quote("{realm}")   -> config.iam.testRealm,
              quote("{iamBase}") -> config.iam.uri.toString,
              quote("{label}")   -> realmLabel
            )
          )
        }

    }
    "update realm" in {

      val body =
        jsonContentOf("/iam/realms/update.json",
                      Map(
                        quote("{realm}") -> config.iam.testRealm
                      )).toEntity
      cl(Req(PUT, s"$iamBase/realms/$realmLabel?rev=1", headersGroup, body))
        .mapJson { (json, result) =>
          result.status shouldEqual StatusCodes.OK
          json.removeFields("_createdAt", "_createdBy", "_updatedAt", "_updatedBy") shouldEqual jsonContentOf(
            "/iam/realms/ref-response.json",
            Map(
              quote("{realm}")      -> config.iam.testRealm,
              quote("{iamBase}")    -> config.iam.uri.toString,
              quote("{label}")      -> realmLabel,
              quote("{rev}")        -> "2",
              quote("{deprecated}") -> "false"
            )
          )
        }
    }

    "fetch updated realm" in {
      cl(Req(GET, s"$iamBase/realms/$realmLabel", headersGroup))
        .mapJson { (json, result) =>
          result.status shouldEqual StatusCodes.OK
          json.removeFields("_createdAt", "_createdBy", "_updatedAt", "_updatedBy") shouldEqual jsonContentOf(
            "/iam/realms/fetch-updated-response.json",
            Map(
              quote("{realm}")   -> config.iam.testRealm,
              quote("{iamBase}") -> config.iam.uri.toString,
              quote("{label}")   -> realmLabel
            )
          )
        }

    }

    "deprecate realm" in {
      cl(Req(DELETE, s"$iamBase/realms/$realmLabel?rev=2", headersGroup))
        .mapJson { (json, result) =>
          result.status shouldEqual StatusCodes.OK
          json.removeFields("_createdAt", "_createdBy", "_updatedAt", "_updatedBy") shouldEqual jsonContentOf(
            "/iam/realms/ref-response.json",
            Map(
              quote("{realm}")      -> config.iam.testRealm,
              quote("{iamBase}")    -> config.iam.uri.toString,
              quote("{label}")      -> realmLabel,
              quote("{rev}")        -> "3",
              quote("{deprecated}") -> "true"
            )
          )
        }
    }
    "fetch deprecated realm" in {
      cl(Req(GET, s"$iamBase/realms/$realmLabel", headersGroup))
        .mapJson { (json, result) =>
          result.status shouldEqual StatusCodes.OK
          json.removeFields("_createdAt", "_createdBy", "_updatedAt", "_updatedBy") shouldEqual jsonContentOf(
            "/iam/realms/fetch-deprecated-response.json",
            Map(
              quote("{realm}")   -> config.iam.testRealm,
              quote("{iamBase}") -> config.iam.uri.toString,
              quote("{label}")   -> realmLabel
            )
          )
        }
    }
  }
  "manage permissions" should {

    val permission1 = s"${genString(8)}/${genString(8)}"
    val permission2 = s"${genString(8)}/${genString(8)}"
    val minimumPermissions = Set(
      "acls/read",
      "acls/write",
      "events/read",
      "files/write",
      "organizations/create",
      "organizations/read",
      "organizations/write",
      "permissions/read",
      "permissions/write",
      "projects/create",
      "projects/read",
      "projects/write",
      "realms/read",
      "realms/write",
      "resolvers/write",
      "resources/read",
      "resources/write",
      "schemas/write",
      "views/query",
      "views/write",
      "storages/write"
    )

    "clear permissions" in {
      cl(Req(GET, s"$iamBase/permissions", headersGroup)).mapDecoded[Permissions] { (permissions, result) =>
        result.status shouldEqual StatusCodes.OK
        if (permissions.permissions == minimumPermissions)
          succeed
        else
          cl(Req(DELETE, s"$iamBase/permissions?rev=${permissions._rev}", headersGroup))
            .mapResp(_.status shouldEqual StatusCodes.OK)
      }
    }
    "add permissions" in {
      cl(Req(GET, s"$iamBase/permissions", headersGroup)).mapDecoded[Permissions] { (permissions, result) =>
        result.status shouldEqual StatusCodes.OK
        val body = jsonContentOf("/iam/permissions/append.json",
                                 Map(
                                   quote("{perms}") -> List(permission1, permission2).mkString("\",\"")
                                 )).toEntity
        cl(Req(PATCH, s"$iamBase/permissions?rev=${permissions._rev}", headersGroup, body))
          .mapResp(_.status shouldEqual StatusCodes.OK)
      }
    }

    "check added permissions" in {
      cl(Req(GET, s"$iamBase/permissions", headersGroup)).mapDecoded[Permissions] { (permissions, result) =>
        result.status shouldEqual StatusCodes.OK
        permissions.permissions shouldEqual minimumPermissions + permission1 + permission2
      }
    }

    "subtract permissions" in {
      cl(Req(GET, s"$iamBase/permissions", headersGroup)).mapDecoded[Permissions] { (permissions, result) =>
        result.status shouldEqual StatusCodes.OK
        val body = jsonContentOf("/iam/permissions/subtract.json",
                                 Map(
                                   quote("{perms}") -> permission2
                                 )).toEntity
        cl(Req(PATCH, s"$iamBase/permissions?rev=${permissions._rev}", headersGroup, body))
          .mapResp(_.status shouldEqual StatusCodes.OK)
      }

    }
    "check subtracted permissions" in {
      cl(Req(GET, s"$iamBase/permissions", headersGroup)).mapDecoded[Permissions] { (permissions, result) =>
        result.status shouldEqual StatusCodes.OK
        permissions.permissions shouldEqual minimumPermissions + permission1
      }
    }

    "replace permissions" in {
      cl(Req(GET, s"$iamBase/permissions", headersGroup)).mapDecoded[Permissions] { (permissions, result) =>
        result.status shouldEqual StatusCodes.OK
        val body =
          jsonContentOf("/iam/permissions/replace.json",
                        Map(
                          quote("{perms}") -> (minimumPermissions + permission1 + permission2).mkString("\",\"")
                        )).toEntity
        cl(Req(PUT, s"$iamBase/permissions?rev=${permissions._rev}", headersGroup, body))
          .mapResp(_.status shouldEqual StatusCodes.OK)
      }
    }

    "check replaced permissions" in {
      cl(Req(GET, s"$iamBase/permissions", headersGroup)).mapDecoded[Permissions] { (permissions, result) =>
        result.status shouldEqual StatusCodes.OK
        permissions.permissions shouldEqual minimumPermissions + permission1 + permission2
      }
    }

    "reject subtracting minimal permission" in {
      cl(Req(GET, s"$iamBase/permissions", headersGroup)).mapDecoded[Permissions] { (permissions, result) =>
        result.status shouldEqual StatusCodes.OK
        val body = jsonContentOf("/iam/permissions/subtract.json",
                                 Map(
                                   quote("{perms}") -> minimumPermissions.head
                                 )).toEntity
        cl(Req(PATCH, s"$iamBase/permissions?rev=${permissions._rev}", headersGroup, body))
          .mapResp(_.status shouldEqual StatusCodes.BadRequest)
      }
    }

    "reject replacing minimal permission" in {
      cl(Req(GET, s"$iamBase/permissions", headersGroup)).mapDecoded[Permissions] { (permissions, result) =>
        result.status shouldEqual StatusCodes.OK
        val body = jsonContentOf("/iam/permissions/replace.json",
                                 Map(
                                   quote("{perms}") -> minimumPermissions.subsets(1).next().mkString("\",\"")
                                 )).toEntity
        cl(Req(PUT, s"$iamBase/permissions?rev=${permissions._rev}", headersGroup, body))
          .mapResp(_.status shouldEqual StatusCodes.BadRequest)
      }
    }
  }
  "manage acls" should {

    val orgPath1     = genString()
    val orgPath2     = genString()
    val projectPath1 = genString()
    val projectPath2 = genString()

    "add permissions for user on /" in {
      val json =
        jsonContentOf("/iam/add.json",
                      replSub + (quote("{perms}") -> """projects/create","projects/read","projects/write""")).toEntity
      cl(Req(GET, s"$iamBase/acls/", headersGroup)).mapDecoded[AclListing] { (acls, result) =>
        result.status shouldEqual StatusCodes.OK
        val rev = acls._results.head._rev

        cl(Req(PATCH, s"$iamBase/acls/?rev=$rev", headersGroup, json)).mapResp(_.status shouldEqual StatusCodes.OK)
      }
    }

    "fetch permissions for user" in {
      cl(Req(GET, s"$iamBase/acls/?self=false", headersGroup)).mapDecoded[AclListing] { (acls, result) =>
        result.status shouldEqual StatusCodes.OK
        acls._results.head.acl
          .find {
            case AclEntry(User(_, config.iam.userSub), _) => true
            case _                                        => false
          }
          .value
          .permissions shouldEqual Set("projects/create", "projects/read", "projects/write")
      }
    }

    "delete some permissions for user" in {
      cl(Req(GET, s"$iamBase/acls/", headersGroup)).mapJson { (js, result) =>
        result.status shouldEqual StatusCodes.OK
        val acls = js
          .as[AclListing]
          .right
          .getOrElse(throw new RuntimeException(s"Couldn't decode ${js.noSpaces} to AclListing"))

        val rev = acls._results.head._rev
        val entity =
          jsonContentOf("/iam/subtract-permissions.json", replSub + (quote("{perms}") -> "projects/write")).toEntity
        cl(Req(PATCH, s"$iamBase/acls/?rev=$rev", headersGroup, entity)).mapResp { result =>
          result.status shouldEqual StatusCodes.OK
        }
      }
    }

    "check if permissions were removed" in {
      cl(Req(GET, s"$iamBase/acls/?self=false", headersGroup)).mapDecoded[AclListing] { (acls, result) =>
        result.status shouldEqual StatusCodes.OK
        acls._results.head.acl
          .find {
            case AclEntry(User(_, config.iam.userSub), _) => true
            case _                                        => false
          }
          .value
          .permissions shouldEqual Set("projects/create", "projects/read")
      }
    }

    "add permissions for user on paths with depth1" in {
      val json =
        jsonContentOf("/iam/add.json",
                      replSub + (quote("{perms}") -> """projects/create","projects/read","projects/write""")).toEntity

      cl(Req(PUT, s"$iamBase/acls/$orgPath1", headersGroup, json)).mapResp { result =>
        result.status shouldEqual StatusCodes.Created
      }
      cl(Req(PUT, s"$iamBase/acls/$orgPath2", headersGroup, json)).mapResp { result =>
        result.status shouldEqual StatusCodes.Created
      }
    }

    "add permissions for user on /orgpath/projectpath1 and /orgpath/projectpath2" in {
      val body =
        jsonContentOf("/iam/add.json",
                      replSub + (quote("{perms}") -> """projects/create","projects/read","projects/write""")).toEntity
      cl(Req(PATCH, s"$iamBase/acls/$orgPath1/$projectPath1", headersGroup, body))
        .mapResp(_.status shouldEqual StatusCodes.Created)
      cl(Req(PATCH, s"$iamBase/acls/$orgPath1/$projectPath2", headersGroup, body))
        .mapResp(_.status shouldEqual StatusCodes.Created)
      cl(Req(PATCH, s"$iamBase/acls/$orgPath2/$projectPath1", headersGroup, body))
        .mapResp(_.status shouldEqual StatusCodes.Created)
      cl(Req(PATCH, s"$iamBase/acls/$orgPath2/$projectPath2", headersGroup, body))
        .mapResp(_.status shouldEqual StatusCodes.Created)
    }

    "list permissions on /*/*" in {
      cl(Req(GET, s"$iamBase/acls/*/*", headerUser)).mapDecoded[AclListing] { (acls, result) =>
        result.status shouldEqual StatusCodes.OK
        acls._total should be >= 4L
        val expectedPermissions = Set("projects/create", "projects/read", "projects/write")
        acls._results
          .find(_._path == s"/$orgPath1/$projectPath1")
          .value
          .acl
          .head
          .permissions shouldEqual expectedPermissions
        acls._results
          .find(_._path == s"/$orgPath1/$projectPath2")
          .value
          .acl
          .head
          .permissions shouldEqual expectedPermissions
        acls._results
          .find(_._path == s"/$orgPath2/$projectPath1")
          .value
          .acl
          .head
          .permissions shouldEqual expectedPermissions
        acls._results
          .find(_._path == s"/$orgPath1/$projectPath2")
          .value
          .acl
          .head
          .permissions shouldEqual expectedPermissions
      }
    }

    "list permissions on /orgpath1/*" in {
      cl(Req(GET, s"$iamBase/acls/$orgPath1/*", headerUser)).mapDecoded[AclListing] { (acls, result) =>
        result.status shouldEqual StatusCodes.OK
        acls._total shouldEqual 2
        val expectedPermissions = Set("projects/create", "projects/read", "projects/write")
        acls._results
          .find(_._path == s"/$orgPath1/$projectPath1")
          .value
          .acl
          .head
          .permissions shouldEqual expectedPermissions
        acls._results
          .find(_._path == s"/$orgPath1/$projectPath2")
          .value
          .acl
          .head
          .permissions shouldEqual expectedPermissions

      }
    }

    "list permissions on /*/* with ancestors" in {
      cl(Req(GET, s"$iamBase/acls/*/*?ancestors=true", headerUser)).mapDecoded[AclListing] { (acls, result) =>
        result.status shouldEqual StatusCodes.OK
        val expectedPermissions = Set("projects/create", "projects/read", "projects/write")
        acls._results
          .find(_._path == "/")
          .value
          .acl
          .find {
            case AclEntry(User("nexusdev", config.iam.userSub), _) => true
            case _                                                 => false
          }
          .value
          .permissions shouldEqual Set("projects/create", "projects/read")
        acls._results
          .find(_._path == s"/$orgPath1")
          .value
          .acl
          .head
          .permissions shouldEqual expectedPermissions
        acls._results
          .find(_._path == s"/$orgPath2")
          .value
          .acl
          .head
          .permissions shouldEqual expectedPermissions
        acls._results
          .find(_._path == s"/$orgPath1/$projectPath1")
          .value
          .acl
          .head
          .permissions shouldEqual expectedPermissions
        acls._results
          .find(_._path == s"/$orgPath1/$projectPath2")
          .value
          .acl
          .head
          .permissions shouldEqual expectedPermissions
        acls._results
          .find(_._path == s"/$orgPath2/$projectPath1")
          .value
          .acl
          .head
          .permissions shouldEqual expectedPermissions
        acls._results
          .find(_._path == s"/$orgPath1/$projectPath2")
          .value
          .acl
          .head
          .permissions shouldEqual expectedPermissions
      }
    }
  }

}
