package com.atigeo.jobmanager

import akka.actor.{ActorSelection}
import com.typesafe.config.Config

/**
 * Created by raduc on 6/12/14.
 */
case class JobManagerParameter(contextName : String, config : Config, isAddHoc : Boolean, resultActorRef: Option[ActorSelection])