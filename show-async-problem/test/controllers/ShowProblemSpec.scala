package controllers

import scala.concurrent.{Await, ExecutionContext}
import scala.concurrent.duration._
import scala.util._

import org.scalatest._
import org.scalatestplus.play._
import org.scalatestplus.play.guice._
import play.api.Application
import play.api.db.Databases
import play.api.inject.bind
import play.api.inject.guice._
import play.api.mvc._
import play.api.test._
import play.api.test.Helpers._
import play.core.server.Server

class ShowProblemSpec extends PlaySpec with GuiceOneAppPerSuite with Injecting with BeforeAndAfterAll {

  val database = Databases.inMemory(
    "default", 
    urlOptions = Map(
      "MODE" -> "PostgresQL"
    ), 
    config = Map(
    ))
  
  /**
   * This is where we define the Application for testing. In particular, it is where we define
   * test-specific config overrides, as well as stubbing out Guice modules for tests.
   */
  override def fakeApplication():Application =
    new GuiceApplicationBuilder()
      // Config settings go here:
      .configure(
        // By default, CSRF doesn't allow running directly from localhost:
        "play.filters.hosts.allowed" -> Seq("localhost"),
        "slick.dbs.default.profile" -> "slick.jdbc.H2Profile$",
        "slick.dbs.default.db.driver" -> "org.h2.Driver",
        "slick.dbs.default.db.url" -> "jdbc:h2:mem:play"
      )
      // Test-only bits go here:
      .bindings(bind[TestDatabase].to[TestDatabaseImpl])
      .build()
      
  def testdb = inject[TestDatabase]
  implicit def ec = inject[ExecutionContext]

  override def beforeAll() {
    Await.result(testdb.setupDB(), 5 seconds)
  }
  
  override def afterAll() {
    database.shutdown()
  }

  "My simple Slick code" should {
    "work if there is no Server" in {
      val rows = Await.result(testdb.getRows(), 5 seconds)
      rows must equal (0)
    }
    
    "fail is there is a Server" in {
      Server.withRouter() {
        case _ => stubControllerComponents().actionBuilder { Results.Ok("") }
      } {
        implicit port =>
          val rows = Await.result(testdb.getRows(), 5 seconds)
          rows must equal (0)
      }      
    }
  }
}
