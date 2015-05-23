import akka.stream.scaladsl.Source

/**
 * Created by Francesco on 23/05/2015.
 */
trait TweetsFixture {

  final case class Author(handle: String)

  final case class Hashtag(name: String)

  final case class Tweet(author: Author, timestamp: Long, body: String) {
    def hashtags: Set[Hashtag] =
      body.split(" ").collect { case t if t.startsWith("#") => Hashtag(t) }.toSet
  }

  val akka = Hashtag("#akka")

  // a list of tweets to analyze
  val tweets: Source[Tweet, Unit] = Source(List(
    Tweet(Author("pippo"), 1L, "#twitter on #scala #twitter")
    , Tweet(Author("pippo"), 1L, "#scala is #fun and #akka too")
    , Tweet(Author("pluto"), 2L, "its #akka time")
  ))

}
