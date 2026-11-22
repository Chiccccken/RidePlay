# KTM control-only audio focus experiment

This is an opt-in diagnostic experiment, not a confirmed KTM fix. On 2026-11-03,
field snapshots showed an active RidePlay MediaSession with current metadata,
but both the system media-button target and Bluetooth active player were null.
KTM commands reached Android Bluetooth and fell back to the previous music app.
See `KTM_AVRCP_HANDOFF_2026-11-03.md` for the evidence.

## Configuration

In Advanced, enable audio-free media controls and the separate audio-free control
focus experiment. Keep **Disable media-key audio focus** off: that global opt-out
always takes priority. Apply by reconnecting CarPlay. Saving does not interrupt
the running session. The experiment defaults off and is read on controller attach.

The experiment permits the existing media-key focus request without adding local
sound, silent playback, fake playback states, or Bluetooth routing changes. It may
pause or lower audio in other Android apps. Focus callbacks in control-only mode
update the media-key owner's state but do not reach the local audio sink.

Android 15+ can deny focus unless the app is on top or running a foreground service.
The app keeps its media session even if focus is denied. A foreground session service
already exists; this experiment adds no new service or permissions. A focus grant
does not prove that Android selected RidePlay for media keys or AVRCP.

## Diagnostics and verification

The exported report includes `Media control runtime`: actual controlOnly mode,
focus experiment preference sampled at attach, active/playback state, focus request
object presence, last request result, held/transient state, forwarded key attempts,
and title/artist presence. `focusResult=1` means granted, `0` means failed;
`null` means no returned request result in this session. `forwardedKeys` counts
attempts on the CarPlay path, not confirmed iPhone execution. Counts reset on detach.
The separate legacy report line about media-key focus is the saved global preference.

First verify local checks and an update install using the existing signing key and
runtime assets. Then, while parked, collect system snapshots for experiment off,
on, and off again after reconnect. Check actual media-button target, Bluetooth active
player and Current Data, instrument title/artist, handlebar pause/play/next, and
uninterrupted iPhone-to-C50 sound. Repeat background/lock and reconnect cases if the
initial experiment succeeds. Keep the instrument on during each comparison.

To undo, disable the experiment and reconnect. If focus is granted but targets remain
null, record that result: do not silently add a silent AudioTrack or broaden the fix.
