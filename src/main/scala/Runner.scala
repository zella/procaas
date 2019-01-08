import fastily.jwiki.core.Wiki

object Runner {
  def main(args: Array[String]): Unit = {
    print("Hello world")


    import fastily.jwiki.core.NS

   // val wiki= new Wiki("ru.wikipedia.org")
//    val p = wiki.getRandomPages(5, NS.MAIN)
//
//
//   val t = wiki.getPageText("GitHub")
//   println(p)


   // val txt = wiki.getTextExtract("Беннетт, Тони")
   // println("end")
    new HttpServer().startFuture()
  }
}
