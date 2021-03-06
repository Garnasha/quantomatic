package quanto.util

import java.io.File
import java.util.concurrent.TimeUnit
import java.util.{Calendar, Date}

import scala.swing.event.Event
import scala.swing.{Color, Dialog, Publisher}


// Universal system for alerting the user
// Messages appear in the bottom pane (Label and progress bar)
// SelfAlertingProcess is the easiest way to access this system
// Listen to events via AlertPublisher
object UserAlerts {

  var ongoingProcesses: List[UserStartedProcess] = List()
  var alerts: List[Alert] = List()

  def leastCompleteProcess: Option[UserStartedProcess] = {
    if (ongoingProcesses.isEmpty) None else {
      val indeterminate = ongoingProcesses.find(op => !op.determinate)
      if (indeterminate.nonEmpty) indeterminate else {
        Some(ongoingProcesses.minBy(op => op.value))
      }
    }
  }

  def latestMessage: Alert = {
    if (alerts.headOption.nonEmpty) alerts.head else {
      alert("Quantomatic starting up")
      latestMessage
    }
  }

  def alert(message: Any): Unit = alert(message.toString, Elevation.NOTICE)

  def debug(message: String): Unit = alert(message, Elevation.DEBUG)

  def errorBox(message: String): Unit = {
    alert(message, Elevation.ERROR)
    Dialog.showMessage(
      title = "Error",
      message = message,
      messageType = Dialog.Message.Error)
  }

  def alert(message: String, elevation: Elevation.Elevation): Unit = {
    val newAlert = Alert(Calendar.getInstance().getTime, elevation, message)
    println(newAlert.toString)
    alerts = newAlert :: alerts
    AlertPublisher.publish(UserAlertEvent(newAlert))
    writeToLogFile(newAlert)
  }

  def writeToLogFile(alert: Alert, force: Boolean = false): Unit = {
    val elevation = alert.elevationText match {
      case "" => ""
      case e => s"[$e]"
    }
    if (logFile.nonEmpty && (UserOptions.logging || force)) {
      FileHelper.printToFile(logFile.get)(
        p => p.println(UserOptions.preferredTimeFormat.format(alert.time) + ": " + elevation + alert.message)
      )

    }
  }

  def registerLogFile(optionFile: Option[File]) : Unit = {
    _logFile = optionFile
  }

  private var _logFile : Option[File] = None

  def logFile: Option[File] = _logFile

  case class UserAlertEvent(alert: Alert) extends Event

  case class UserProcessUpdate(ongoingProcess: UserStartedProcess) extends Event

  class SelfAlertingProcess(name: String) extends UserStartedProcess(name) {
    alert(name + ": Started")
    val startTime: Long = Calendar.getInstance().getTimeInMillis


    override def halt(): Unit = {
      super.halt()
      alert(name + ": Halted", Elevation.NOTICE)
    }

    override def fail(): Unit = {
      super.fail()
      alert(name + ": Failed", Elevation.ERROR)
    }

    override def finish(): Unit = {
      super.finish()
      val timeTaken = TimeUnit.SECONDS.toSeconds(Calendar.getInstance().getTimeInMillis - startTime)
      alert(name + s": Finished ${timeTaken / 1000.0}s")
    }
  }

  class UserStartedProcess(val name: String) {
    //private val uuid : UUID = UUID.randomUUID() //Will need for log files
    private var _determinate: Boolean = false
    private var _value: Int = 0
    private var _failed: Boolean = false

    def failed: Boolean = _failed

    def determinate: Boolean = _determinate

    def setIndeterminate(): Unit = {
      _value = 0
      _determinate = false
      AlertPublisher.publish(UserProcessUpdate(this))
    }

    def fail(): Unit = {
      _failed = true
      value = 100
    }

    def value: Int = _value

    def value_=(newValue: Int): Unit = {
      _value = newValue
      _determinate = true
      AlertPublisher.publish(UserProcessUpdate(this))
    }

    def finish(): Unit = {
      value = 100
    }

    def halt(): Unit = {
      _failed = false
      value = 0
    }

    ongoingProcesses = this :: ongoingProcesses
    AlertPublisher.publish(UserProcessUpdate(this))
  }

  case class Alert(time: Date, elevation: Elevation.Elevation, message: String) {
    override def toString: String = UserOptions.preferredTimeFormat.format(time) + ": " + message

    def color: Color = {
      elevation match {
        case Elevation.ERROR => new Color(150, 0, 0) // Something broke
        case Elevation.ALERT => new Color(150, 150, 0) // Something soon to break
        case Elevation.WARNING => new Color(0, 150, 0) // That would have caused something to break
        case Elevation.DEBUG => new Color(0, 150, 150) // I want to know how it would have broken
        case Elevation.NOTICE => new Color(0, 0, 150) // Nothing is broken
      }
    }

    def elevationText: String = {
      elevation match {
        case Elevation.ERROR => "ERROR" // Something broke
        case Elevation.ALERT => "ALERT" // Something soon to break
        case Elevation.WARNING => "WARNING" // That would have caused something to break
        case Elevation.DEBUG => "DEBUG" // I want to know how it would have broken
        case Elevation.NOTICE => "" // Nothing is broken
      }
    }
  }

  object AlertPublisher extends Publisher

  object Elevation extends Enumeration {
    type Elevation = Value
    val ALERT, ERROR, WARNING, NOTICE, DEBUG = Value
  }
}
