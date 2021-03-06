/**
 * Copyright (C) 2009-2010 Scalable Solutions AB <http://scalablesolutions.se>
 */

package akka.amqp

import com.rabbitmq.client._

import akka.event.EventHandler
import akka.amqp.AMQP.ProducerParameters

private[amqp] class ProducerActor(producerParameters: ProducerParameters)
    extends FaultTolerantChannelActor(
        producerParameters.exchangeParameters, producerParameters.channelParameters) {

  import producerParameters._

  val exchangeName = exchangeParameters.flatMap(params => Some(params.exchangeName))

  def specificMessageHandler = {

    case message@Message(payload, routingKey, mandatory, immediate, properties) if channel.isDefined => {
      channel.foreach(_.basicPublish(exchangeName.getOrElse(""), routingKey, mandatory, immediate, properties.getOrElse(null), payload))
    }
    case message@Message(payload, routingKey, mandatory, immediate, properties) => {
      EventHandler notifyListeners EventHandler.Warning(this, "Unable to send message [%s]" format message)
      // FIXME: If channel is not available, messages should be queued back into the actor mailbox and actor should only react on 'Start'
    }
  }

  protected def setupChannel(ch: Channel) {
    returnListener match {
      case Some(listener) => ch.setReturnListener(listener)
      case None => ch.setReturnListener(new ReturnListener() {
        def handleBasicReturn(
            replyCode: Int,
            replyText: String,
            exchange: String,
            routingKey: String,
            properties: com.rabbitmq.client.AMQP.BasicProperties,
            body: Array[Byte]) = {
          throw new MessageNotDeliveredException(
            "Could not deliver message [" + body +
            "] with reply code [" + replyCode +
            "] with reply text [" + replyText +
            "] and routing key [" + routingKey +
            "] to exchange [" + exchange + "]",
            replyCode, replyText, exchange, routingKey, properties, body)
        }
      })
    }
  }

  override def toString =
    "AMQP.Poducer[address= "+ self.address +
    ", exchangeParameters=" + exchangeParameters + "]"
}

