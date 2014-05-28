package util

import scala.util.matching.Regex


object Color {

  val HexCode = """(#[0-9a-zA-Z]{6})""".r

  def parse(code: String) = code match {
    case HexCode(code) => new Color(code)
    case _             => throw new IllegalArgumentException("Not a valid hex color code!")
  }

  def unapply(code: String) = code match {
    case HexCode(code) => Some(new Color(code))
    case _             => None
  }

  val colors = Vector(

    // i demand a pancake
    Color.parse("#547980"),
    Color.parse("#45ADA8"),
    Color.parse("#9DE0AD"),
    Color.parse("#E5FCC2"),

    // let them eat cake
    Color.parse("#F1D4AF"),
    Color.parse("#E08E79"),
    Color.parse("#774F38"),
    Color.parse("#ECE5CE"),
    Color.parse("#C5E0DC"),


    Color.parse("#B3DC6C"),
    Color.parse("#FBE983"),
    Color.parse("#FAD165"),
    Color.parse("#92E1C0"),
    Color.parse("#9FE1E7"),
    Color.parse("#9FC6E7"),
    Color.parse("#4986E7"),
    Color.parse("#9A9CFF"),
    Color.parse("#B99AFF"),
    Color.parse("#C2C2C2"),
    Color.parse("#CABDBF"),
    Color.parse("#CCA6AC"),
    Color.parse("#F691B2"),
    Color.parse("#CD74E6"),
    Color.parse("#A47AE2")
  ) 
}


case class Color private(val code: String) {

  override def toString() = code
}