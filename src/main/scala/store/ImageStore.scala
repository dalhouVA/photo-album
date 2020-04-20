package store

import java.io.{ByteArrayInputStream, File}
import java.util.{Base64, UUID}

import javax.imageio.ImageIO

trait Store {
  def saveImage(base64String: String): File
}

trait ImageStore extends Store {
  def saveImage(base64String: String): File = {
    val byteArray = Base64.getDecoder.decode(base64String)
    val bis = new ByteArrayInputStream(byteArray)
    val file = new File(s"D:\\img\\${UUID.randomUUID()}.jpg")
    ImageIO.write(ImageIO.read(bis), "jpg", file)
    file
  }

}
