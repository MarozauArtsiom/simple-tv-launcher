# Keep rules live here. Start empty: AGP's optimized defaults plus consumer rules are enough.
# Add narrow rules only with a reason (see the r8-analyzer skill).

# JSch (the optional internet tunnel, system/remote/Tunnel.kt) creates its key exchanges, ciphers, MACs,
# signatures and auth methods by class name from its config, so R8 can't see they are used.
-keep class com.jcraft.jsch.** { *; }
# Its optional extras (Windows agents, Unix sockets, Kerberos, Bouncy Castle, logging bridges) are not used here.
-dontwarn com.sun.jna.**
-dontwarn org.apache.logging.log4j.**
-dontwarn org.bouncycastle.**
-dontwarn org.ietf.jgss.**
-dontwarn org.newsclub.net.unix.**
-dontwarn org.slf4j.**
