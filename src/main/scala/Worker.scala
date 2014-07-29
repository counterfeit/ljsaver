import java.io.{FileOutputStream, PrintWriter}
import java.net.SocketTimeoutException

import akka.actor.Actor
import org.jsoup.{HttpStatusException, Jsoup}

/*
*  типы сообщений:
*  Start  == начать работу
*  Work   == информация для загрузки
*  Result == результат - HTTP код, URL и название страницы*/
case class Start()
case class Work(pageNum: Int, name: String)
case class Result(code: Int, url: String, pageName: String)


//рабочий актор
class Worker extends Actor {
  private var tO: Int = 20000

  def receive = {
    case Work(pageNum, name) =>
      val url = f"http://$name.livejournal.com/$pageNum.html"
      try {
        val connection =
          Jsoup.connect(url).timeout(tO).userAgent("Mozilla/5.0")
        val page = connection.get
        val code = connection.response.statusCode
        //берем название страницы и удаляем лишние символы (windows)
        val filename = page.select("div.entry-title-content > h4").
          first.text.filter(!"""\/:*?"<>|""".contains(_)) + ".html"
        val writer =
          new PrintWriter(
            new FileOutputStream(f"$name/$filename")
          )
        //записываем в файл
        try {
          writer.write(page.html)
          sender() ! Result(code, url, filename)
        } finally {
          writer.close()
        }
      } catch {
        //страница не найдена или недоступна
        case e: HttpStatusException    =>
          sender() ! Result(e.getStatusCode, e.getUrl, "None")
        case e: SocketTimeoutException =>
          sender() ! Result(408, url, "None") //408 == HTTP Request TimeOut
          tO *= 2
      }
  }
}

