package repository

import java.io.File

trait Repo {
  def uploadImageInRepo(base64String: String): Option[File]
}

