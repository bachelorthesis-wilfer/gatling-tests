package bookApi

import io.gatling.core.Predef._
import io.gatling.http.Predef._

import scala.concurrent.duration._
import scala.language.postfixOps

// name of this class is name of target simulation
class TestSimulation extends Simulation {

  val httpProtocol = http
    .baseUrl("http://localhost:8080") // Add URL from your server
    .acceptHeader("application/json")
    .userAgentHeader("Gatling")

  val csvFeeder = csv("src/test/resources/bookFiles/book_input.csv").shuffle

  val postAndPutScenario = scenario("15 Min Spring Blocking JAR POST then GET then PUT")
    .feed(csvFeeder.circular())
    .exec(http("POST request")
      .post("/books")
      .header("Content-type", "application/json")
      .body(StringBody("""{
                   "author" : "${author}",
                   "title" : "${title}",
                   "price" : ${price},
                   "isbn" : ${isbn}
                 }"""))
      .check(jsonPath("$.id").saveAs("bookId"))
  )
    .exec(http("GET newly posted Book")
      .get("/books/${bookId}")
      .check(jsonPath("$.id").saveAs("bookId"))
      .check(jsonPath("$.title").saveAs("bookTitle"))
      .check(jsonPath("$.author").saveAs("bookAuthor"))
      .check(jsonPath("$.price").saveAs("bookPrice"))
      .check(jsonPath("$.isbn").saveAs("bookIsbn")))
    .exec(session => {
      val bookTitle = session("bookTitle").as[String]
      val modifiedBookTitle = bookTitle.concat(" PART 2!!!")
      session.set("modifiedBookTitle", modifiedBookTitle)
    })
    .exec(http("PUT stuff to the title of the new book")
      .put("/books/${bookId}")
      .body(StringBody(session => {
        s"""
           {
             "id": ${session("bookId").as[String]},
             "title": "${session("modifiedBookTitle").as[String]}",
             "author": "${session("bookAuthor").as[String]}",
             "price": ${session("bookPrice").as[String]},
             "isbn": ${session("bookIsbn").as[String]}
           }
         """.stripMargin
      })).asJson
    )

  setUp(
    postAndPutScenario.inject(
      // use whatever scenario you want to test
     // constantUsersPerSec(500).during(45.minutes))
      constantUsersPerSec(500).during(10.minutes))
  ).protocols(httpProtocol)

}
