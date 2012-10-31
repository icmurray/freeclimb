package freeclimb.restApi

import akka.actor.{Props, ActorSystem}
import spray.can.server.HttpServer
import spray.io._


object Main extends App {

  

  // Setup database connection pool.
  // NOTE: in the future, the pool will probably not be handled
  // by freeclimb, rather it will be handled by a piece of middleware.
  private val connectionSource = freeclimb.sql.createConnectionPool(
    "127.0.0.1",
    "freeclimb",
    "freeclimb",
    "password"
  )

  freeclimb.sql.performMigrations(connectionSource)

  // we need an ActorSystem to host our application in
  val system = ActorSystem("rest-api")

  // every spray-can HttpServer (and HttpClient) needs an IOBridge for low-level network IO
  // (but several servers and/or clients can share one)
  val ioBridge = new IOBridge(system).start()

  // create and start our service actor
  val service = system.actorOf(Props(new ServiceActor(connectionSource)), "request-handler")

  // create and start the spray-can HttpServer, telling it that
  // we want requests to be handled by our singleton service actor
  val httpServer = system.actorOf(
    Props(new HttpServer(ioBridge, SingletonHandler(service))),
    name = "http-server"
  )

  // a running HttpServer can be bound, unbound and rebound
  // initially to need to tell it where to bind to
  httpServer ! HttpServer.Bind("localhost", 8080)

  // finally we drop the main thread but hook the shutdown of
  // our IOBridge into the shutdown of the applications ActorSystem
  system.registerOnTermination {
    ioBridge.stop()
  }
}