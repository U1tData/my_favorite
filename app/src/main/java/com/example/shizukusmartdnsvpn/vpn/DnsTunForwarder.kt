package com.example.shizukusmartdnsvpn.vpn

import android.net.VpnService
import java.io.FileDescriptor
import java.io.FileInputStream
import java.io.FileOutputStream
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.InetSocketAddress

class DnsTunForwarder(
    private val service: VpnService,
    private val fd: FileDescriptor,
    private val upstreams: List<InetAddress>
) : Runnable {

    @Volatile
    private var running = true

    fun stop() { running = false }

    override fun run() {
        val input = FileInputStream(fd)
        val output = FileOutputStream(fd)
        val packet = ByteArray(32767)
        var upIdx = 0

        while (running && !Thread.currentThread().isInterrupted) {
            val len = try { input.read(packet) } catch (e: Exception) { break }
            if (len <= 0) continue
            // IPv4 only
            val version = (packet[0].toInt() ushr 4) and 0xF
            if (version != 4) continue
            val ihl = (packet[0].toInt() and 0xF) * 4
            if (len < ihl + 8) continue
            val protocol = packet[9].toInt() and 0xFF
            if (protocol != 17) continue // UDP only

            val srcIp = byteArrayOf(packet[12], packet[13], packet[14], packet[15])
            val dstIp = byteArrayOf(packet[16], packet[17], packet[18], packet[19])

            val srcPort = ((packet[ihl].toInt() and 0xFF) shl 8) or (packet[ihl + 1].toInt() and 0xFF)
            val dstPort = ((packet[ihl + 2].toInt() and 0xFF) shl 8) or (packet[ihl + 3].toInt() and 0xFF)
            if (dstPort != 53) continue

            val udpLen = ((packet[ihl + 4].toInt() and 0xFF) shl 8) or (packet[ihl + 5].toInt() and 0xFF)
            val dnsOffset = ihl + 8
            val dnsLen = udpLen - 8
            if (len < dnsOffset + dnsLen) continue

            // Forward to upstream
            val upstream = upstreams[upIdx % upstreams.size]
            upIdx++
            val upstreamSocket = DatagramSocket()
            try {
                service.protect(upstreamSocket)
            } catch (_: Throwable) {
                // best effort
            }
            upstreamSocket.soTimeout = 3000
            try {
                upstreamSocket.send(
                    DatagramPacket(
                        packet,
                        dnsOffset,
                        dnsLen,
                        InetSocketAddress(upstream, 53)
                    )
                )
                val respBuf = ByteArray(1500)
                val respPkt = DatagramPacket(respBuf, respBuf.size)
                upstreamSocket.receive(respPkt)

                val respLen = respPkt.length
                val totalLen = 20 + 8 + respLen
                val out = ByteArray(totalLen)

                // IPv4 header
                out[0] = 0x45.toByte() // version(4) + ihl(5)
                out[1] = 0x00
                out[2] = ((totalLen ushr 8) and 0xFF).toByte()
                out[3] = (totalLen and 0xFF).toByte()
                out[4] = 0x00; out[5] = 0x00 // id
                out[6] = 0x00; out[7] = 0x00 // flags/fragment
                out[8] = 64.toByte()         // TTL
                out[9] = 17.toByte()         // protocol UDP
                // src 10.0.0.1
                out[12] = 10; out[13] = 0; out[14] = 0; out[15] = 1
                // dst = original src
                out[16] = srcIp[0]; out[17] = srcIp[1]; out[18] = srcIp[2]; out[19] = srcIp[3]
                // checksum later

                // UDP header
                val udpLenResp = 8 + respLen
                out[20] = 0x00; out[21] = 0x35 // src port 53
                out[22] = ((srcPort ushr 8) and 0xFF).toByte()
                out[23] = (srcPort and 0xFF).toByte()
                out[24] = ((udpLenResp ushr 8) and 0xFF).toByte()
                out[25] = (udpLenResp and 0xFF).toByte()
                out[26] = 0x00; out[27] = 0x00 // UDP checksum optional for IPv4 (0 = not used)

                // payload
                System.arraycopy(respPkt.data, 0, out, 28, respLen)

                // compute IP header checksum
                out[10] = 0; out[11] = 0
                val csum = ipv4HeaderChecksum(out)
                out[10] = ((csum ushr 8) and 0xFF).toByte()
                out[11] = (csum and 0xFF).toByte()

                output.write(out)
            } catch (_: Exception) {
                // ignore query
            } finally {
                upstreamSocket.close()
            }
        }
    }

    private fun ipv4HeaderChecksum(header: ByteArray): Int {
        var sum = 0
        var i = 0
        while (i < 20) {
            if (i == 10) { i += 2; continue }
            val v = ((header[i].toInt() and 0xFF) shl 8) or (header[i + 1].toInt() and 0xFF)
            sum += v
            i += 2
        }
        while ((sum ushr 16) != 0) {
            sum = (sum and 0xFFFF) + (sum ushr 16)
        }
        return sum.inv() and 0xFFFF
    }
}
