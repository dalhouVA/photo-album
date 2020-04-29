package dto.converters

import components.Image
import dto.OutImageDTO

object ImageDTOConverter {
  def fromImage: Image => OutImageDTO = img => OutImageDTO(img.id.get, img.name, img.uri.get, img.visibility)
}
