package store

import java.io.{ByteArrayInputStream, File}
import java.util.Base64

import generator.Generator
import javax.imageio.ImageIO

import scala.util.{Failure, Success, Try}

trait Store {
  def saveImage(base64String: String): Option[File]
}

class ImageStore(generator: Generator) extends Store {
  def saveImage(base64String: String): Option[File] = {
    Try {
    val base64 = base64String.split(",")
    val (data, img_string) = (base64.head, base64.last)

    def extension: String = {
      val idx = data.indexOf('/')
      val idx_ = data.indexOf(';')
      data.substring(idx + 1, idx_)
    }

      val byteArray = Base64.getDecoder.decode(img_string)
      val bis = new ByteArrayInputStream(byteArray)
      val file = new File(s"/img/${generator.id}.$extension")
      ImageIO.write(ImageIO.read(bis), s"$extension", file)
      file
    } match {
      case Failure(_) => None
      case Success(value) =>Some(value)
    }
  }
}
