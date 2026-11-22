# Media-key routing bootstrap experiment (2026-11-04)

## Current acceptance status — subsequent owner feedback

After several days of real-vehicle use, the owner reports that handlebar button
operations are normal. Handlebar operation is therefore accepted based on the
owner's field feedback, beyond the earlier desk/ADB checks.

Known deferred issue: the instrument play/pause control keeps showing the play
triangle even though actual operation is normal. At the owner's explicit request,
leave this display-state issue alone for now. Do not reopen media routing or alter
playback behavior merely to change that icon.

This feedback does not separately establish full title/artist rendering or an
explicit uninterrupted iPhone-to-C50 audio acceptance test. Do not infer these
from successful handlebar operation. The remaining sections describe the historical
investigation and its validation status at each stage; older "pending" statements
are not the current status of handlebar control.

Keep the routing/bootstrap and own-receiver implementation as the accepted baseline.
Future updates should preserve its opt-in default and reconnect semantics, then
compare actual handlebar behavior against this baseline. The instrument icon issue
is deferred, not a blocker for this accepted control behavior.

The previously recorded APK path below was temporary. It is no longer present in
the local temporary directory on this follow-up; its recorded SHA256 is historical
build identification, not a currently available download. Preserve or recover the
installed field-tested APK before replacing it for a future release.

## Evidence and scope

On this Redmi HyperOS port, focus and an active PLAYING MediaSession did not select
RidePlay for system media keys. A temporary process running under the RidePlay UID
played local PCM; afterward MediaSessionService and Bluetooth both selected RidePlay.
Selection survived the tested CarPlay session recreation and pause/resume. Local
Android NetEase playback took ownership; pausing it did not restore RidePlay.
Repeating the UID playback restored selection. All-zero static PCM succeeded in
three controlled rounds (200 ms, 50 ms, 200 ms), each starting with NetEase selected.
A later system NEXT also produced a different iPhone track and restarted progress.
These were desk experiments, not KTM instrument or C50 acceptance tests.

## Implemented behavior

Advanced → Video and audio → Media-key routing bootstrap (experimental).
The separate preference `rideplay_control_only_route_bootstrap` defaults false and
is sampled at controller attach. It requires audio output to be disabled and
Audio-free media controls to be enabled. Saving marks reconnect needed; it does
not disconnect the active session. It is independent of the focus experiment.

When opted in, the first valid iPhone Now Playing data (title or elapsed position)
and return to an existing CarPlay host with such data request
one 200 ms mono PCM16 zero buffer (48 kHz, USAGE_MEDIA / CONTENT_TYPE_MUSIC).
The background worker releases the AudioTrack after a bounded 300 ms wait.
Duplicate requests within 1.5 seconds are ignored; detach cancels pending work and
releases its track. No periodic retry, extra focus request, device selection, forced
PLAYING state, privileged permission, or Root is used.

Diagnostics record whether this session enabled bootstrap and its queued, started,
released, failed or cancelled result. A released result establishes resource release,
not that Android selected the session; system/Bluetooth dumps remain the authority.

Returning to CarPlay may take media-button ownership from another Android player.
The app cannot reliably identify the current system target through its existing
ordinary permissions. Turning off and reconnecting stops future bootstrap requests;
it cannot erase Android playback history or restore the previous target.

## Validation and remaining acceptance

The full AGENTS.md CI command passed (5m36s); shared 1798 and common 1779 tests had
zero failures. Supplementary session policy/persistence and screenshot checks passed
(30 session cases plus one four-layout screenshot test); standalone build passed
(22s). Large-font compact Japanese, wide English, high-density Chinese and Arabic
RTL setting screenshots were rendered and the key layouts inspected.

The standalone APK was updated with `adb install -r` after verifying the installed
signature and both runtime authentication assets match. Existing settings were
preserved and the new switch remains at its false default. Previous APK backup:
`/tmp/rideplay-before-route.apk`. Runtime identity files remain outside the repository.

The newly integrated APK still needs an enabled in-app device check for creation,
return, detach/cancellation and recovery after another player. KTM title/artist,
handlebar play/pause/next and uninterrupted iPhone → C50 sound remain unverified.
Do not substitute the previous temporary-program experiment or CI for these checks.


## First integrated device observation

The integrated app released its bootstrap track and became the selected system and
Bluetooth player. The user reported a brief apparent pause. No outgoing pause key
was found in the capture; initialization PAUSED alone cannot identify the cause.
The connection made four failed Bluetooth handshake attempts before succeeding.
This exposed an early trigger: media-session creation preceded successful CarPlay
setup. Bootstrap now waits for real iPhone Now Playing data instead, and foreground
resume is gated by the same condition. This avoids local playback during empty
connection attempts; it does not establish that the reported pause is fixed.

## Additional Bluetooth lifecycle boundary

A revised APK session /137 remained the system target, but Bluetooth later showed
no Active player and initial cached metadata. Retained logcat established this order:
15:46:28.554 new token notification; 15:46:28.557 setActivePlayer RidePlay;
15:46:28.562 MediaPlayerWrapper reports the session destroyed; 15:46:28.568 a new
RidePlay wrapper is added. Bootstrap later produced local playback, but the system
target was unchanged and did not generate another selection notification. This is
consistent with a stale controller destruction callback clearing the selected player;
the precise callback/controller identity is not available in this log.

A desk recovery test using the installed APK then established:
- NetEase startup eventually autoplayed. Returning to RidePlay transiently selected
  RidePlay, then the still-playing local player was selected again after release.
- Pausing NetEase and returning to RidePlay restored both the system media-button
  target and Bluetooth Active marker, with current RidePlay song metadata.

Foreground recovery from a paused competitor is verified in the integrated app.
The cold-process/update lifecycle mismatch remains unresolved. More repeated silent
tracks while the target is already RidePlay are not an evidence-based fix for a
missing target-change notification. Do not claim this feature fully solves KTM AVRCP.

## Historical receiver fix and repeated integrated checks

The failure above was reproduced from an already-active RidePlay player by a
controlled force-stop/restart. After stop, Bluetooth retained a dead RidePlay
wrapper; the system notified its historical NetEase receiver as fallback, causing
Bluetooth to switch away/unregister before the dead controller could be cleaned.
At the next creation it selected the old controller, received session-destroyed,
removed the selected player and later added the new wrapper without activation.

For the opted-in control-only session, RidePlay now explicitly registers its own
manifest media-button BroadcastReceiver (public API 31+; explicit mutable broadcast
PendingIntent on older Android). This updates the system historical receiver to
RidePlay. On teardown Bluetooth no longer switches to a historical unrelated app
before processing destruction; the old wrapper is actually removed. This is not
an API to forcibly refresh an arbitrary Bluetooth target.

The receiver is non-exported. It only forwards ACTION_MEDIA_BUTTON to an active
current CarPlay media session on the main thread. It does not connect, start a
foreground service, request focus or create a media session when invoked cold.
Registration is gated by both control-only mode and the opt-in bootstrap setting.
The system may retain its historical receiver across app restarts or disabling
the experiment; disabling does not reconstruct a prior app's media-button history.

After the change on the same Redmi:
- /140 (initial upgrade inherited the previously bad cache) -> process stop removed
  RidePlay from the Bluetooth player list -> /141 became system and Bluetooth target.
- A second stop/restart restored /142; settled metadata contained real title,
  artist, duration and the actual paused iPhone state.
- Reinstalling the same signed experiment APK with install -r restored /143,
  Active plus real cached song data within the first five-second poll.
No local-player handoff or additional audio probe outside the integrated feature
was used to make those cold-start/update checks pass. The iPhone's paused state
was preserved. These observations address the previously unresolved lifecycle
mismatch on this ROM; final KTM hardware and C50 audio continuity remain pending.

Raw local evidence: `/tmp/ktm-coldstart-20261104-160134` (before receiver fix),
`/tmp/ktm-coldstart-20261104-160538`, `/tmp/ktm-coldstart-20261104-160618`, and
`/tmp/ktm-receiver-update-20261104-160721`. Raw dumps remain outside Git.

Final receiver-fix validation: complete AGENTS.md CI passed in 4m12s. Shared 1798,
common 1791 and Home 11 tests had zero failures; the control-only session suite
contains 36 cases including broadcast forwarding, unrelated-action rejection and
no active-session resurrection. All required lint/build tasks passed. The same
signed standalone experiment APK used for device checks is preserved as
`/tmp/rideplay-ktm-route-fixed.apk` (SHA256
`59d164eaa96e5e43e3283487bceeb14f241ca475ca8be6e1a3e475fbca01bac5`).
It is installed on the device, with the experimental setting left at the user's
enabled value. The pre-test iPhone pause state was preserved. No Root used.
