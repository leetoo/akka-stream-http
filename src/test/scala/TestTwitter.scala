
import akka.actor.ActorSystem
import akka.stream.ActorFlowMaterializer
import akka.stream.scaladsl._
import org.scalatest._
import org.scalatest.concurrent.{ScalaFutures, Futures}


import scala.concurrent.ExecutionContext.Implicits.global

import scala.concurrent.Future


class TwitterTest
  extends FeatureSpec with GivenWhenThen with Matchers with BeforeAndAfter
  //with Futures
  with ScalaFutures
  with TweetsFixture // using some tweets and related values
  with AkkaEngine {

  var retHashtags: List[Hashtag]=List()
  var retAuthors: List[Author]=List()
  def writeAuthors(a:Author):Unit = retAuthors = a :: retAuthors
  def writeHashtags(h:Hashtag):Unit = retHashtags = h :: retHashtags

  def writeAuthorsSink  = Sink.foreach(writeAuthors)
  def writeHashtagsSink = Sink.foreach(writeHashtags)

  before {
    retAuthors=List()
    retHashtags=List()
  }

  override def akkaName: String = "TwitterTest"

  info("I like to test akka stream examples")

  feature("simple sources handling") {

    scenario("Simple authors extraction") {
      Given("Having an authors flow producing only authors with #akka")
      val authors: Source[Author, Unit] =
        tweets
          .filter(_.hashtags.contains(akka))
          .map(_.author)

      Then("running the flow")

      authors.runForeach(writeAuthors)

      waitABit()

      Then("authors should be a valid size")
      retAuthors.size should be(numAuthorsWithAkka)
    }

    scenario("Simple hashtags extraction") {
      Given("Having an hashtags flow producing all hashtags with mapConcat (flattening them)")
      val hashtags = tweets.mapConcat(_.hashtags.toList)

      Then("Running that flow")
      hashtags.runForeach(writeHashtags)
      waitABit()
      Then("hashtags should be valid count")
      retHashtags.size should be(numHashTags)

    }
    scenario("Using a broadcast component"){
      Given("A two outputs flow broadcaster")
      val g = FlowGraph.closed() {
        implicit b =>
          import FlowGraph.Implicits._
          val bcast = b.add(Broadcast[Tweet](2))
          tweets ~> bcast.in
          bcast.out(0) ~> Flow[Tweet].map(_.author) ~> writeAuthorsSink
          bcast.out(1) ~> Flow[Tweet].mapConcat(_.hashtags.toList) ~> writeHashtagsSink
      }
      g.run()
      waitABit()
      Then("verifies we have correct counts on both outputs")
      retAuthors.size should be(numAuthors)
      retHashtags.size should be(numHashTags)



    }
    scenario("using value materialized to count tweet"){
      Given("A sum sink able to produce a Int")
      val sumSink = Sink.fold[Int,Int](0)(_+_)

      Then("Materialize a counter able to add 1 for each tweet")
      /* this is the long way to connect a flow to a sink
      val counter: RunnableFlow[Future[Int]] =
        tweets.map(t=>1).toMat(sumSink)(Keep.right)
      val sum:Future[Int] = counter.run()
      */
      Then("Count all the tweets")

      /* and this is much easier */
      val sum=tweets.map(t=>1).runWith(sumSink)

      Then("verify that sum is really the number of tweets")
      whenReady(sum){
        c:Int => c should be (numTweets)
      }
    }


  }


}