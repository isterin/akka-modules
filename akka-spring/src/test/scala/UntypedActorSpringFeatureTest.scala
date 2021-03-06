/**
 * Copyright (C) 2009-2010 Scalable Solutions AB <http://scalablesolutions.se>
 */
package akka.spring


import foo.PingActor
import akka.dispatch.ExecutorBasedEventDrivenWorkStealingDispatcher
import org.scalatest.matchers.ShouldMatchers
import org.scalatest.junit.JUnitRunner
import org.junit.runner.RunWith
import org.springframework.context.support.ClassPathXmlApplicationContext
import akka.remote.netty.NettyRemoteSupport
import org.scalatest.{BeforeAndAfterAll, FeatureSpec}

import java.util.concurrent.CountDownLatch
import akka.actor.{RemoteActorRef, Actor, ActorRef, TypedActor}
import akka.actor.Actor._

/**
 * Tests for spring configuration of typed actors.
 * @author michaelkober
 */
@RunWith(classOf[JUnitRunner])
class UntypedActorSpringFeatureTest extends FeatureSpec with ShouldMatchers with BeforeAndAfterAll {

  var optimizeLocal_? = remote.asInstanceOf[NettyRemoteSupport].optimizeLocalScoped_?

  override def beforeAll {
    remote.asInstanceOf[NettyRemoteSupport].optimizeLocal.set(false) //Can't run the test if we're eliminating all remote calls
    remote.start("localhost",9990)
  }

  override def afterAll {
    remote.asInstanceOf[NettyRemoteSupport].optimizeLocal.set(optimizeLocal_?) //Reset optimizelocal after all tests

    remote.shutdown
    Thread.sleep(1000)
  }


  def getPingActorFromContext(config: String, id: String) : ActorRef = {
    PingActor.latch = new CountDownLatch(1)
    val context = new ClassPathXmlApplicationContext(config)
    val pingActor = context.getBean(id).asInstanceOf[ActorRef]
    assert(pingActor.getActorClassName() === "akka.spring.foo.PingActor")
    pingActor.start()
  }


  feature("parse Spring application context") {

    scenario("get a untyped actor") {
      val myactor = getPingActorFromContext("/untyped-actor-config.xml", "simple-untyped-actor")
      myactor.sendOneWay("Hello")
      PingActor.latch.await
      assert(PingActor.lastMessage === "Hello")
      assert(myactor.isDefinedAt("some string message"))
    }

    scenario("untyped-actor of provided bean") {
      val myactor = getPingActorFromContext("/untyped-actor-config.xml", "simple-untyped-actor-of-bean")
      myactor.sendOneWay("Hello")
      PingActor.latch.await
      assert(PingActor.lastMessage === "Hello")
      assert(myactor.isDefinedAt("some string message"))
    }

    scenario("untyped-actor with timeout") {
      val myactor = getPingActorFromContext("/untyped-actor-config.xml", "simple-untyped-actor-long-timeout")
      assert(myactor.getTimeout() === 10000)
      myactor.sendOneWay("Hello 2")
      PingActor.latch.await
      assert(PingActor.lastMessage === "Hello 2")
    }

    scenario("get a remote typed-actor") {
      val myactor = getPingActorFromContext("/untyped-actor-config.xml", "remote-untyped-actor")
      myactor.sendOneWay("Hello 4")
      assert(myactor.homeAddress.isDefined)
      assert(myactor.homeAddress.get.getHostName() === "localhost")
      assert(myactor.homeAddress.get.getPort() === 9990)
      PingActor.latch.await
      assert(PingActor.lastMessage === "Hello 4")
    }

    scenario("untyped-actor with custom dispatcher") {
      val myactor = getPingActorFromContext("/untyped-actor-config.xml", "untyped-actor-with-dispatcher")
      assert(myactor.id === "untyped-actor-with-dispatcher")
      assert(myactor.getTimeout() === 1000)
      assert(myactor.getDispatcher.isInstanceOf[ExecutorBasedEventDrivenWorkStealingDispatcher])
      myactor.sendOneWay("Hello 5")
      PingActor.latch.await
      assert(PingActor.lastMessage === "Hello 5")
    }

    scenario("create client managed remote untyped-actor") {
      val myactor = getPingActorFromContext("/server-managed-config.xml", "client-managed-remote-untyped-actor")
      myactor.sendOneWay("Hello client managed remote untyped-actor")
      PingActor.latch.await
      assert(PingActor.lastMessage === "Hello client managed remote untyped-actor")
      assert(myactor.homeAddress.isDefined)
      assert(myactor.homeAddress.get.getHostName() === "localhost")
      assert(myactor.homeAddress.get.getPort() === 9990)
    }

    scenario("autostart untypedactor when requested in config") {
      val context = new ClassPathXmlApplicationContext("/untyped-actor-config.xml")
      val pingActor = context.getBean("remote-untyped-actor-autostart").asInstanceOf[ActorRef]
      assert(pingActor.isRunning === true)
      assert(pingActor.id === "remote-untyped-actor-autostart")
    }

    scenario("create server managed remote untyped-actor") {
      val myactor = getPingActorFromContext("/server-managed-config.xml", "server-managed-remote-untyped-actor")
      val nrOfActors = Actor.registry.actors.length
      val actorRef = remote.actorFor("server-managed-remote-untyped-actor", "localhost", 9990)
      actorRef.sendOneWay("Hello server managed remote untyped-actor")
      PingActor.latch.await
      assert(PingActor.lastMessage === "Hello server managed remote untyped-actor")
      assert(Actor.registry.actors.length === nrOfActors)
    }

    scenario("create server managed remote untyped-actor with custom service id") {
      val myactor = getPingActorFromContext("/server-managed-config.xml", "server-managed-remote-untyped-actor-custom-id")
      val nrOfActors = Actor.registry.actors.length
      val actorRef = remote.actorFor("ping-service", "localhost", 9990)
      actorRef.sendOneWay("Hello server managed remote untyped-actor")
      PingActor.latch.await
      assert(PingActor.lastMessage === "Hello server managed remote untyped-actor")
      assert(Actor.registry.actors.length === nrOfActors)
    }

    scenario("get client actor for server managed remote untyped-actor") {
      PingActor.latch = new CountDownLatch(1)
      val context = new ClassPathXmlApplicationContext("/server-managed-config.xml")
      val pingActor = context.getBean("server-managed-remote-untyped-actor-custom-id").asInstanceOf[ActorRef]
      assert(pingActor.getActorClassName() === "akka.spring.foo.PingActor")
      pingActor.start()
      val nrOfActors = Actor.registry.actors.length
      // get client actor ref from spring context
      val actorRef = context.getBean("client-1").asInstanceOf[ActorRef]
      assert(actorRef.isInstanceOf[RemoteActorRef])
      actorRef.sendOneWay("Hello")
      PingActor.latch.await
      assert(Actor.registry.actors.length === nrOfActors)
    }

  }
}

