# VOJS — WebRTC session lifecycle

1. Sender records locally, with **no user account**.
2. Sender explicitly requests a P2P link. PHP creates an unguessable ID and owner token in a small, private JSON file (900 seconds). No audio bytes enter PHP.
3. Sender creates a WebRTC SDP offer, waits for ICE candidates and saves the offer JSON. Receiver opens the URL and creates an answer; PHP stores the answer JSON.
4. The WebRTC DataChannel sends at most 8 MiB directly between devices, with SHA-256 integrity verification and bounded chunks. Recipient listens to a locally created Blob.
5. Sender must keep the page/app active while the receiver connects. The URL is not a persistent recording URL.
6. Sender can close the session. Old JSON metadata becomes inaccessible after expiry; opportunistic cleanup occurs on new room creation. No scheduled cron and no SQL database are required.

**Limitations:** HTTP(S) is required for signaling; a STUN service (currently Google STUN) receives network information needed for NAT traversal, and some network pairs need TURN. TURN is not configured. WebRTC uses encrypted DTLS transport, but a compromised sender/receiver device or malicious content can still expose audio. The PHP service is not an audio hosting service.

**Legacy migration:** Existing previously uploaded audio on a live deployment is NOT automatically removed by a source-code update. Before enabling P2P-only endpoints, the operator must verify and securely delete any old private VOJS M4A/.audio files and backup copies, subject to applicable retention requirements. New legacy upload endpoints return HTTP 410, not media-storage success.

**Private storage:** Set VOJS_SIGNAL_STORAGE to an absolute path outside DocumentRoot, or use sibling directory vojs-signal-data. It contains only SDP/expiry/token hash/anonymous rate-limit JSON, not audio. Browser client code contains no database credentials.

**Abuse controls:** randomized 192-bit IDs and 256-bit owner tokens, 15-minute expiry, only one answer, owner-only offer/close, max 100 rooms, max 20 new rooms/IP/day. WAF rate-limiting on polling and traffic is still recommended for shared hosting. These server-side records may contain IP/network details inside SDP and must be disclosed in privacy policy.

**Play Console:** Target SDK 36 and data safety disclosures must reflect actual data flow. No false claims of universally reliable connection or full Play approval without real device/host testing.
