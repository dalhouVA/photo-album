package dto

import java.util.UUID

import akka.http.scaladsl.model.Uri

case class ImageDTO(id: UUID, name: String, uri: Uri)
