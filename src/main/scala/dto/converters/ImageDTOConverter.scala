package dto.converters

import core.Image
import dto.ImageDTO

object ImageDTOConverter {
  def fromImage: Image => ImageDTO = img => ImageDTO(img.id, img.name, img.uri.get)
}
