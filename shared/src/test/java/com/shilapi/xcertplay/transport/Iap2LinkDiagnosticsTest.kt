package com.shilapi.xcertplay.transport

import org.junit.Assert.*
import org.junit.Test

class Iap2LinkDiagnosticsTest {
    @Test fun timeoutIdentifiesTheUnconfirmedCommandWithoutItsContents() {
        val engine = readyEngine()
        engine.sendSession(10, "private-network-secret".toByteArray(), 2, 0x5703)
        engine.advanceTime(2_002)
        engine.advanceTime(4_002)
        val failure = (engine.pollEvent() as Iap2LinkEngine.Event.Dead).reason!!
        assertTrue(failure, failure.startsWith("iAP2 packet 100 was not acknowledged"))
        for (field in listOf("session=10", "command=0x5703", "peerAck=99", "sent=100", "inFlight=1", "attempts=2")) {
            assertTrue(failure, failure.contains(field))
        }
        assertFalse(failure.contains("private-network-secret"))
    }

    @Test fun diagnosticAttributionDoesNotChangeAnyWireBytes() {
        val tagged = readyEngine()
        val plain = readyEngine()
        val content = byteArrayOf(0x40, 0x40, 0, 6, 0x43, 1)
        tagged.sendSession(10, content, 2, 0x4301)
        plain.sendSession(10, content, 2)
        assertArrayEquals(plain.takeOutput(), tagged.takeOutput())
        tagged.advanceTime(2_002)
        plain.advanceTime(2_002)
        assertArrayEquals(plain.takeOutput(), tagged.takeOutput())
    }

    @Test fun repeatedExtendedAcknowledgementsAttributeTheSameCommand() {
        val engine = readyEngine()
        engine.sendSession(10, byteArrayOf(1, 2), 2, 0x4301)
        engine.feed(packet(0x60, 1, 99, 0, byteArrayOf(100)), 3)
        engine.feed(packet(0x60, 1, 99, 0, byteArrayOf(100)), 4)
        val failure = (engine.pollEvent() as Iap2LinkEngine.Event.Dead).reason!!
        assertTrue(failure, failure.contains("command=0x4301"))
        assertTrue(failure, failure.contains("attempts=2"))
    }

    private fun readyEngine(): Iap2LinkEngine = Iap2LinkEngine().apply {
        start(wiredInitiator = true, nowMillis = 0)
        takeOutput()
        val sync = Iap2LinkEngine.SynchronizationPayload(
            maxOutgoing = 8, maxLength = 4096, retransmissionTimeoutMillis = 2000,
            acknowledgementTimeoutMillis = 500, maxRetransmissions = 2, maxAcknowledgements = 3,
            sessions = listOf(Iap2LinkEngine.SessionDescriptor(10, 0, 2)),
        )
        feed(packet(0xc0, 1, 99, 0, sync.encode()), 1)
        assertTrue(writable())
        while (pollEvent() != null) Unit
        takeOutput()
    }

    private fun packet(control: Int, seq: Int, ack: Int, session: Int, data: ByteArray): ByteArray {
        val size = 10 + data.size
        val header = byteArrayOf(0xff.toByte(), 0x5a, (size ushr 8).toByte(), size.toByte(),
            control.toByte(), seq.toByte(), ack.toByte(), session.toByte(), 0)
        header[8] = checksum(header.copyOf(8))
        return header + data + checksum(data)
    }

    private fun checksum(data: ByteArray): Byte = (-data.sumOf { it.toInt() and 0xff }).toByte()
}
