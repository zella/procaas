import monix.eval.Task

import scala.concurrent.duration.Duration

trait ExecutorFor[T] {

  def executeBlocking(timeout: Duration): Task[T]

  def execute(timeout: Duration): Task[T]
}
