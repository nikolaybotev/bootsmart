package ns

import java.io.PrintWriter
import java.nio.charset.Charset
import java.nio.file.Files
import java.nio.file.Paths
import scala.collection.JavaConversions.asScalaBuffer

object ScaleThatVector extends App {

  case class Component(container: String, level: Int, name: String)
  case class Event(c: Component, start: Long, end: Long)

  val inputFile = if (args.size > 0) args(0) else "nsboot"
  println("Reading file " + inputFile)

  // Read the text file in memory
  val lines = Files.readAllLines(Paths.get(inputFile), Charset.defaultCharset)

  val allEvents = lines map { line =>
    val fields = line.split(" ")
    Event(Component(fields(0), fields(1).toInt, fields(2)), fields(3).toLong, fields(4).toLong)
  }

  val pixelsPerMs = 15 / 1000d
  val msPerPixel = 1 / pixelsPerMs

  // Omit uninteresting events and order by start time
  val events = allEvents
      .filter (!_.c.name.matches("Level[1-4]"))
      .filter (e => e.end - e.start >= msPerPixel)
      .sortBy (_.start)

  val startTime = events map (_ start) reduce (_ min _)
  val endTime = events map (_ end) reduce (_ max _)
  val timeSpan = ((endTime - startTime) / 5000d).ceil.toInt * 5000

  val lineHeight = 16

  val pixelWidth = (timeSpan * pixelsPerMs).ceil.toLong + 300
  val pixelHeight = lineHeight * events.size + 30

  def time2px(t: Long) = ((t - startTime) * pixelsPerMs).round

  printf("netsmart start began %tT%nnetsmart start ended %tT%n", startTime, endTime)

  // Draw time axis labels
  val axisLabels = for (i <- 5000 to (timeSpan, 5000)) yield {
    <text style="font-family: sans-serif; font-size: 9pt; fill: #000000; opacity: 0.7;" x={(i * pixelsPerMs).toInt.toString} y="0" dx="-0.5em" dy="-3">{(i/1000).toString}s</text>
  }

  // Draw vertical axis lines
  val bottom = events.size * lineHeight
  val axisLinesVert = for (i <- 0 to (timeSpan, 1000)) yield {
    val x = (i * pixelsPerMs).toInt.toString
    <line style={if (i % 5000 == 0) "stroke: #d0d0d0;" else ""} x1={x} y1="0" x2={x} y2={bottom.toString}/>
  }

  // Draw horizontal axis lines
  val axisLinesHoriz = for (i <- events.size to events.size) yield {
    val y = i * lineHeight
    <line x1="0" y1={y.toString} x2={pixelWidth.toString} y2={y.toString}/>
  }

  // Draw process
  var y = 0
  var maxEnd = startTime
  val eventNodes = events flatMap { event =>
    val label = event.c.level + " " + event.c.container + " / " + event.c.name
    val x1 = time2px(event.start)
    val x2 = time2px(event.end)
    val gapped = (event.start - maxEnd) >= msPerPixel * 3
    var nodes =
        <rect x={ x1.toString } y={ y.toString } width={(x2 - x1).toString} height={lineHeight.toString} transform="translate(0.5, 0.5)" style="stroke: #a0a0a0; fill: #e0e0e0;"/> ++
        <text transform={ "translate(%d,%d)".format(x1, y) }
          style={ "font-family: sans-serif; font-size: 9pt; fill: #%s; opacity: 0.7; font-weight: %s;".format(if (gapped) "ff0000" else "000000", if (gapped) "bold" else "normal") }
          dx="4" dy="12" x={ (x2 - x1).toString } y="0">{ label }</text>
    if (gapped) {
      val gx1 = time2px(maxEnd)
      val gx2 = x1
      nodes ++=
          <rect x={gx1.toString} y={y.toString} width={(gx2 - gx1).toString} height={lineHeight.toString} transform="translate(0.5, 0.5)" style="stroke: #a0a0a0; fill: #c27a7a; fill-opacity: 0.25;"/> ++
          <text transform={"translate(%d,%d)".format(gx1, y)}
            style="font-family: sans-serif; font-size: 9pt; fill: #000000; opacity: 0.7; font-weight: bold;"
            dx="4" dy="13" x="0" y="0">{"%.1fs".format((event.start - maxEnd)/1000d)}</text>
    }
    y += lineHeight
    maxEnd = maxEnd max event.end
    nodes
  }

  val svg =
  <svg width={ pixelWidth.toString + "px" } height={ pixelHeight.toString } xmlns="http://www.w3.org/2000/svg">
    <rect x="0" y="0" width="100%" height="100%" style="fill: white;"/>
    <g transform="translate(0, 20)" style="fill: none;">
      { axisLabels }
        <rect style="stroke: #e0e0e0; padding: 10px;" x="0" y="0" width="100%" height="100%"/>
        <g style="stroke: #e0e0e0; stroke-width: 1px;" transform="translate(0.5, 0.5)">
          { axisLinesVert }
          { axisLinesHoriz }
        </g>
      { eventNodes }
    </g>
  </svg>

  val file = new PrintWriter(inputFile + ".svg", "UTF-8")
  try {
    file.append(svg.toString)
  } finally {
    file.close
  }

}