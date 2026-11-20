package io.github.ondeoma.scalactive.effects

import io.github.ondeoma.scalactive.reactive.{CRV, RV}

import scala.concurrent.Future
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue
import scala.util.{Failure, Success, Try}

/** 例外の表示用の文言 */
private[effects] object ErrorText {

  def of(e: Throwable): String = Option(e.getMessage).filter(_.nonEmpty).getOrElse(e.toString)

}

/**
 * 非同期に読み込む値の状態([[Effects.useAsync]] で作ります).
 *
 *  - data: 読み込んだ値(読み込む前はNone. 読み直し中も前の値のまま)
 *  - loading: 読み込み中ならtrue
 *  - error: 最後の読み込みが失敗した場合の例外
 */
final class AsyncState[A] private[effects](loader: () => Future[A]) extends EffectHandle(() => ()) {

  val data: RV[Option[A]] = new RV(None)
  val loading: RV[Boolean] = new RV(false)
  val error: RV[Option[Throwable]] = new RV(None)

  /** 最後の読み込みが失敗していればtrue */
  val failed: CRV[Boolean] = error.mapC(_.isDefined)

  /** 最後の読み込みの例外の文言(失敗していなければ空文字) */
  val errorMessage: CRV[String] = error.mapC(_.fold("")(ErrorText.of))

  /** 値を読み込み済みならtrue */
  val ready: CRV[Boolean] = data.mapC(_.isDefined)

  private var seq = 0

  /** 読み込んだ値(読み込む前はdefault) */
  def getOrElse(default: => A): A = data.v.getOrElse(default)

  /** 読み直します. 後から別の読み直しを始めた場合、このFutureは古い応答のまま完了しますが、状態には反映しません. */
  def reload(): Future[A] = {
    seq += 1
    val my = seq
    loading := true
    val f = Try(loader()) match {
      case Success(x) => x
      case Failure(e) => Future.failed(e)
    }
    f.onComplete { r =>
      if (my == seq && !isStopped) {
        r match {
          case Success(a) =>
            data := Some(a)
            error := None
          case Failure(e) =>
            error := Some(e)
        }
        loading := false
      }
    }
    f
  }

  /** 値を直接書き換えます(保存した結果を反映するときなど). 実行中の読み込みの結果は捨てます. */
  def set(a: A): Unit = {
    seq += 1
    data := Some(a)
    error := None
    loading := false
  }

  /** 読み込んだ値を書き換えます(読み込む前は何もしません) */
  def update(f: A => A): Unit = data.v.foreach(a => set(f(a)))

}

/**
 * 実行中は二重に実行できない操作の状態([[Effects.useAction]] で作ります).
 *
 *  - busy: 実行中ならtrue(ボタンの :disabled に使います)
 *  - error: 最後の実行が失敗した場合の例外
 */
final class ActionState private[effects]() extends EffectHandle(() => ()) {

  val busy: RV[Boolean] = new RV(false)
  val error: RV[Option[Throwable]] = new RV(None)

  /** 最後の実行が失敗していればtrue */
  val failed: CRV[Boolean] = error.mapC(_.isDefined)

  /** 最後の実行の例外の文言(失敗していなければ空文字) */
  val errorMessage: CRV[String] = error.mapC(_.fold("")(ErrorText.of))

  /**
   * fを実行します. 実行中に呼ばれた場合は、fを実行せずに [[ActionState.Busy]] で失敗したFutureを返します.
   */
  def run[A](f: => Future[A]): Future[A] = {
    if (busy.v) Future.failed(ActionState.Busy)
    else {
      busy := true
      error := None
      val fut = Try(f) match {
        case Success(x) => x
        case Failure(e) => Future.failed(e)
      }
      fut.onComplete { r =>
        r match {
          case Failure(e) => error := Some(e)
          case Success(_) => ()
        }
        busy := false
      }
      fut
    }
  }

  /** エラーの表示を消します */
  def clearError(): Unit = error := None

}

object ActionState {

  /** 実行中に、もう一度実行しようとした */
  case object Busy extends RuntimeException("the action is already running")

}
