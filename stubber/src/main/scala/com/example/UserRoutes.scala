package com.example

import akka.actor.typed.{ActorRef, ActorSystem}
import akka.actor.typed.scaladsl.AskPattern._
import akka.http.scaladsl.model.{MediaTypes, StatusCodes}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.{Route, StandardRoute}
import akka.util.Timeout
import com.example.UserRegistry._
import com.sbrf.util.json.{HandlerExample, JsonSupport}
import com.sbrf.util.xml.{Copier, XMLSupport}
import spray.json.JsValue

import scala.concurrent.Future
import scala.xml.NodeSeq

//#import-json-formats
//#user-routes-class
class UserRoutes(userRegistry: ActorRef[UserRegistry.Command], xmlSupport: ActorRef[XMLSupport.Command], jsonSupport: ActorRef[JsonSupport.Command])(implicit val system: ActorSystem[_]) {

  //#user-routes-class
  import JsonFormats._
  import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
  import akka.http.scaladsl.marshallers.xml.ScalaXmlSupport._
  //#import-json-formats

  // If ask takes more time than this to complete the request is failed
  private implicit val timeout: Timeout = Timeout.create(system.settings.config.getDuration("my-app.routes.ask-timeout"))

  def getUsers(): Future[Users] =
    userRegistry.ask(GetUsers)
  def getUser(name: String): Future[GetUserResponse] =
    userRegistry.ask(GetUser(name, _))
  def createUser(user: User): Future[ActionPerformed] =
    userRegistry.ask(CreateUser(user, _))
  def deleteUser(name: String): Future[ActionPerformed] =
    userRegistry.ask(DeleteUser(name, _))

  //#all-routes
  //#users-get-post
  //#users-get-delete
  val userRoutes: Route =
    extractRequest { rq =>
      rq.entity.contentType.mediaType match {
        case MediaTypes.`text/xml` | MediaTypes.`application/xml` =>
          entity(as[NodeSeq]) { data =>
            val future: Future[XMLSupport.Response] = xmlSupport ? (XMLSupport.Transform(data, rq.uri.path, _))
            onSuccess(future) {
              case XMLSupport.Success(nodes) => complete(nodes)
              case XMLSupport.Failure(error) => notFound(error.getMessage)
            }
          }
        case MediaTypes.`application/json` =>
          entity(as[JsValue]) { data =>
            val future: Future[JsonSupport.Response] = jsonSupport ? (JsonSupport.Transform(data, rq.uri.path, _))
            onSuccess(future) {
              case JsonSupport.Success(nodes) => complete(StatusCodes.OK, nodes)
              case JsonSupport.Failure(error) => notFound(error.getMessage)
            }
          }
        case _ => pathPrefix("users") {
          concat(
            //#users-get-delete
            pathEnd {
              concat(
                get {
                  val eventualUsers: Future[Users] = getUsers()
                  complete(eventualUsers)
                },
                post {
                  entity(as[User]) { user =>
                    onSuccess(createUser(user)) { performed =>
                      complete((StatusCodes.Created, performed))
                    }
                  }
                })
            },
            //#users-get-delete
            //#users-get-post
            path(Segment) { name =>
              concat(
                get {
                  //#retrieve-user-info
                  rejectEmptyResponse {
                    onSuccess(getUser(name)) { response =>
                      complete(response.maybeUser)
                    }
                  }
                  //#retrieve-user-info
                },
                delete {
                  //#users-delete-logic
                  onSuccess(deleteUser(name)) { performed =>
                    complete((StatusCodes.OK, performed))
                  }
                  //#users-delete-logic
                })
            })
          //#users-get-delete
        }
      }
    }
  //#all-routes
  private def notFound(message: String): StandardRoute = {
    complete(Future.successful((StatusCodes.NotFound, message)))
  }
  XMLSupport.register(Copier)
  JsonSupport.register(HandlerExample)
}
