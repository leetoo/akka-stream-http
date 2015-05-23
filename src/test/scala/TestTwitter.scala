
import akka.actor.ActorSystem
import akka.stream.ActorFlowMaterializer
import akka.stream.scaladsl.Source
import org.scalatest._

import scala.collection.immutable.Range.Inclusive


final case class Author(handle: String)

final case class Hashtag(name: String)

final case class Tweet(author: Author, timestamp: Long, body: String) {
  def hashtags: Set[Hashtag] =
    body.split(" ").collect { case t if t.startsWith("#") => Hashtag(t) }.toSet
}


trait AkkaEngine {

  implicit val system = ActorSystem("reactive-twwets")
  implicit val materializer = ActorFlowMaterializer()

}

class TwitterTest extends FeatureSpec with GivenWhenThen with Matchers with AkkaEngine {

  val akka = Hashtag("#akka")

  info("I like to test akka stream examples")

  feature("simple sources handling") {

    scenario("Create from tweets and playing around") {

      Given("setting up")

      var retAuthors:List[Author] = Nil
      var retHashtags:List[Hashtag] = Nil
      val tweets: Source[Tweet, Unit] = Source(List(
        Tweet(Author("pippo"), 1L, "#twitter on #scala #twitter")
        , Tweet(Author("pippo"), 1L, "#scala is #fun and #akka too")
        , Tweet(Author("pluto"), 2L, "its #akka time")
      ))
      val authors: Source[Author, Unit] =
        tweets
          .filter(_.hashtags.contains(akka))
          .map(_.author)



      authors.runForeach(a => { retAuthors = a :: retAuthors})
      val hashtags = tweets.mapConcat(_.hashtags.toList)
      hashtags.runForeach(h=>retHashtags = h :: retHashtags)

      Thread.sleep(1000)
      Then("authors should be valid size")
      retAuthors.size should be (2)
      Then("hashtags should be valid count")
      retHashtags.size should be (6)

    }
  }

}