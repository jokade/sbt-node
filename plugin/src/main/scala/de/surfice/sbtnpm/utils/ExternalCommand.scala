//     Project: SBT NPM
//      Module:
// Description:
package de.surfice.sbtnpm.utils

import sbt.{Level, _}

/**
 * Helper for platform-independent handling of external commands.
 */
trait ExternalCommand {
  def run(args: String*)(workingDir: File, logger: Logger): Unit = {
    ExternalCommand.execute(cmdPath +: args, workingDir, logger)
  }

  def start(args: String*)(logger: Logger, waitAndKillOnInput: Boolean = false, connectInput: Boolean = false): Process =
    ExternalCommand.start(cmdPath +: args, logger, waitAndKillOnInput, connectInput)

  def startAndStore(args: String*)(key: Any, logger: Logger): Process = ExternalCommand.storedProcesses.get((this,key)) match {
    case Some(p) =>
      logger.warn(s"process $cmdPath already running")
      p
    case None =>
      logger.info(s"starting $cmdPath")
      ExternalCommand.storeProcess((this,key),start(args:_*)(logger))
  }

  def destroy(key: Any, logger: Logger): Option[Process] = ExternalCommand.destroyProcess((this,key)) match {
    case None =>
      logger.warn(s"process $cmdPath not running")
      None
    case x =>
      logger.info(s"stopped $cmdPath")
      x
  }

  def cmdPath: String
}


object ExternalCommand {
  private var _processes = Map.empty[Any,Process]
  def storeProcess(key: Any, process: Process): Process = this.synchronized{
    if(_processes.contains(key))
      throw new RuntimeException(s"There is already a process with defined for key $key")
    _processes += key -> process
    process
  }
  def storedProcesses: Map[Any,Process] = _processes
  def destroyProcess(key: Any): Option[Process] = this.synchronized{
    _processes.get(key) map { p =>
      p.destroy()
      _processes -= key
      p
    }
  }

  def apply(cmdName: String): ExternalCommand = new Impl(cmdName)

  case class Impl(cmdPath: String) extends ExternalCommand

  def execute(cmdLine: Seq[String], cwd: sbt.File, logger: Logger): Unit = {
    val ret = Process(cmdLine) ! logger

    if(ret != 0)
      sys.error(s"Non-zero exit code from command ${cmdLine.head}")

  }

  def start(cmdLine: Seq[String], logger: Logger, waitAndKillOnInput: Boolean, connectInput: Boolean): Process = {
    val p = Process(cmdLine).run(logger,connectInput)
    if(waitAndKillOnInput) {
      readLine("\nPress RETURN to stop external command\n\n")
      p.destroy()
    }
    p
  }

  object npm extends Impl("npm") {
    class NpmLogger(wrapped: Logger) extends Logger {
      override def trace(t: => Throwable): Unit = wrapped.trace(t)
      override def success(message: => String): Unit = wrapped.success(message)
      override def log(level: Level.Value, message: => String): Unit = (level,message) match {
        case (Level.Error,msg) if msg.startsWith("npm WARN") =>
          wrapped.log(Level.Warn,message.stripPrefix("npm WARN "))
        case (lvl,msg) =>
          wrapped.log(lvl,msg)
      }
    }
    def install(npmTargetDir: File, logger: Logger): Unit = run("install")(npmTargetDir,new NpmLogger(logger))
  }
}
