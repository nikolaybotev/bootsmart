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
  
  val file = new PrintWriter(inputFile + ".svg", "UTF-8")
  
  file.print("""<?xml version="1.0" standalone="no"?>
<!DOCTYPE svg PUBLIC "-//W3C//DTD SVG 20010904//EN" "http://www.w3.org/TR/2001/REC-SVG-20010904/DTD/svg10.dtd">

<svg width="%dpx" height="%dpx" xmlns="http://www.w3.org/2000/svg">
      
  <rect x="0" y="0" width="100%%" height="100%%" style="fill: white;"/>
  <g transform="translate(0, 20)" style="fill: none;">
      """.format(pixelWidth, pixelHeight))
      
  // Draw time axis labels
  for (i <- 5000 to (timeSpan, 5000)) {
    file.print("""
        <text style="font-family: sans-serif; font-size: 9pt; fill: #000000; opacity: 0.7;" x="%d" y="0" dx="-0.5em" dy="-3">%ds</text>
        """.format((i * pixelsPerMs).toInt, i/1000))
  }
  
  // Draw vertical axis lines
  file.print("""
      <rect style="stroke: #e0e0e0; padding: 10px;" x="0" y="0" width="100%" height="100%"/>
      <g style="stroke: #e0e0e0; stroke-width: 1px;" transform="translate(0.5, 0.5)">
      """)
  for (i <- 0 to (timeSpan, 1000)) {
    file.print("""
        <line%2$s x1="%1$d" y1="0" x2="%1$d" y2="%3$d"/>
        """.format((i * pixelsPerMs).toInt, if (i % 5000 == 0) " style=\"stroke: #d0d0d0;\"" else "", events.size * lineHeight))
  }
  
  // Draw horizontal axis lines
  for (i <- events.size to events.size) {
    file.print("""
        <line x1="0" y1="%1$d" x2="%2$d" y2="%1$d"/>
        """.format(i * lineHeight, pixelWidth))
  }
  file.print("""
      </g>
      """)

  // Draw process
  var y = 0
  var maxEnd = startTime
  for (event <- events) {
    val label = event.c.level + " " + event.c.container + " / " + event.c.name
    val x1 = time2px(event.start)
    val x2 = time2px(event.end)
    val gapped = (event.start - maxEnd) >= msPerPixel * 3
    val gapLabel = if (gapped) "(%.1f s gap) ".format((event.start - maxEnd)/1000d) else "" 
    file.print("""
        <rect x="%d" y="%d" width="%d" height="%d" transform="translate(0.5, 0.5)" style="stroke: #a0a0a0; fill: #%s;"/>
        """.format(x1, y, x2 - x1, lineHeight, if (gapped) "ff6600" else "e0e0e0"))
    file.print("""
        <text transform="translate(%d,%d)" style="font-family: sans-serif; font-size: 9pt; fill: #%s; opacity: 0.7; font-weight: %s;" dx="4" dy="12" x="%d" y="0">%s</text>            
        """.format(x1, y, if (gapped) "ff0000" else "000000", if (gapped) "bold" else "normal", x2 - x1, gapLabel + label))
    y += lineHeight
    maxEnd = maxEnd max event.end
  }
      
  file.print("""
  </g>
</svg>      
      """)
      
  file.close
  
}