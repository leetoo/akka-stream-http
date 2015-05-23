
import akka.actor.ActorSystem
import akka.stream.ActorFlowMaterializer
import akka.stream.scaladsl.Source
import org.scalatest._





class TwitterTest
  extends FeatureSpec with GivenWhenThen with Matchers
  with TweetsFixture
  with AkkaEngine {

  override def akkaName: String = "TwitterTest"

  info("I like to test akka stream examples")

  feature("simple sources handling") {

    scenario("Simple authors extraction") {

      val authors: Source[Author, Unit] =
        tweets
          .filter(_.hashtags.contains(akka))
          .map(_.author)

      Given("Having an authors flow producing only authors with #akka")
      var retAuthors: List[Author] = Nil

      authors.runForeach(a => {
        retAuthors = a :: retAuthors
      })

      waitABit()

      Then("authors should be valid size")
      retAuthors.size should be(2)
    }

    scenario("Simple hashtags extraction") {
      Given("Having an hashtags flow producing all hashtags with mapConcat (flattening them)")


      val hashtags = tweets.mapConcat(_.hashtags.toList)

      var retHashtags: List[Hashtag] = Nil
      hashtags.runForeach(h =>
        retHashtags = h :: retHashtags
      )
      waitABit()
      Then("hashtags should be valid count")
      retHashtags.size should be(6)

    }
  }


}