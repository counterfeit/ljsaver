import akka.actor.{Actor, ActorLogging, Props}
import akka.routing.RoundRobinRouter

import scala.collection.mutable.ArrayBuffer


class Manager (pageNums: ArrayBuffer[Int], name: String)
  extends Actor with ActorLogging {
  //роутер, нужен для распределения работы между акторами
  val router =
    context.actorOf( //количество акторов зависит от конфигурации
      Props[Worker].withRouter(
        RoundRobinRouter(Runtime.getRuntime.availableProcessors)),
      name="router"
    )

  def receive = {
    //сигнал к началу
    case Start() =>
      for (pageNum <- pageNums)
        router ! Work(pageNum, name)
    //получили результат
    case Result(code, url, filename) =>
      if (code == 200) //200 == HTTP Success
        log.info(f"HTTP 200 $filename")
      else
        log.error(f"HTTP $code $url")
  }
}

