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

  private val twList: List[Tweet] = List(
    Tweet(Author("pippo"), 1L, "#twitter on #scala #twitter")
    , Tweet(Author("pippo"), 1L, "#scala is #fun and #akka too")
    , Tweet(Author("pluto"), 2L, "its #akka time")
    , Tweet(Author("minnie"), 3L, "#daisies")
  )
  // a list of tweets to analyze
  val tweets: Source[Tweet, Unit] = Source(twList)

  def numAuthors = 4
  def numAuthorsWithAkka = 2
  def numHashTags = 7
  def numTweets = twList.size

}
