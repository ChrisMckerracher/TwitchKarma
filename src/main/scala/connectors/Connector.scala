package connectors

import java.net.InetAddress

import com.datastax.driver.core.Cluster
import com.typesafe.config.ConfigFactory
import com.websudos.phantom.connectors.{KeySpace, SimpleConnector}
import com.websudos.phantom.dsl.Session

/**
  * Created on 2016-01-28.
  * @author Sina Ghaffari
  */
object Connector extends SimpleConnector {
  val config = ConfigFactory.load()
  val host = config.getString("cassandra.host")
  val inet = InetAddress.getByName(host)
  val cluster = Cluster.builder()
    .addContactPoints(inet)
    .withPort(config.getInt("cassandra.port"))
    .build()

  override implicit def keySpace: KeySpace = KeySpace(config.getString("cassandra.keyspace"))
  override implicit lazy val session: Session = cluster.connect(keySpace.name)
}
