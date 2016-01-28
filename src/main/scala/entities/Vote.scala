package entities

import java.util.UUID

import org.joda.time.DateTime

/**
  * Created on 2016-01-28.
  * @author Sina Ghaffari
  */
case class Vote(id: UUID, user1: String, user2: String, time: DateTime, vote: Int)
