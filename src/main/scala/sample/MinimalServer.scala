package sample

import akka.Done
import akka.actor.typed.scaladsl.AskPattern.Askable
import akka.actor.typed.{ActorRef, ActorSystem, Behavior}
import akka.actor.typed.scaladsl.Behaviors
import akka.http.scaladsl.Http
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.headers.EntityTag
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.util.Timeout
import sample.SimpleItemCache.{AddItem, GetItem, GetItems}
import spray.json.DefaultJsonProtocol

import java.util.UUID
import scala.concurrent.Future
import scala.concurrent.duration.DurationInt

// domain

final case class Item(id: String, name: String)

final case class Items(items: List[Item])

// api request

final case class NewItem(name: String)


// small typed actor holding items
object SimpleItemCache {

  sealed trait CacheMessages

  final case class AddItem(item: Item, replyTo: ActorRef[Done]) extends CacheMessages

  final case class GetItem(id: String, replyTo: ActorRef[Option[Item]]) extends CacheMessages

  final case class GetItems(replyTo: ActorRef[Items]) extends CacheMessages

  def apply(): Behavior[CacheMessages] =
    Behaviors.setup { _ =>

      def handleWithCache(items: List[Item]): Behavior[CacheMessages] =
        Behaviors.receiveMessage {
          case AddItem(item, replyTo) =>
            replyTo ! Done
            handleWithCache(items :+ item)
          case GetItem(itemId, replyTo) =>
            replyTo ! items.find(_.id == itemId)
            Behaviors.same
          case GetItems(replyTo) =>
            replyTo ! Items(items)
            Behaviors.same
        }

      handleWithCache(List.empty)
    }
}

// json formats for endpoint
object JsonSupport extends SprayJsonSupport with DefaultJsonProtocol {
  implicit val newItemFormat = jsonFormat1(NewItem)
  implicit val itemFormat = jsonFormat2(Item)
  implicit val itemsFormat = jsonFormat1(Items) // contains List[Item]
}

// just a sample server without much error handling and synchronisation for shutdown
class MinimalServer {

  private var bindingFuture: Option[Future[Http.ServerBinding]] = None

  implicit val system: ActorSystem[SimpleItemCache.CacheMessages] = ActorSystem(SimpleItemCache(), "my-system")
  implicit val executionContext = system.executionContext

  def run(port: Int = 8080): Future[Done] = {
    import JsonSupport._

    implicit val timeout: Timeout = 5.seconds
    implicit val scheduler = system.scheduler
    val cache: ActorRef[SimpleItemCache.CacheMessages] = system

    val route: Route =
      pathPrefix("test") {
        pathPrefix("items") {
          pathEnd {
            get {
              onSuccess(cache.ask(replyTo => GetItems(replyTo))) {
                case items => complete(items)
              }
            } ~
              post {
                entity(as[NewItem]) { newItem =>
                  val id = UUID.randomUUID().toString
                  val item = Item(id = id, name = newItem.name)
                  onSuccess(cache.ask(replyTo => AddItem(item, replyTo))) {
                    case _ => complete(StatusCodes.Created, item)
                  }
                }
              }
          } ~ path(Segment) { itemId =>
            onSuccess(cache.ask(replyTo => GetItem(itemId, replyTo))) {
              case Some(item) =>
                // Note we should create a better Etag here - e.g. use modification time - just for illustration in the test
                conditional(eTag = Some(EntityTag(item.hashCode().toString, true)), None) {
                  complete(StatusCodes.OK, item)
                }
              case None => complete(StatusCodes.NotFound)
            }
          }
        } ~
          pathPrefix("redirect") {
            path("items") {
              redirect("/test/items", StatusCodes.PermanentRedirect)
            }
          }
      }

    val binding = Http().newServerAt("localhost", port).bind(route)
    bindingFuture = Some(binding)

    println(s">>>>>>>>>>>> Server online at http://localhost:$port/")
    binding.map(_ => Done)
  }

  def shutdown: Unit = {
    bindingFuture.map { bf =>
      bf
        .flatMap(_.unbind()) // trigger unbinding from the port
        .map(_ => Done)
        .onComplete { _ =>
          println(">>>>>>>>>>>> shutDown server")
          system.terminate()
        } // and shutdown when done
    }.getOrElse(Future.successful(Done))
  }

}
