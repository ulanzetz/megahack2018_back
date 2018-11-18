package org.devrock.invalids

import com.typesafe.config.ConfigFactory
import net.ceedubs.ficus.Ficus._
import net.ceedubs.ficus.readers.ArbitraryTypeReader._

case class ServerConfig(host: String, port: Int)

case class DatabaseConfig(driver: String, url: String, user: String, password: String)

case class MentorSearchConfig(url: String)

case class Config(server: ServerConfig, database: DatabaseConfig, mentorSearch: MentorSearchConfig)

class ApiConfigFactory(fileName: String) {
  def load(configFile: String = "application.conf"): Config =
    ConfigFactory.load(configFile).resolve.as[Config]
}
