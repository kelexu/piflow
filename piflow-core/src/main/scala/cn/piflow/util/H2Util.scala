package cn.piflow.util

import java.sql.{Connection, DriverManager, ResultSet}
import java.util.Date

import org.h2.tools.Server

object H2Util {

  val QUERY_TIME = 30
  val CREATE_FLOW_TABLE = "create table if not exists flow (id varchar(255), name varchar(255), state varchar(255), startTime varchar(255), endTime varchar(255))"
  val CREATE_STOP_TABLE = "create table if not exists stop (flowId varchar(255), name varchar(255), state varchar(255), startTime varchar(255), endTime varchar(255))"
  //val CONNECTION_URL = "jdbc:h2:tcp://" + PropertyUtil.getPropertyValue("server.ip") + ":9092/~/piflow"
  val serverIP = PropertyUtil.getPropertyValue("server.ip") + ":" + PropertyUtil.getPropertyValue("h2.port")
  val CONNECTION_URL = "jdbc:h2:tcp://" +  serverIP + "/~/piflow;AUTO_SERVER=true"
  var connection : Connection= null

  try{

    val statement = getConnectionInstance().createStatement()
    statement.setQueryTimeout(QUERY_TIME)
    //statement.executeUpdate("drop table if exists flow")
    //statement.executeUpdate("drop table if exists stop")
    statement.executeUpdate(CREATE_FLOW_TABLE)
    statement.executeUpdate(CREATE_STOP_TABLE)
    statement.close()
  }catch {
    case ex => println(ex)
  }

  def getConnectionInstance() : Connection = {
    if(connection == null){
      Class.forName("org.h2.Driver")
      connection = DriverManager.getConnection(CONNECTION_URL)
    }
    connection
  }

  def addFlow(appId:String,name:String)={
    val startTime = new Date().toString
    val statement = getConnectionInstance().createStatement()
    statement.setQueryTimeout(QUERY_TIME)
    statement.executeUpdate("insert into flow(id, name) values('" + appId + "','" + name + "')")
    statement.close()
  }
  def updateFlowState(appId:String, state:String) = {
    val statement = getConnectionInstance().createStatement()
    statement.setQueryTimeout(QUERY_TIME)
    val updateSql = "update flow set state='" + state + "' where id='" + appId + "'"
    println(updateSql)
    statement.executeUpdate(updateSql)
    statement.close()
  }
  def updateFlowStartTime(appId:String) = {
    val startTime = new Date().toString
    val statement = getConnectionInstance().createStatement()
    statement.setQueryTimeout(QUERY_TIME)
    val updateSql = "update flow set startTime='" + startTime + "' where id='" + appId + "'"
    println(updateSql)
    statement.executeUpdate(updateSql)
    statement.close()
  }
  def updateFlowFinishedTime(appId:String) = {
    val endTime = new Date().toString
    val statement = getConnectionInstance().createStatement()
    statement.setQueryTimeout(QUERY_TIME)
    val updateSql = "update flow set endTime='" + endTime + "' where id='" + appId + "'"
    println(updateSql)
    statement.executeUpdate(updateSql)
    statement.close()
  }

  def getFlowState(appId: String): String = {
    var state = ""
    val statement = getConnectionInstance().createStatement()
    statement.setQueryTimeout(QUERY_TIME)
    val rs : ResultSet = statement.executeQuery("select * from flow where id='" + appId +"'")
    while(rs.next()){
      state = rs.getString("state")
      println("id:" + rs.getString("id") + "\tname:" + rs.getString("name") + "\tstate:" + rs.getString("state"))
    }
    rs.close()
    statement.close()
    state
  }

  def getFlowInfo(appId:String) : String = {
    val statement = getConnectionInstance().createStatement()
    statement.setQueryTimeout(QUERY_TIME)
    var flowInfo = ""

    val flowRS : ResultSet = statement.executeQuery("select * from flow where id='" + appId +"'")
    while (flowRS.next()){
      flowInfo = "{\"flow\":{\"id\":\"" + flowRS.getString("id") +
        "\",\"name\":\"" +  flowRS.getString("name") +
        "\",\"state\":\"" +  flowRS.getString("state") +
        "\",\"startTime\":\"" +  flowRS.getString("startTime") +
        "\",\"endTime\":\"" + flowRS.getString("endTime") +
        "\",\"stops\":["
    }
    flowRS.close()

    var stopList:List[String] = List()
    val rs : ResultSet = statement.executeQuery("select * from stop where flowId='" + appId +"'")
    while(rs.next()){
      val stopStr = "{\"stop\":{\"name\":\"" + rs.getString("name") +
        "\",\"state\":\"" +  rs.getString("state") +
        "\",\"startTime\":\"" + rs.getString("startTime") +
        "\",\"endTime\":\"" + rs.getString("endTime") + "\"}}"
      //println(stopStr)
      stopList = stopStr.toString +: stopList
    }
    rs.close()

    statement.close()
    if (!flowInfo.equals(""))
      flowInfo += stopList.mkString(",") + "]}}"

    flowInfo
  }


  def getFlowProgress(appId:String) : String = {
    val statement = getConnectionInstance().createStatement()
    statement.setQueryTimeout(QUERY_TIME)

    var stopCount = 0
    var completedStopCount = 0
    val totalRS : ResultSet = statement.executeQuery("select count(*) as stopCount from stop where flowId='" + appId +"'")
    while(totalRS.next()){
      stopCount = totalRS.getInt("stopCount")
      println("stopCount:" + stopCount)
    }
    totalRS.close()

    val completedRS : ResultSet = statement.executeQuery("select count(*) as completedStopCount from stop where flowId='" + appId +"' and state='" + StopState.COMPLETED + "'")
    while(completedRS.next()){
      completedStopCount = completedRS.getInt("completedStopCount")
      println("completedStopCount:" + completedStopCount)
    }
    completedRS.close()
    statement.close()

    val process:Double = completedStopCount.asInstanceOf[Double] / stopCount * 100
    process.toString
  }

  def addStop(appId:String,name:String)={
    val statement = getConnectionInstance().createStatement()
    statement.setQueryTimeout(QUERY_TIME)
    statement.executeUpdate("insert into stop(flowId, name) values('" + appId + "','" + name + "')")
    statement.close()
  }
  def updateStopState(appId:String, name:String, state:String) = {
    val statement = getConnectionInstance().createStatement()
    statement.setQueryTimeout(QUERY_TIME)
    val updateSql = "update stop set state='" + state + "' where flowId='" + appId + "' and name='" + name + "'"
    println(updateSql)
    statement.executeUpdate(updateSql)
    statement.close()
  }

  def updateStopStartTime(appId:String, name:String) = {
    val startTime = new Date().toString
    val statement = getConnectionInstance().createStatement()
    statement.setQueryTimeout(QUERY_TIME)
    val updateSql = "update stop set startTime='" + startTime + "' where flowId='" + appId + "' and name='" + name + "'"
    println(updateSql)
    statement.executeUpdate(updateSql)
    statement.close()
  }

  def updateStopFinishedTime(appId:String, name:String) = {
    val endTime = new Date().toString
    val statement = getConnectionInstance().createStatement()
    statement.setQueryTimeout(QUERY_TIME)
    val updateSql = "update stop set endTime='" + endTime + "' where flowId='" + appId + "' and name='" + name + "'"
    println(updateSql)
    statement.executeUpdate(updateSql)
    statement.close()
  }

  def main(args: Array[String]): Unit = {

    /*try{

      val appId = "111"
      addFlow(appId,"xjzhu")
      updateFlowState(appId,"running")
      val state2 = getFlowState(appId)

      val stop1 = "stop1"
      val stop2 = "stop2"
      addStop(appId, stop1)
      updateStopState(appId,stop1,StopState.COMPLETED)
      addStop(appId, stop2)
      updateStopState(appId,stop2,StopState.STARTED)


      val process = getFlowProgress(appId)
      println("appId=" + appId + "'s process is " + process + "%")

    }catch {
      case ex => println(ex)
    }*/
    val flowInfo = getFlowInfo("application_1539850523117_0157")
    println(flowInfo)
  }

}