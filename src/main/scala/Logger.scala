class Logger (val name: String) {

  def error (m: String) {
    println("[" + name + "]" + "ERROR: " + m)
  }

  def info (m: String) {
    println("[" + name + "]" + "INFO: " + m)
  }
}

object Logger {
  def apply(name: String) = new Logger(name)
}
