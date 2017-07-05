package controllers

import scala.concurrent.{ExecutionContext, Future}

import com.google.inject._

import play.api.Application
import play.api.db.slick.DatabaseConfigProvider
import play.db.NamedDatabase
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}

import slick.jdbc.JdbcProfile

import slick.dbio.DBIOAction
import slick.jdbc.JdbcProfile

trait TestDatabase {
  def setupDB():Future[Unit]
  def getRows():Future[Int]
}

/**
 * This class builds the mock in-memory database that we will use for testing.
 */
class TestDatabaseImpl @Inject() 
  (protected val dbConfigProvider: DatabaseConfigProvider)
  (implicit ec:ExecutionContext)
  extends TestDatabase with HasDatabaseConfigProvider[JdbcProfile]
{
  import profile.api._
  
  class Users(tag:Tag) extends Table[(Long)](tag, "artimauser") {
    val id: Rep[Long] = column[Long]("id", O.PrimaryKey)

    def * = (id)
  }
  val users = TableQuery[Users]
  
  def setupDB():Future[Unit] = {
    db.run(users.schema.create)
  }
  
  def getRows():Future[Int] = {
    db.run(users.length.result)
  }
}
