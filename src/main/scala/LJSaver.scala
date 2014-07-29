import java.io.File
import java.net.{SocketTimeoutException, URL}

import akka.actor.{ActorSystem, Props}
import org.jsoup.{HttpStatusException, Jsoup}

import scala.collection.JavaConversions._
import scala.collection.mutable.ArrayBuffer


object LJSaver {

  def main (args: Array[String]) {
    val logger = Logger(name="main thread")
    val name = args(0) //имя блоггера
    val lj = f"http://$name.livejournal.com/"

    //проверка имени и доступности хоста
    try {
      Jsoup.connect(lj).userAgent("Mozilla/5.0").timeout(20000)
    } catch {
      case ex: HttpStatusException =>
        logger.error(f"Error: No such LiveJournal user: $name")
        System.exit(-1)
      case ex: SocketTimeoutException =>
        logger.error(f"Can't connect to $lj: connection timed out")
        System.exit(-1)
    }

    try {
      //сколько всего записей
      val nrOfPages = Jsoup.connect(f"$lj/profile").
      timeout(20000).userAgent("Mozilla/5.0").get.
      select("div.b-profile-stat-value").first.text.toInt
      logger.info(f"Downloading $nrOfPages pages from $lj")
      //составляем список номеров страниц вида [231232, 164321, 782131, ...]
      val pageNums: ArrayBuffer[Int] = {
        //страницы разделены по 10
        val temp = ArrayBuffer[Int]()
        for (p <- 0 to nrOfPages by 10) {
          val links = Jsoup.connect(f"$lj/?skip=$p").
          timeout(20000).userAgent("Mozilla/5.0").get.
          getElementsByClass("subj-link")
          for (link <- links) {
            val pageNum =
            new URL(link.attr("href")).getFile.drop(1).dropRight(5)
            temp += pageNum.toInt
          }
        }
        temp
      }
      //создаем download directory
      new File(name).mkdirs
      logger.info(f"Created directory: $name")
      //начинаем работу
      val system = ActorSystem("ljsaver")
      val manager = system.actorOf(Props(new Manager(pageNums, name)), name="manager")
      manager ! Start()

    } catch {
      case e: HttpStatusException =>
        logger.error(
        "HTTP" + e.getStatusCode + "Error" +
        "\twhile retrieving page:\n\t" +
        e.getUrl
      )
        System.exit(-1)
      case e: SocketTimeoutException =>
        logger.error(
        "Error: connection timed out while retrieving page\n" +
        e.getMessage
      )
        System.exit(-1)

      case e: Exception =>
        logger.error("Unhandled exception")
        e.printStackTrace()
        System.exit(-1)
    }
  }
}
