# Data safety notes

- Data collected for app functionality: game progress and settings, stored on device.
- Data collected by advertising partners when ads are enabled: advertising id, handled by Google AdMob.
- Purchases are processed by Google Play Billing. The app stores the resulting entitlement, not a payment card.
- Analytics events name gameplay actions. No account registration is required.
- Crash logs may be sent by Firebase Crashlytics.
- Leaderboards, when a Play Games app id is configured, submit a score only.
- The game is playable offline after install.
- Content is a puzzle game without user-generated public content.

Suggested content rating: everyone. No violence, no user chat, optional ads.
