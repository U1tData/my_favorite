# ShizukuSmartDnsVPN (prototype)

A minimal Android VPNService app that integrates with Shizuku and forwards DNS to upstream resolvers, intended as a starting point to integrate SmartDNS.

What it does now
- Requests Shizuku permission (optional) and starts a foreground VPNService
- Establishes a TUN interface, sets default route, and a placeholder DNS "10.0.0.1"
- Includes a DnsTunForwarder that forwards UDP/53 packets extracted from TUN to configured upstreams
- Lays groundwork to run smartdns via Shizuku (you need to provide the binary and config)

What it does NOT do yet
- Bundle/execute the SmartDNS binary
- HTTP/DoH/DoT upstreams, caching, rules, etc.

How to continue
1. Build and install with Android Studio or Gradle.
2. Put your smartdns binary on device, e.g. /data/local/tmp/smartdns (arm64/x86_64 as needed)
3. Replace startSmartDns() in SmartDnsVpnService with real exec using ShizukuRunner.exec(
   arrayOf("/data/local/tmp/smartdns", "-c", confFile.absolutePath)
)
4. Optionally protect sockets created by your smartdns process via VpnService.protect by passing fd numbers or using a localhost port and protecting the client sockets only.

Security and permissions
- Uses android.permission.BIND_VPN_SERVICE and runs as foreground service
- Uses Shizuku provider to allow privileged process execution when Shizuku is available

This is a prototype intended for further extension.
