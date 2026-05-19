package io.floci.gcp.core.common.dns;

import io.floci.gcp.config.EmulatorConfig;
import io.floci.gcp.core.common.docker.ContainerDetector;
import io.quarkus.runtime.Startup;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.datagram.DatagramSocket;
import io.vertx.core.datagram.DatagramSocketOptions;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import java.net.DatagramPacket;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.SequencedSet;

/**
 * Embedded UDP/53 DNS server that runs inside the floci-gcp container.
 *
 * Resolves *.localhost.floci-gcp.io (and any configured extra-suffixes) to the
 * container's own IP so virtual-hosted GCS URLs work from inside sidecar containers
 * without requiring wildcard Docker aliases.
 *
 * All other queries are forwarded transparently to the upstream resolver read from
 * /etc/resolv.conf (Docker's embedded DNS at 127.0.0.11).
 *
 * Only starts when running inside Docker. No-op on the host.
 */
@ApplicationScoped
@Startup
public class EmbeddedDnsServer {

    private static final Logger LOG = Logger.getLogger(EmbeddedDnsServer.class);
    private static final int DNS_PORT = 53;
    private static final int TTL = 60;
    private static final String FALLBACK_UPSTREAM = "127.0.0.11";
    public static final String DEFAULT_SUFFIX = "localhost.floci.io";

    static final List<String> BUILTIN_SUFFIXES = List.of(DEFAULT_SUFFIX);

    private volatile String serverIp;
    private final SequencedSet<String> suffixes = new LinkedHashSet<>();
    private String upstreamDns;

    EmbeddedDnsServer(List<String> suffixes) {
        this.suffixes.addAll(BUILTIN_SUFFIXES);
        this.suffixes.addAll(suffixes);
    }

    @Inject
    public EmbeddedDnsServer(EmulatorConfig config, ContainerDetector containerDetector, Vertx vertx) {
        if (!containerDetector.isRunningInContainer()) {
            return;
        }
        try {
            String myIp = InetAddress.getLocalHost().getHostAddress();
            upstreamDns = readUpstreamDns();

            suffixes.addAll(BUILTIN_SUFFIXES);
            config.hostname().ifPresent(suffixes::add);
            config.dns().extraSuffixes().ifPresent(suffixes::addAll);

            DatagramSocket socket = vertx.createDatagramSocket(new DatagramSocketOptions().setIpV6(false));
            socket.listen(DNS_PORT, "0.0.0.0", ar -> {
                if (ar.succeeded()) {
                    serverIp = myIp;
                    LOG.infov("Embedded DNS server started on {0}:53, resolving {1} → {0}", myIp, suffixes);
                    socket.handler(packet -> handleQuery(
                            vertx, socket, packet.data().getBytes(),
                            packet.sender().host(), packet.sender().port(), myIp));
                } else {
                    LOG.warnv("Embedded DNS server failed to bind on port 53: {0}", ar.cause().getMessage());
                }
            });
        } catch (Exception e) {
            LOG.warnv("Failed to initialize embedded DNS server: {0}", e.getMessage());
        }
    }

    public Optional<String> getServerIp() {
        return Optional.ofNullable(serverIp);
    }

    private void handleQuery(Vertx vertx, DatagramSocket socket, byte[] data,
                             String senderHost, int senderPort, String myIp) {
        try {
            ByteBuffer buf = ByteBuffer.wrap(data);
            short txId = buf.getShort();
            short flags = buf.getShort();
            short qdCount = buf.getShort();
            buf.getShort(); // ancount
            buf.getShort(); // nscount
            buf.getShort(); // arcount

            if ((flags & 0x8000) != 0 || qdCount < 1) {
                return;
            }

            int questionOffset = buf.position();
            String qname = readName(buf, data);
            short qtype = buf.getShort();
            buf.getShort(); // qclass
            int questionEnd = buf.position();

            if (qtype == 1 && matchesSuffix(qname)) {
                byte[] response = buildAResponse(data, txId, questionOffset, questionEnd, myIp);
                socket.send(Buffer.buffer(response), senderPort, senderHost, v -> {});
            } else {
                forwardAsync(vertx, socket, data, senderHost, senderPort);
            }
        } catch (Exception e) {
            LOG.debugv("DNS packet error: {0}", e.getMessage());
        }
    }

    boolean matchesSuffix(String name) {
        if (name == null || name.isEmpty()) {
            return false;
        }
        String lower = name.toLowerCase();
        for (String suffix : suffixes) {
            String s = suffix.toLowerCase();
            if (lower.equals(s) || lower.endsWith("." + s)) {
                return true;
            }
        }
        return false;
    }

    String readName(ByteBuffer buf, byte[] data) {
        StringBuilder sb = new StringBuilder();
        int safety = 0;
        while (buf.hasRemaining() && safety++ < 128) {
            int len = buf.get() & 0xFF;
            if (len == 0) {
                break;
            }
            if ((len & 0xC0) == 0xC0) {
                int offset = ((len & 0x3F) << 8) | (buf.get() & 0xFF);
                ByteBuffer ptr = ByteBuffer.wrap(data);
                ptr.position(offset);
                if (sb.length() > 0) {
                    sb.append('.');
                }
                sb.append(readName(ptr, data));
                return sb.toString();
            }
            if (sb.length() > 0) {
                sb.append('.');
            }
            byte[] label = new byte[len];
            buf.get(label);
            sb.append(new String(label));
        }
        return sb.toString();
    }

    byte[] buildAResponse(byte[] query, short txId, int questionOffset, int questionEnd, String ip) {
        int questionLength = questionEnd - questionOffset;
        ByteBuffer resp = ByteBuffer.allocate(12 + questionLength + 16);

        resp.putShort(txId);
        resp.putShort((short) 0x8180);
        resp.putShort((short) 1);
        resp.putShort((short) 1);
        resp.putShort((short) 0);
        resp.putShort((short) 0);

        resp.put(query, questionOffset, questionLength);

        resp.putShort((short) 0xC00C);
        resp.putShort((short) 1);
        resp.putShort((short) 1);
        resp.putInt(TTL);
        resp.putShort((short) 4);

        for (String octet : ip.split("\\.")) {
            resp.put((byte) Integer.parseInt(octet));
        }

        return resp.array();
    }

    private void forwardAsync(Vertx vertx, DatagramSocket socket, byte[] query,
                              String senderHost, int senderPort) {
        String upstream = upstreamDns;
        if (upstream == null) {
            return;
        }
        vertx.executeBlocking(() -> {
            try (java.net.DatagramSocket fwd = new java.net.DatagramSocket()) {
                fwd.setSoTimeout(2000);
                InetAddress addr = InetAddress.getByName(upstream);
                fwd.send(new DatagramPacket(query, query.length, addr, DNS_PORT));
                byte[] buf = new byte[512];
                DatagramPacket resp = new DatagramPacket(buf, buf.length);
                fwd.receive(resp);
                return Arrays.copyOf(resp.getData(), resp.getLength());
            }
        }).onSuccess(response ->
            socket.send(Buffer.buffer(response), senderPort, senderHost, v -> {})
        ).onFailure(e ->
            LOG.debugv("DNS forwarding to {0} failed: {1}", upstream, e.getMessage())
        );
    }

    private String readUpstreamDns() {
        try {
            for (String line : Files.readAllLines(Path.of("/etc/resolv.conf"))) {
                line = line.trim();
                if (line.startsWith("nameserver ")) {
                    String server = line.substring("nameserver ".length()).trim();
                    if (!server.equals("127.0.0.1")) {
                        return server;
                    }
                }
            }
        } catch (Exception e) {
            LOG.debugv("Could not read /etc/resolv.conf: {0}", e.getMessage());
        }
        return FALLBACK_UPSTREAM;
    }
}